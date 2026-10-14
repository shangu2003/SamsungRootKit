# ============================================================
# SamsungRootKit — R8 / ProGuard 规则
# 目标：release 开启 minify + shrinkResources，把 64MB 的 dex 砍到几 MB 级，
#       同时保证 Compose / material-kolor / Shizuku 运行时不被误删。
# 注意：assets/ 下的 ksud 二进制与 payloads.pack 是内核载荷，R8 与
#       shrinkResources 都不会触碰，它们才是体积硬下限（约 20MB）。
# ============================================================

# Tier1.1 加固：移除 blanket keep，自有业务代码（RootExecutor/PayloadManager/
# ShizukuManager/RootKitScreen 等）全部参与 R8 混淆 + 裁剪。
# 仅保留清单声明的组件入口（下方显式 keep），避免运行时 NoClassDefFoundError。
# 已静态核查：全工程无 Class.forName / getDeclaredMethod / @Serializable / 自定义
# Parcelable，核心模块均为静态直接调用，重命名安全（见审计记录）。
# ── 清单声明的组件入口（必须保留）──
-keep class com.samsung.rootkit.RootKitApp { *; }
-keep class com.samsung.rootkit.ui.MainActivity { *; }
-keep class com.samsung.rootkit.BootReceiver { *; }

# Shizuku 隐藏 API stub（Binder 接口/Provider，必须保留）
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.server.** { *; }

# ── DFReroot 桥接（集成版新增，v5.1.0）──
# 这一条漏了会**直接崩且无任何编译期提示**，原因有两个：
#   1) app_process 以「字符串类名」加载入口（DfBridge.ENTRY_CLASS =
#      "com.samsung.rootkit.df.DfInject"），R8 改名后 app_process 找不到类
#      → NoClassDefFoundError，只在真机注入那一刻暴露；
#   2) 这几支代码内部大量反射：android.util.Xml.resolvePullParser（hidden API，
#      故必须跑在 app_process 而非 zygote 沙箱）、ServiceManager、
#      ActivityThread.systemMain()。任何重命名/裁剪都会在运行时炸。
-keep class com.samsung.rootkit.df.** { *; }
# 静态 main 是 app_process 的进入契约，单独再钉一遍（防被 R8 判定为 unused 而删）
-keepclassmembers class com.samsung.rootkit.df.** {
    public static void main(java.lang.String[]);
}
-dontwarn com.samsung.rootkit.df.**

# ── DFRoot 桥接（df / df.root 集成，2026-10-04 A 路线）──
# 与 DFReroot 同理由，但风险点不同：
#   1) 跨 App 触发用「字符串类名」——DfRootBridge.TRIGGER_ACTIVITY = "df.root.TriggerActivity"
#      指向的是 **df 包内**的类（不受本包 R8 影响），但本包这几个类若被 shrink，
#      触发逻辑整体消失，且**无任何编译期提示**；
#   2) 当前 P3（UI 接入）尚未完成 ⇒ 本包暂时**没有任何引用点**，
#      R8 的 shrink 会把整个 com.samsung.rootkit.dfroot.** 判为 unused 而整体删除。
#      故必须显式 keep，否则「代码写完了但没进包」，且 dex 对比也看不出异常。
#   3) DfRootManager 用 BroadcastReceiver 动态注册（非 manifest 声明），
#      不受 R8 组件裁剪影响，但 receiver 内部类依赖外层 object，一并 keep。
# TODO(P3)：UI 接线后本包会有真实引用点，届时可视 dex 体积决定是否保留本条
#           （建议保留——保留成本极低，删了则可能静默失效）。
-keep class com.samsung.rootkit.dfroot.** { *; }
-dontwarn com.samsung.rootkit.dfroot.**

# ── df 漏洞利用引擎（df.root 包，round24 单包整合）──
# 这是三条**静默失败**风险，任何一条漏掉都不会在编译期报错，只在跑链那一刻炸：
#   1) exp.c 的 JNI_OnLoad 里 `FindClass("df/root/IReporter")` 是**字符串查找**，
#      R8 把 IReporter 改名/合并后 → FindClass 返回 null → JNI_OnLoad 直接失败；
#   2) ExploitRunner.nativeRunAll 是 native 方法，符号名由 Java 包名+类名+方法名决定
#      （Java_df_root_ExploitRunner_nativeRunAll），R8 改名后 native 注册失配
#      → UnsatisfiedLinkError；
#   3) 该包若在 SRK 内暂时没有调用点，R8 shrink 会把整包判为 unused 直接删除
#      （「代码写完了但没进包」，dex 里查不到、也无告警）。
-keep class df.root.** { *; }
-dontwarn df.root.**

# ── 运行记录的两个枚举（2026-10-05）──
# 它们是**以枚举名持久化**的：RunLogStore 把 `source.name` / `outcome.name` 写进 JSON，
# 读回时用 `RunSource.valueOf(...)` / `RunOutcome.valueOf(...)` 还原。
# R8 full mode 的 **enum unboxing** 会把枚举类拆成 int 常量以实现「枚举反优化」，
# 一旦发生，`valueOf` 拿到的名字就再也对不上 → 运行记录整体读不出来（静默变成空列表）。
# 故显式保留这两个枚举的身份（不混淆、不优化）。代价只有几十字节。
-keep class com.samsung.rootkit.data.RunSource { *; }
-keep class com.samsung.rootkit.data.RunOutcome { *; }

# material-kolor 动态配色（含反射/内联色调推导，保留以防运行时丢类）
-keep class com.materialkolor.** { *; }
-dontwarn com.materialkolor.**

# kotlinx 协程 / 反射在 R8 full-mode 下偶发告警，抑制以免中断构建
-dontwarn kotlinx.**
-dontwarn org.jetbrains.**

# Compose / AndroidX 由各自 AAR 的 consumer-rules 处理；此处仅抑制噪声告警
-dontwarn androidx.compose.**
-dontwarn androidx.**

# 保留注解（Compose opt-in、Serializable 等），避免被优化剥离
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# AndroidLiquidGlass (Kyant0/Backdrop) — 真·实时玻璃模糊（Phase 3 弹窗）
# RenderEffect / RuntimeShader 内部类与 RuntimeShader 缓存若被 R8 裁剪，
# 会导致运行时 NoClassDefFoundError / 玻璃效果失效。全量 keep 该包。
-keep class com.kyant.backdrop.** { *; }
-keep class com.kyant.shapes.** { *; }
-dontwarn com.kyant.**

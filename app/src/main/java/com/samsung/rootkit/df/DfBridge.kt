package com.samsung.rootkit.df

import android.content.Context
import android.content.pm.PackageManager

/**
 * DFReroot 桥接层 —— 纯常量与纯函数，不含 UI、不执行任何 root 动作。
 *
 * 设计来源（都有出处，不是自创）：
 *  - 注入命令拼装与入口类约定：DFReroot `installer/.../MainActivity.kt:193-202`
 *    （原为 `CLASSPATH=<installer sourceDir> app_process /system/bin --nice-name=df_inject
 *      com.polygraphene.df.installer.InjectMain --keyhex <hex> --targets android.uid.system`）。
 *    集成后 CLASSPATH 指向**本工程自己的 APK**，入口类为 [ENTRY_CLASS]。
 *  - 「是否已生效」判据：`getApplicationInfo(pkg, 0).uid == 1000`。
 *    这是唯一零 root、一行 API 的判据；`dumpsys` / `getRunningAppProcesses()` 受权限限制，
 *    root 下 grep `packages.xml` 属本末倒置。
 *
 * ⚠️ 与 [DfInject] 的强耦合：[ENTRY_CLASS] 是字符串，改 [DfInject] 的类名或包名必须同步改这里；
 *    同时 `app/proguard-rules.pro` 必须有 `-keep class com.samsung.rootkit.df.** { *; }`，
 *    否则 R8 改名后 app_process 会在运行时 NoClassDefFoundError（无任何编译期提示）。
 */
object DfBridge {

    /** DFReroot 的包名。D2 决策：保持不变（JNI 类名 org.lsposed.lspromise.DirtyFrag 亦不可改）。 */
    const val PKG_DF = "com.polygraphene.df.reroot"

    /** app_process 入口类 —— 必须与 DfInject.kt 里的 object 名逐字一致。 */
    const val ENTRY_CLASS = "com.samsung.rootkit.df.DfInject"

    /** 注入目标：packages.xml 里 android.uid.system 这个 shared-user 的 <pastSigs>。 */
    const val TARGET_SHARED_USER = "android.uid.system"

    /** DFReroot APK 在 assets 中的路径（由构建脚本灌入，见工程根 build.sh）。 */
    const val DF_APK_ASSET = "dfreroot/df_reroot.apk"

    /** 安装时用的设备端中转路径（root 可读，避免 pm 去读 /data/user/0/... 的权限疑问）。 */
    const val DEV_DF_APK = "/data/local/tmp/df_reroot.apk"

    /** app_process 进程名，便于 ps 里辨认。 */
    const val NICE_NAME = "srk_inject"

    /** android.uid.system 的 uid 数值。 */
    const val UID_SYSTEM = 1000

    /**
     * DFReroot 主 Activity 的完整类名。
     *
     * ⚠️ 必须用**显式组件**启动它：v5.1.1 起 DFReroot 移除了 `LAUNCHER` category
     * （按要求隐藏桌面图标），`getLaunchIntentForPackage()` 会返回 null。
     * 该 Activity 仍保留 `android:exported="true"`，否则跨 UID 的我们起不来它。
     */
    const val DF_MAIN_ACTIVITY = "com.polygraphene.df.reroot.MainActivity"

    /**
     * DFReroot 变体 extra 的键名 —— 必须与 DFReroot `MainActivity` 侧逐字一致。
     * 值：`"ksu"`（KernelSU 原版）或 `"next"`（KernelSU-Next）。
     * 用于「SRK 选哪个 root 变体，DFReroot 就用哪个」的联动（2026-10-01）。
     */
    const val EXTRA_KSV_VARIANT = "ksu_variant"

    /**
     * 取"打开 DFReroot 界面"的显式 Intent。
     * 用 `FLAG_ACTIVITY_NEW_TASK` 是为了在非 Activity 上下文（如 Service）里也能起。
     *
     * @param variant 要传给 DFReroot 的变体，null = 不传（DFReroot 用其自身 AUTO 自动识别）。
     */
    fun buildOpenIntent(variant: KsuVariant? = null): android.content.Intent =
        android.content.Intent().apply {
            setClassName(PKG_DF, DF_MAIN_ACTIVITY)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            if (variant != null) {
                putExtra(EXTRA_KSV_VARIANT, when (variant) {
                    KsuVariant.KSU -> "ksu"
                    KsuVariant.KSU_NEXT -> "next"
                })
            }
        }

    // ══════════════════════════════════════════════════════════════════════════
    //  DfRunService 内联通道（2026-10-07 新增）
    // ══════════════════════════════════════════════════════════════════════════
    // 目标：主页红/蓝按钮**不再跳转** DFReroot 界面，而是在本进程发起、由 DFReroot 的
    // DfRunService 在它自己进程内跑链，并把日志经广播回吐，显示在主页卡片里。
    //
    // 能做"不跳转"的前提：DFReroot 侧（v32 起）新增了 `DfRunService`
    // （`exported=true` + signature 权限），它复用既有 `ExploitRunner.runCore`
    // —— 后者本就声明为"跑完整条链（无 UI 依赖）"，开机自启 `BootReceiver` 在用同一条。
    //
    // 三跳安全性：
    //   ① 能力探测（[isInlineCapable]）：旧版 DFReroot 无该组件 ⇒ 回落跳转，功能不丢；
    //   ② 权限：Service 由 signature 级权限保护，两 APK 同签名 ⇒ 自动授予；
    //      非本签名者即便显式指名组件也起不来（AMS 按 manifest 强校验）。
    //   ③ 异常兜底：`startService` 的权限/后台限制校验是**同步**发生的，抛异常即回落跳转。

    /** DFReroot 内联入口的 Service 类名。 */
    const val SERVICE_CLASS = "com.polygraphene.df.reroot.DfRunService"

    /**
     * 保护该 Service 的 signature 级权限 —— 必须与 DFReroot 侧
     * `DfRunService.PERMISSION_CONTROL` **逐字一致**。
     *
     * 本应用在 manifest 里以 `<uses-permission>` 声明它；两 APK 共用 `srk-release.jks`
     * ⇒ 同签名 ⇒ 系统自动授予（无需用户确认）。第三方应用拿不到该权限也就起不了那个 Service。
     */
    const val SERVICE_PERMISSION = "com.polygraphene.df.reroot.permission.CONTROL"

    /** 触发跑链。 */
    const val ACTION_RUN = "com.polygraphene.df.reroot.action.RUN"

    /** 请求只读状态快照（是否已挂钩 / 当前工作变体 / 自启开关…）。 */
    const val ACTION_STATE = "com.polygraphene.df.reroot.action.STATE"

    /** DFReroot 回吐日志所用广播的 action（本应用动态注册它）。与 DFReroot 侧同名常量逐字一致。 */
    const val RUN_LOG_ACTION = "com.samsung.root.kit.dfe.RUN_LOG"

    /** 日志回吐广播的 extra 键 —— 与 DFReroot 侧常量逐字一致。 */
    const val LOG_EXTRA_TYPE = "type"
    const val LOG_EXTRA_LINE = "line"
    const val LOG_EXTRA_RESULT = "result"

    /** 日志回吐广播的帧类型。 */
    const val LOG_TYPE_LOG = "log"     // 一行日志（[LOG_EXTRA_LINE]）
    const val LOG_TYPE_DONE = "done"   // 跑链结束（[LOG_EXTRA_RESULT] = native rc）
    const val LOG_TYPE_STATE = "state" // 状态快照（见 [ACTION_STATE]）

    /** 内联 Service 的 intent extra 键（与 DFReroot `DfRunService` 的伴生常量逐字一致）。 */
    const val EXTRA_LOG_ACTION = "log_action"
    const val EXTRA_LOG_PKG = "log_pkg"

    /**
     * DFReroot 是否**带内联入口**（`DfRunService` 是否存在）。
     *
     * 这是**能力探测**（能不能内联），不是可用性探测（装了没 / 生效没）。
     * v31 及更早的 DFReroot 没有该组件 ⇒ 返回 false ⇒ 调用方回落 `startActivity` 跳转。
     *
     * ⚠️ 依赖 `<queries>` 中 `com.polygraphene.df.reroot` 的包级声明；未声明时包可见性
     *    过滤会让这里恒为 false —— 与 [isDfInstalled] 同一前提。
     */
    fun isInlineCapable(ctx: Context): Boolean = try {
        ctx.packageManager.getServiceInfo(
            android.content.ComponentName(PKG_DF, SERVICE_CLASS), 0,
        )
        true
    } catch (_: Throwable) {
        false
    }

    /**
     * 取"内联跑链"的显式 Service Intent。
     *
     * @param variant       目标变体；null = 不传（DFReroot 走它自己的"持久化 → 自动识别"决策链）。
     * @param logAction     日志回吐所用 action（默认 [RUN_LOG_ACTION]）。
     * @param logPkg        日志回吐目标包（默认本应用包名 —— 只投给自己，不广播到全网）。
     */
    fun buildRunServiceIntent(
        ctx: Context,
        variant: KsuVariant? = null,
        logAction: String = RUN_LOG_ACTION,
        logPkg: String = ctx.packageName,
    ): android.content.Intent =
        android.content.Intent().apply {
            setClassName(PKG_DF, SERVICE_CLASS)
            action = ACTION_RUN
            putExtra(EXTRA_LOG_ACTION, logAction)
            putExtra(EXTRA_LOG_PKG, logPkg)
            if (variant != null) {
                putExtra(EXTRA_KSV_VARIANT, when (variant) {
                    KsuVariant.KSU -> "ksu"
                    KsuVariant.KSU_NEXT -> "next"
                })
            }
        }

    /** 取"查询 DFReroot 状态快照"的显式 Service Intent（结果经同一日志广播通道回吐）。 */
    fun buildStateServiceIntent(
        ctx: Context,
        logAction: String = RUN_LOG_ACTION,
        logPkg: String = ctx.packageName,
    ): android.content.Intent =
        android.content.Intent().apply {
            setClassName(PKG_DF, SERVICE_CLASS)
            action = ACTION_STATE
            putExtra(EXTRA_LOG_ACTION, logAction)
            putExtra(EXTRA_LOG_PKG, logPkg)
        }

    /** DFReroot 变体的镜像（与 PayloadManager.KsuVariant 同构，避免桥接层反向依赖 UI/data）。 */
    enum class KsuVariant { KSU, KSU_NEXT }

    // ─────────────────────────── 状态检测 ───────────────────────────

    /**
     * 是否**已安装**（不代表可用）。
     *
     * ⚠️ 需要 AndroidManifest 的 `<queries>` 声明 [PKG_DF]，否则包可见性过滤会让这里恒为 false。
     */
    fun isDfInstalled(ctx: Context): Boolean = try {
        ctx.packageManager.getApplicationInfo(PKG_DF, 0)
        true
    } catch (_: Throwable) {
        false
    }

    /**
     * 是否**已生效**（真正的判据）。
     *
     * 只有 `sharedUserId="android.uid.system"` 的包才会以 uid 1000 出现。
     * 装了但没注入 / 注入后没软重启时，它只是个 uid 10xxx 的普通 App —— **不可用**。
     */
    fun isDfActive(ctx: Context): Boolean = try {
        ctx.packageManager.getApplicationInfo(PKG_DF, 0).uid == UID_SYSTEM
    } catch (_: Throwable) {
        false
    }

    /** DFReroot 的 versionName（排障用，读不到返回 null）。 */
    fun dfVersionName(ctx: Context): String? = try {
        ctx.packageManager.getPackageInfo(PKG_DF, 0).versionName
    } catch (_: Throwable) {
        null
    }

    // ─────────────────────────── 证书 ───────────────────────────

    /**
     * 读**本应用自己**的签名证书，返回小写 hex。
     *
     * 这是与 DFReroot 原实现的关键差异点：原实现从内置的 `df_reroot.apk` 资产里读证书
     * （`installer/MainActivity.kt:217-229` 的 `resolveCertApk()`）。集成后两 APK 共用同一把密钥
     * `srk-release.jks`，所以注入**本工程自己的证书**即可同时授权两个 APK。
     *
     * hex 小写是硬要求：PMS 用 `hexStringToBytes` 读取，base64 会被拒
     * （`PackagesXml.kt:33-35`、`Abx.kt:200-208`）。
     */
    fun ownCertHex(ctx: Context): String {
        val info = ctx.packageManager.getPackageInfo(
            ctx.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES,
        )
        val cert = info.signingInfo?.apkContentsSigners?.firstOrNull()
            ?: throw IllegalStateException("读不到本应用签名证书（signingInfo 为空）")
        return Abx.toHex(cert.toByteArray())
    }

    // ─────────────────────────── 命令拼装 ───────────────────────────

    /**
     * 拼 app_process 注入命令（不含 `su -c` 前缀，由 [DfRunner] 包起来执行）。
     *
     * @param sourceDir 本应用的 `applicationInfo.sourceDir`（APK 绝对路径）
     * @param keyHex    小写 hex 证书
     * @param mode      ""（完整注入）/ "--uninstall" / "--check" / "--dry-run" / "--dump"
     */
    fun buildAppProcessCommand(sourceDir: String, keyHex: String, mode: String = ""): String {
        val sb = StringBuilder()
        sb.append("CLASSPATH='").append(sourceDir).append("' ")
        sb.append("app_process /system/bin --nice-name=").append(NICE_NAME).append(' ')
        sb.append(ENTRY_CLASS).append(' ')
        sb.append("--keyhex '").append(keyHex).append("' ")
        sb.append("--targets ").append(TARGET_SHARED_USER)
        if (mode.isNotEmpty()) sb.append(' ').append(mode)
        return sb.toString()
    }

    /**
     * 软重启命令：**唯一**能让 PMS 重读 packages.xml 的手段。
     *
     * 必须经 `sh -c` 执行（[DfRunner] 走 `su -c`），`$(pidof ...)` 才会被展开；
     * 软重启只杀 framework，内核模块（KernelSU LKM）不受影响。
     */
    fun buildSoftRebootCommand(): String = "kill \$(pidof system_server)"
}

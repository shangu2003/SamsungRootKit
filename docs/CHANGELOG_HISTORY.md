# 历史更新日志（内部版本时期）

> SamsungRootKit 于 **v1.0.0（2026-10-14）**在 GitHub 首发，版本号体系自此重置。
> 本文件原样保留首发前的内部版本更新记录（v2.x - v6.2.x 时期），仅作历史溯源；
> 文中「用户指令 / 定版」等表述为内部协作语境，按原样归档。
> 当前版本更新日志见 [README.md](../README.md)。

---

## 更新日志

### v6.2.0 (2026-10-08 · 定版发布)

**版本号定版回归 6.2.0（用户指令）+ 内嵌 DFE 升 v40**

- **版本号定版**：6.2.1 / 6.2.2 为同日内部迭代，最终以 **6.2.0**（versionCode 620）号发布；**行为 = v6.2.2 定版形态不变**（主页红/蓝按钮已 root ⇒ 小提示弹窗；切换管理器弹窗保持 2026-10-07 原样）。注意：同日早些时候曾以 6.2.0/620 交付过一版不同内容的构建，本定版与其同号不同内容，真机从 622 降级安装需先卸载或 `adb install -r -d`。
- **内嵌 DFE 升 v40**（`assets/dfreroot/df_reroot.apk`，sha256 `6fd5e485…`）：**KSU 路分派回归修复**——v36 的 chain_sh 分派 `/system/bin/sh $X …` 对两路共用，但 mksh 的 file 操作数按脚本文本解析、不对 ELF execve，KSU 路的 `$X=$K`（bind-mount 的 ksud ELF）后台即死 ⇒ 自动软重启失效 + 30 秒跳转延迟。v40 将 sh 前缀并入 X（仅 Next 路带）、分派行还原直 exec 形态、`[ -x $W ]` 回退，净 +2B；Next 路 wrapper 零改动（v39 行为原样）。KSU/Next 双路真机验证通过。
- 内嵌 DFE 自身链路（stage1.S 提权链 + Next 包装器）随 v40 源码包交付，见 `BUILD_NOTES_DFE`。

### v6.2.2 (2026-10-08)

**主页红/蓝按钮「已 root」提示形态定版（用户二次调整）**

- 6.2.1 曾把已 root 时的行为定为「跳转 DFReroot UI（原规则回归）」；用户定版改为**不跳转、不内联**——与 df 链的「已 root」提醒同款：复用同一个小提示弹窗（`showDfRootAlreadyRootedDialog`），文案「当前系统已经 root，请勿重复操作。」，无需新增字符串。
- dfe 主页按钮的判定顺序与 df 链完全同构：① 自启开 && 变体不同 ⇒「切换下次开机管理器」确认弹窗（**该弹窗及其确认动作保持 2026-10-07 原样不动**，用户明确要求）；② 已 root ⇒ 小提示弹窗；③ 未 root ⇒ 内联跑链（写运行记录）。
- `versionName` 6.2.1 → **6.2.2**，`versionCode` 621 → **622**；内嵌 DFE 仍为 v39（`5afec68a…`）。

### v6.2.1 (2026-10-08)

**主页红蓝按钮「已 root」入口回归 + 内嵌 DFE 同步 v39**

- **主页红/蓝按钮「系统已 root」时回归原规则「跳转 DFReroot UI」**：2026-10-07 的内联改造（`DfRunService` 后台跑链）把该入口顶掉了——已 root 时内联是空转（DFReroot 侧 `/dev/df` 存在即回 rc=0），`dfeBusy` 一闪即灭，用户观感是"点了一下、卡片闪一下、界面进不去"。修复：`onOpenDf` 与「变体切换确认弹窗」的 dfe 分支两处调用点均传 `preferInline = !systemRooted`——已 root ⇒ 跳 UI（原规则），未 root ⇒ 内联跑链（2026-10-07 定义的跑链入口不变）。设置页「打开 DFReroot」卡恒为 UI 入口（`preferInline = false`），分工注释同步修订。
- **内嵌 DFE 同步 v39**（`assets/dfreroot/df_reroot.apk`，sha256 `5afec68a…`）：Next 路包装脚本最终一跳改走 bind-mount 存活通道（30 秒跳转延迟与软重启失效修复）+ worker LPID 判据 `*ksud*|*late-load*`（dfe/df 分歧点修正）。
- **（2026-10-08 上半天随 versionCode 620 交付、当时未登记）**：dfe 跑链结束（DONE 帧）即重算主页状态（`dfeRunFinishedTick`，不再依赖手动切 tab）；dfe「自动软重启」小字简化为「root成功后自动软重启」（中英双语）。
- `versionName` 6.2.0 → **6.2.1**，`versionCode` 620 → **621**

### v6.2.0 (2026-10-07)

**一轮 BUG 修复（主页状态识别 / 开机提醒 / 日志卡高度 / df 链切换管理器确认窗）**

- **主页状态卡 root 识别修复**：`SettingsStore` 的「本次开机已提权」判据由 `elapsedRealtime` 单调比较改为 **boot_id + 开机挂钟起点全等**（新增 `currentBootStartWall()`），冷重启后不再把上一轮标记误判为本次；陈旧键（含旧的 `rooted_elapsed_ms`）自愈清除。
- **开机「安装管理器」提醒误报修复**：`ExploitRunner.isManagerInstalled()` 改为三级降级的「包存在」判据（`getPackageInfo` → 带 `MATCH_DIRECT_BOOT_AWARE/UNAWARE` → `getLaunchIntentForPackage` 兜底），不再受 Direct Boot 组件过滤影响（KSU-Next 无 `directBootAware` 时曾误判为「未安装」）；`DfBootReceiver.refreshManagerNotice()` 的提醒刷新提到 `isArmedThisBoot()` 早退之前，并加 `UserManager.isUserUnlocked` 门控（未解锁时不判不发不撤，推迟到 `BOOT_COMPLETED`）。
- **主页 DF 日志卡高度固定**：`RootKitScreen` 的实时日志视口由 `heightIn(max = 300.dp)`（只封顶不定高，日志写满前卡片随内容长高）改为 `height(130.dp)` + `clipToBounds()`，卡片高度恒定、绝不越界。
- **df 链主页红/蓝按钮「切换 Root 管理器」确认窗修复**：该确认窗的判据需要「当前生效变体」，而 `RootExecutor.resolveWorkingVariant()` 原四级判据在**纯 df 链**场景全部落空（KSU 的 df late-load 不注册 sucompat；`df-boot-ok` / `df-boot.log` 是 DFReroot 才写；两支管理器都装时无区分力）。新增 df 链**自产**的可靠来源：注入成功（`rc == RC_OK`）时记录本次开机所用变体（`DfRootPrefs.KEY_ARMED_VARIANT`），由 `RootExecutor.dfArmedVariantThisBoot()` 作为判据链的一级来源；`DfBootReceiver` 的开机自启路径（直接调 `DfChain.run`）同步记录。
- `versionName` 6.1.3 → **6.2.0**，`versionCode` 613 → **620**

> 本 README 的更新日志自 v6.0.3 起未逐条登记；6.1.0 ~ 6.1.3 期间的改动散见于工程根目录各 `SamsungRootKit_*_更改文档.md`（单包整合、主页双按钮 F1-F6、变体识别、管理器补齐与 dfe 自启、设置页调整等）。

### 2026-10-02 · KernelSU-Next 支持 S26 系列（取消 S26 门禁）

**KernelSU-Next 变体获得 S26（android16-6.12）支持**

- 取消「KernelSU-Next × S26 系列暂不适配」的全部门禁：`PayloadManager.variantSupports()`（KSU-Next × S26 排除逻辑）及其 4 个调用点（`ksudNameFor()` 防御抛错、`builtinKsudNames()` 过滤、`RootExecutor` 早期中止、`RootKitScreen.matchBuiltinKsud()`）与首页「暂不适配」弹窗
- KernelSU-Next 的 ksud 由「按系列分文件」改为**统一多-KMI 单文件** `ksud-samsung-multi-kdp-next`（内嵌 8 个 KMI 的 `kernelsu.ko`，含 `android16-6.12`），与主线 KernelSU 的 `ksud-samsung-multi-kdp`（v6.0.2 起）策略一致；`ksudFileName()` 相应改为按变体返回
  - 移除：`ksud-samsung-android{13-5.15,14-6.1,15-6.6}-kdp-next`
  - 新增：`ksu_next_tools/root/root-samsung-android16-6.12-next`（与同系列 KernelSU root helper 同哈希）
- `AppStrings` 移除 `s26NextUnsupportedTitle/Body/Back` 三串（中/英）；`SHA256SUMS.txt` 同步为 **98 条**
- 注：本次未改动 `versionName`/`versionCode`（仍 6.0.3 / 36），发版前建议评估是否 bump

### v6.0.2 (2026-10-01)

**恢复 DFReroot（持久化 Root）功能**

- 重新纳入整套内嵌 DFReroot 链路：`app/src/main/assets/dfreroot/`（含 `df_reroot.apk`，上游 v2.2.0）与 `app/src/main/java/com/samsung/rootkit/df/` 代码包（8 个文件：DfBridge / DfRunner / DfInject / DfThemeProvider / PackagesXml / Abx / SigKey / SysKey）
- 同步恢复 UI 层（RootKitScreen 入口卡片、四个弹窗、设置页卸载入口、状态行）、字符串表（AppStrings 的 `DfStrings` 类与转发属性）、`DeviceSupport` 的 KMI/`dfSupported` 判定、`AppTheme` 的 DFReroot 配色联动、构建配置（StringFog 排除、proguard keep、AndroidManifest `queries`/`provider`）、构建脚本 `mk_srk_proj.py` 的 DF APK 灌入逻辑
- 说明：D2 修复与开机自启动能力随 `df_reroot.apk` 二进制一并恢复（其实现在 DFReroot fork 仓库，不在本仓库 Kotlin 代码内）

**清理缓存改为自动执行 + 取消重启续跑**

- 「清理缓存」按钮从成功页移除：root 成功后由 App 自动清除设备端 `/data/local/tmp` 下本工具产物（payload / root helper / ksud / staging / KSU socket 等，名单见 `PayloadManager.TMP_ARTIFACTS`），不再需要用户手动点击
- root 开始前（往机内推送 payload / root helper / ksud 三个文件之前）先自动清理一次，保证推送环境干净；执行失败时也直接清理，不再保留进度
- 清理逻辑下沉为 `RootExecutor.cleanDeviceTmpArtifacts()`（两阶段：shizuku shell 删两遍 → 借本应用 su 删 shell 无权限的 KSU 残留），UI 层的手动清理实现（`tryRootDelete` / `showCacheToast`）随之删除
- **不再支持重启续跑**：删除 `BootReceiver`（开机/解锁自动拉起）、`RootState` 跨重启持久化、Manifest 的 receiver 与 `RECEIVE_BOOT_COMPLETED` 权限、UI 自动续跑分支。流程一旦中断（App 被杀 / 设备重启）即从机型选择重新开始
- 保留同一次执行内的 Shizuku 断连等待（属进程内容错，非续跑）

- `versionName` 6.0.1 → **6.0.2**，`versionCode` 33 → **34**

### v6.0.1 (2026-09-29)

**内嵌 DFReroot APK 更新为上游 v2.2.0**

- `app/src/main/assets/dfreroot/df_reroot.apk` 由自编译 v27 换为上游 **v2.2.0**（`com.polygraphene.df.reroot`，versionCode 27 → **29**，versionName 2.2.0）
- 核心变化：`lib/arm64-v8a/libexp.so` 94,376 → **96,056 B**（+1,680），原生 exploit 库更新；其余 3 个 ZIP 条目（`AndroidManifest.xml` / `classes.dex` / `assets/dexopt/baseline.prof`）仅 CRC 变化，116 条中 112 条逐字节一致
- D2 修复开关与开机自启动完整保留（`DmcVault` / `d2fix` / `DmcBootReceiver` / `BootGuard` / `VaultKeeper` 字符串计数与 v27 全等）
- 签名指纹 `09d8801c…74baed`，与 SRK 同密钥（装成 system UID 的前提）
- 注意：该 APK 不登记进 `SHA256SUMS.txt`（由构建脚本按 `out/last_df_apk.txt` 灌入），换版后须同步该 marker，否则会静默回灌旧版

**版本**

- `versionName` 6.0.0 → **6.0.1**，`versionCode` 32 → **33**

> v5.0.8 → v6.0.0 之间未在本日志逐条登记，相关变更见工程根目录的
> `SamsungRootKit_S24门禁_20260926_更改文档.md` 与 `SamsungRootKit_权限修复同步_20260926_更改文档.md`。

### v5.0.8 (2026-09-23)

**KernelSU-Next 物料升级至 v3.4.0（build 33294）**

- Manager APK：`KernelSU_Next_v3.3.0_33214.apk` → `KernelSU_Next_v3.4.0_33294.apk`（10.21 MB → 11.31 MB）
- ksud 三份（`android13-5.15` / `android14-6.1` / `android15-6.6` 的 `-kdp-next`）全部替换为 3.4.0 构建，S23 / S24 / S25 三份互为独立构建（非同一文件改名）
- 三份 Next root helper 经逐字节比对与 v3.3.0 时期**完全相同**，未变动
- `SHA256SUMS.txt` 按磁盘实况重新生成，仍为 **101 条**（payloads 84 / tools 17），逐条回读自校验通过

**新增：推送前自动清理设备端残留**

- 每次执行 root 在推送文件之前，先清理 `/data/local/tmp` 下**本工具自身**的产物，避免上一轮的残留干扰内核态
- 清理范围收窄为 8 个具名条目（`cve-2026-43499` / `cve-2026-43499-root` / `ksud-selected` / `.ksud-stage` / `ksud-s25u-kdp` / `temp_su.sock` / `ksu_manager.apk` / `reboot_guard*`），**不再整目录全清**，不会误删同目录下其他工具的文件
- 结果页「清理缓存」按钮复用同一条收窄后的命令（shell 与 su 两条路径一致）
- 该行为**默认开启、无开关**；清理失败仅告警，不中断流程

**修复**

- **自选文件（SAF 导入 payload + root）被变体门禁误拦**：原门禁在读取设备信息之前执行，自定义条目的系列推断为空即中止，`ksudNameFor()` 的内核版本兜底永远执行不到。现门禁后移至设备信息读取之后，并补 `seriesOfKernel(kernelVer)` 兜底（与 `ksudNameFor` 同口径）；S26 × KernelSU-Next 仍被正确拦截
- **包可见性缺声明**：`<queries>` 内声明的是不存在的 `me.weishu.kernelsu.next`，补上 KernelSU-Next 的真实包名 `com.rifsxd.ksunext`。修复「只装 Next 时主页显示未安装 / 结果页『打开管理器』无反应」
- 结果页「打开管理器」按当前变体优先唤起（`getInstalledKernelSUPackage(context, preferredVariant)`），不再固定先试 `me.weishu.kernelsu`

**设置页调整**

- 「导出运行日志」由独立的「诊断」分区并入「高级选项」，说明小字简化为「分享完整运行记录，便于排查」

**清理**

- 删除零引用字段：`managerVer`、`homeStartGuide`、`settingsDiagnostics` 及 4 个 KernelSU 管理器文案字段（`settingsStatus` / `settingsKsuManager` / `settingsKsuInstalled` / `settingsKsuNotInstalled`）
- `seriesOf` 白名单移除 `gts11wifi`（对应 ksud 已于 v5.0.7 删除，保留死分支会在新增平板机型时静默选错 ksud）

**版本**

- `versionName` 5.0.7 → **5.0.8**，`versionCode` 22 → **23**

### v5.0.7 (2026-09-16)

**新增：KernelSU / KernelSU-Next 双变体**

- 首页新增两个入口按钮：🔴 **KernelSU**（进既有向导）/ 🔵 **KernelSU-Next**
- 两套内核方案各带独立的 ksud、root helper、Manager APK，按「系列 + 变体」取用，互不干扰
- KernelSU-Next 判定内核模块 `kernelsu_next`，管理器包名 `com.rifsxd.ksunext`
- **S26 系列暂不适配 KernelSU-Next**：点蓝色按钮先弹提示不进向导；`RootExecutor` 入口再做一次防御性拦截
- 变体写入持久化状态（`rootState.variant`），设备重启续跑时沿用同一变体，避免串版本
- 首页按钮文案使用全称：`KernelSU` / `KernelSU-Next`（大字），小字为「开始 Root 引导」

**资产目录重构**

- 84 份 `payload/<固件>/<profileId>__root` 全部移出，改为按「系列 + 变体」共享存放：`tools/ksu_tools/root/` 与 `tools/ksu_next_tools/root/`
- 原 `tools/` 下的 ksud、Manager APK、`patch_map.txt` 归入 `tools/ksu_tools/`；KernelSU-Next 物料放 `tools/ksu_next_tools/`
- 删除冗余的 `ksud-samsung-android15-6.6-kdp-x730`（与被保留的 `...-6.6-kdp` 逐字节相同）
- `SHA256SUMS.txt` 整体重新生成：175 条 → **101 条**（84 payload / 7 root / 4 ksud / 3 ksud-next / 2 APK / 1 patch_map）
- 换 root helper 但缓存不失效的问题由来已久，本次把 `tools/` 也纳入资产源签名计算

**payload 补丁**

- `dm3q-S9180ZHS8FZH3` 打 grku 校验 NOP 补丁（偏移 `0x9480`：`c1290054` → `1f2003d5`），与同族已打补丁的 `dm3q-S9180ZCS8FZH3` 完全一致

**版本**

- `versionName` 5.0.6 → **5.0.7**，`versionCode` 21 → **22**

### v5.0.6

**关键修复：late-load staging**

- 修复 27 232 字节版 root helper（S25 全系 / Z Fold7 / W26 / S26 等 42 个机型）在 `--late-load` 时报 `Failed to stage ksud` 的问题
- 成因：该版 root helper 移除了 `stage_ksud_copy()`，而 ksud 只把 `/data/local/tmp/.ksud-stage` 当消费者、自己不创建 → `rename()` 返回 ENOENT
- 修复方式：在 shell 层补 `cp ksud-selected → .ksud-stage`（rms24 分支原本已有等价操作，本次补齐 srk 分支）
- 附带增强：设备端文件预检日志纳入 `.ksud-stage`；`/proc/modules` 未命中时对 `Failed to stage ksud` 直接给出归因结论

**payload 补丁**

- `dm3q-S9180ZCS8FZH3` 打 grku 校验 NOP 补丁

**新功能**

- 提权耗时统计：执行页在提权成功后显示「提权成功，耗时 X.XX 秒」；结果页新增「提权耗时」卡片，统计区间为「400 次内核预热结束 → root 提权成功」

### V2 (2026-08-12)

**新增机型**

- Galaxy Z Flip6 (SM-F741B / SM-F741U) — 板名 b6q
- Galaxy S24 Ultra (SM-S928B) — 固件 S928BXXS6DZG1
- Galaxy S23 Ultra (SM-S9180) — 固件 S9180ZCS8FZG1 / S9180ZHS8FZG1
- Galaxy W26 (SM-W9026) — 固件 W9026ZCS8BZG3

**关键修复**

- `id` 检查输出合并：exploit 循环中 `uid=0(root)` 检测从 `ShellResult.output`（stdout 非空时丢弃 stderr）改为 `2>&1` 合并检查，修复 root helper 同时向不同流输出诊断信息与 uid 时的误判
- 固件不匹配确认对话框：与 `run_root.sh` 的 `force continue? (type YES)` 对齐，固件不匹配时弹窗确认而非静默继续

**返回导航**

- 所有 Wizard 页面（Shizuku 检查 / 机型选择 / 执行中 / 结果）添加系统返回键支持和可见返回按钮
- 执行中页面返回时弹出确认对话框（防止误触中断内核态操作）
- Disclaimer 从设置页进入时显示返回按钮

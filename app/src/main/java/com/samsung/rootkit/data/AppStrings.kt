package com.samsung.rootkit.data

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * All user-facing UI strings, organized by screen/section.
 * Provides both Chinese (zh) and English (en) versions.
 */

/**
 * DFReroot 持久化相关文案集（v5.1.1 从 [AppStrings] 主构造器迁出）。
 *
 * 为什么要单独一个类：[AppStrings] 主构造器参数已达 222 个，而 D8 为构造调用生成的
 * `invoke-direct/range` 需要 `参数数 + 1` 个寄存器，该指令的 AA 计数段只有 8 位
 * （上限 255）。参数数一旦到 255，寄存器需求 256 就会溢出成 0，D8 静默产出非法 dex，
 * ART 校验器直接抛 `VerifyError: invalid arg count (0) in invoke-direct/range`。
 * 拆开后 [AppStrings] 224 个寄存器、本类 34 个寄存器，都有余量。
 */
data class DfStrings(
    // ── DFReroot 持久化集成（v5.1.0）──
    // 首页第三入口（大字 / 小字 / 不可用时的小字）
    val dfEntryTitle: String,
    val dfEntrySub: String,
    val dfEntryDisabledHint: String,
    // 态2 小字：密钥已注入、DFReroot 未装（灰色可用态）
    val dfEntryInjectedHint: String,
    // 2026-10-02 F2 修复：DF 已生效、且本按钮的变体 ≠ 当前工作变体时的小字（参数：变体名）。
    // 目的：让"点哪个按钮 = 下次用哪支管理器"在界面上可见（原两个按钮统一显示 dfEntrySub，
    // 无法区分点击红/蓝会写到哪一支变体）。
    val dfEntrySubSwitch: (String) -> String,
    // 状态卡
    val dfStatusLabel: String,
    val dfStatusActive: String,
    val dfStatusNotConfigured: String,
    val dfStatusInstalledNotActive: String,
    val dfStatusConfigured: String,
    val dfStatusInjected: String,
    // 配置弹窗（root 成功后触发，是**唯一**的配置入口：首页灰按钮不可点）
    val dfEnableTitle: String,
    val dfEnableBody: String,
    val dfEnableConfirm: String,
    val dfEnableCancel: String,
    // 注入完成 / 软重启
    val dfInjectOk: String,
    val dfSoftRebootTitle: String,
    val dfSoftRebootBody: String,
    val dfSoftRebootConfirm: String,
    // 安装
    val dfInstallTitle: String,
    val dfInstallBody: String,
    val dfInstallOk: String,
    // 通用错误前缀
    val dfErrorPrefix: String,
    // 已安装但未生效（缺一次软重启）
    val dfNotActiveTitle: String,
    val dfNotActiveBody: String,
    val dfNotActiveReboot: String,
    // su 不可用（未授权 / KSU 的 su 不在 PATH）→ 引导去管理器授权
    val dfSuNeededTitle: String,
    val dfSuNeededBody: String,
    val dfSuNeededOpenManager: String,
    val dfSuNeededRetry: String,
    // 设置页「卸载 DFReroot」（2026-09-26：settingsDfSection 随分区标题移除而删除）
    val settingsDfUninstall: String,
    val settingsDfUninstallDesc: String,
    // 设置页 dfe 组**总开关**（2026-10-05 新增，与 df 的 settingsDfRootEnabled 对称）。
    // 关掉后：主页两个按钮不再走 dfe（回退 Root 向导）、设置页「配置 DFReroot」卡不渲染、
    // SRK root 成功后不再弹持久化引导。语义上等价于「不再要持久化 root」。
    val settingsDfEntryEnabled: String,
    val settingsDfEntryEnabledDesc: String,
    /** 「DFReroot 总开关」在持久化已配置时点击关闭 → 弹出的提醒标题（2026-10-07）。 */
    val settingsDfEntryEnabledLockedTitle: String,
    /** 该提醒正文：DFReroot 持久化未卸载，不支持关闭（用户定稿文案）。 */
    val settingsDfEntryEnabledLockedBody: String,
    // 设置页 dfe 组**开机自启**（2026-10-05 新增）：与 DFReroot 自己界面上那个开关是
    // **同一份状态**（`/data/system/df-boot-prefs` 的 `autostart` 键）⇒ 双向联动。
    //   · Unavailable：经 su 仍读不到时替换描述文案 —— 未知绝不能显示成「关」，
    //     否则用户会以为开机不再自动 root，而 DFReroot 那边其实还开着。
    //   · Failed：写入后回读不一致（跨 App 写入，可能被后台启动限制拦下）。
    val settingsDfEntryAutostart: String,
    val settingsDfEntryAutostartDesc: String,
    val settingsDfEntryAutostartUnavailable: String,
    val settingsDfEntryAutostartFailed: String,
    // 设置页 dfe 组**D2 修复**（2026-10-07 新增，用户需求：与 DFReroot 里的 D2 开关联动）。
    // D2 = 三星「D2 Error」——abl 判定 vault 的 at_command 为 [1,0,0] 时拒绝进 Odin 下载模式。
    // 开关落在 DFReroot 的 `/data/system/df-boot-prefs` 的 `d2fix` 键（与 autostart 同一文件），
    // 由 DmcBootReceiver 在每次开机消费 ⇒ 与 autostart 卡同构（同一读通道、同一写通道）。
    //    · Unavailable：经 su 仍读不到 ⇒ 替换描述（未知绝不能显示成「关」）。
    //    · Failed：写入后回读不一致。
    val settingsDfEntryD2Fix: String,
    val settingsDfEntryD2FixDesc: String,
    val settingsDfEntryD2FixUnavailable: String,
    val settingsDfEntryD2FixFailed: String,
    // 设置页 dfe 组**自动软重启**（2026-10-08 新增，用户需求：与 DFReroot 里的自动软重启
    // 开关联动）。语义 = 跑完提权链后自动软重启一次，让 LSPosed 生效。
    // ★ 文案沿革（2026-10-08）：Desc 原为「root成功后自动软重启使LSPosed生效」，
    //   用户要求简化 —— 现为「root成功后自动软重启」，与 dfRoot 组同名卡（settingsDfRoot
    //   AutoSoftRebootDesc）措辞一致；英文同步简化为「Soft reboot automatically after
    //   root succeeds」（同样与 dfRoot 组对齐）。
    // 开关落在 DFReroot 的 `/data/system/df-boot-prefs` 的 `softreboot` 键
    // （与 autostart / d2fix 同一文件），由 DFReroot 链脚本 `stage1.S` 的 chain_sh 消费
    // —— **默认关**（2026-10-08 用户定调）。
    //    · Unavailable：经 su 仍读不到 ⇒ 替换描述（未知绝不能显示成「关」）。
    //    · Failed：写入后回读不一致。
    val settingsDfEntryAutoSoftReboot: String,
    val settingsDfEntryAutoSoftRebootDesc: String,
    val settingsDfEntryAutoSoftRebootUnavailable: String,
    val settingsDfEntryAutoSoftRebootFailed: String,
    // 设置页「配置 DFReroot」（2026-10-01 自主页入口迁入；2026-10-05 由「打开 DFReroot」改名）
    val settingsDfOpen: String,
    val settingsDfOpenDesc: String,
    val dfUninstallTitle: String,
    val dfUninstallBody: String,
    val dfUninstallConfirm: String,
    val dfUninstallDone: String,
    // 修复⑦：DF 未装且密钥也不在时，点「卸载」提示无事可做
    val dfUninstallNothing: String,
    // 2026-10-04 修复：卸载**完成后**弹窗不再显示「卸载」，改为「取消 / 软重启」两个对称按钮。
    // 原因：已卸载却仍留「卸载」按钮语义错误（再点一次无事可做），且此时用户最需要的是
    // 「软重启使 packages.xml 的密钥移除完全生效」。补一条完成态标题（正文复用 dfUninstallDone）。
    val dfUninstallDoneTitle: String,
    // 卸载已完成态的弹窗提示（说明为何建议软重启）
    val dfUninstallDoneHint: String,
    // S24 系列（android14-6.1）不支持 DFReroot：点主页入口时弹的提示（2026-09-26）
    val dfUnsupportedTitle: String,
    val dfUnsupportedBody: String,
    val dfUnsupportedConfirm: String,
    // 变体切换确认弹窗（2026-10-01 晚新增；2026-10-02 F1 修复：原先在 UI 里硬编码中文，
    // 现改为走多语言，与项目其它弹窗一致）。
    // 触发：主页点击 KSU / KSU-Next 按钮，且「开机自启动」已开、目标变体 ≠ 当前工作管理器。
    val dfVariantSwitchTitle: String,
    val dfVariantSwitchBody: (String) -> String,   // 参数：目标管理器名（KernelSU / KernelSU-Next）
    val dfVariantSwitchConfirm: String,
    val dfVariantSwitchCancel: String,
    // 2026-10-02 F6 修复：打开 DFReroot 失败时的用户可见提示（原先 runCatching 静默吞异常）。
    val dfOpenFailed: String,
)

/**
 * df（DFRoot / `df.root`，2026-10-04 P3 UI 接线）相关文案集。
 *
 * <p>为什么**不**并入 [DfStrings]：两者是**两个不同的 App** ——
 * [DfStrings] 是 dfe（DFReroot / `com.polygraphene.df.reroot`，回退与持久化侧），
 * 本类是 df（DFRoot / `df.root`，DirtyFrag 主动 root 引擎）。混在一个类里会让
 * 「改的是哪一侧」无法从代码上分辨（简称纪律：df ≠ dfe）。
 *
 * <p>同样不并入 [AppStrings] 主构造器：主构造器参数已达 224 个，D8 的
 * invoke-direct/range 寄存器计数只有 8 位，逼近 255 上限（v5.1.1 闪退根因，见 [DfStrings] 注释）。
 */
data class DfRootStrings(
    // ── 设置页 DFRoot 区（round24 单包整合后只剩三张开关卡）──
    /** 总开关：关掉后主页按钮不再走 DFRoot（回退 dfe / Root 向导），开机自启同时停用。 */
    val settingsDfRootEnabled: String,
    val settingsDfRootEnabledDesc: String,
    /** 开机自启开关（写本应用 DE prefs 并派生自己的 DfBootReceiver 组件态，零 su）。 */
    val settingsDfRootAutostart: String,
    val settingsDfRootAutostartDesc: String,
    /** 自动软重启开关（写本应用 DE prefs 的 auto_soft_reboot）。 */
    val settingsDfRootAutoSoftReboot: String,
    val settingsDfRootAutoSoftRebootDesc: String,
    // ── 状态值（主页状态卡第五行）──
    val dfRootStatusLabel: String,
    /** df 可用（总开关打开且机型静态支持）。 */
    val dfRootStatusSupported: String,
    /** 总开关关闭 ⇒ df 未启用。 */
    val dfRootStatusNotConfigured: String,
    /** 机型静态不支持（= S24，拍板④）。 */
    val dfRootStatusNotSupported: String,
    // ── 主页两个 root 按钮的小字 ──
    /**
     * df（DFRoot）入口的**简约态**小字「一键 Root（DFRoot）」。
     *
     * <p>2026-10-05 起承担两种场景（此前只有第一种，文案带「· 重启免引导」后缀，已按用户要求去掉）：
     * <ol>
     *   <li>系统**尚未取得 root** → 红/蓝两个按钮**都**用这句：此刻还没有「当前工作管理器」可言，
     *       「切换到 X」无从谈起，只需告诉用户点下去就是跑链；</li>
     *   <li>已 root 且本按钮变体 == 当前工作管理器 → 同样用这句（点击即在本变体上再跑一次）。</li>
     * </ol>
     * 仅「已 root 且本按钮变体 ≠ 当前工作管理器」才用 [dfRootEntrySubSwitch]。
     */
    val dfRootEntrySub: String,
    /** df（DFRoot）入口，本按钮变体 ≠ 当前工作管理器时（与 dfe 的 dfEntrySubSwitch 同构）。 */
    val dfRootEntrySubSwitch: (String) -> String,
    // ── 跑链日志（卡内显示；2026-10-05 需求① 由弹窗改为设备卡内动态日志）──
    /** 跑链日志卡的标题（原「DFRoot 执行」弹窗标题，沿用同句）。 */
    val dfRootRunTitle: String,
    /** 跑链中的等待文案。 */
    val dfRootRunning: String,
    /** 跑链成功。 */
    val dfRootResultOk: String,
    /** dfe（DFReroot）内联跑链日志卡的标题（2026-10-07 新增）。与 df 组同款卡片、
     *  但两条链不同名，故独立文案，避免把 DFReroot 的进度写成 "DFRoot"。 */
    val dfeRunTitle: String,
    /** dfe 内联跑链中的等待文案。 */
    val dfeRunning: String,
    /** dfe 内联跑链成功的结论（与 [dfRootResultOk] 同构；2026-10-07 新增）。 */
    val dfeRunOk: String,
    /** dfe 内联跑链失败的结论前缀（结果码在调用点拼接）。 */
    val dfeRunFail: String,
    /** 跑链失败（后接结果码摘要）。 */
    val dfRootResultFail: String,
    /** 通用「关闭」按钮文案（「已 root 提示」「系统版本不支持」两个单按钮弹窗共用）。 */
    val dfRootRunClose: String,
    /** 开关写入失败。 */
    val dfRootSetFailed: String,
    /** 切换成功（下次开机自动 Root 使用 XX）的提示。 */
    val dfRootSwitchNextBoot: (String) -> String,
    // ── 「切换 Root 管理器」确认弹窗 · df 专用文案（2026-10-05）──
    /**
     * df 入口的切换确认正文（参数 = 目标管理器名）。
     *
     * <p>为什么不复用 dfe 的 [DfStrings.dfVariantSwitchBody]：那句末行写的是
     * 「确认后将在**跳转 DFReroot** 时写入对应的 ksud 文件」——那是 dfe 的动作
     * （跳独立 App 并由它覆盖写 ksud）。df 的链就在**本进程内**跑，根本不跳 DFReroot，
     * 照搬会让用户以为要打开另一个 App。故 df 侧单列一句。
     */
    val dfRootVariantSwitchBody: (String) -> String,
    /** 「当前系统已 Root」提示弹窗标题（df 模式下已 root 时点红/蓝按钮的兜底）。 */
    val dfRootAlreadyRootedTitle: String,
    /** 同上弹窗正文。 */
    val dfRootAlreadyRootedBody: String,
    // ── 失败版本门控（2026-10-05 需求②；2026-10-08 改为「连续 3 次」）──
    //
    // 背景（用户需求）：df 提权失败 ⇒ 把**当前固件版本号**记进文件（只记失败），
    // 随后自动关闭 df 总开关并进入 SRK 的 Root 流程；此后手动到设置里打开 df 总开关时，
    // 若文件里的版本号与当前一致 ⇒ 开关打不开 + 弹「当前系统版本不支持」；
    // 若不一致（系统升级过）⇒ 不拦截、放行。
    //
    // ★ 2026-10-08 修正：不再「失败 1 次即锁」。用户原话「DF 链同样是用了漏洞，
    //   是有可能一次不成功的，不代表下次不成功，需要改为记录 3 次 root 不成功，
    //   自动关闭 df 总开关」。故：
    //     · 第 1、2 次失败 ⇒ 用 dfRootFailedRetryBody（含次数），总开关不动；
    //     · 第 3 次失败    ⇒ 用 dfRootFailedFallbackBody（已记录 + 已关总开关）。
    //   阈值常量唯一来源：DfRootFailureRecord.FAIL_THRESHOLD。
    /** 设置页打开总开关被失败记录拦下时的弹窗标题。 */
    val dfRootUnsupportedTitle: String,
    /**
     * 设置页打开总开关被失败记录拦下时的弹窗正文（要交代原因与解除条件）。
     *
     * <p>参数 = **阈值**（[DfRootFailureRecord.FAIL_THRESHOLD]）。做成函数而不是常量串，
     * 是为了让「3」这个数字**只有一个来源** —— 常量改了文案自动跟着改，不会说错话。
     */
    val dfRootUnsupportedBody: (Int) -> String,
    /** 跑链失败后的引导弹窗标题（两种失败共用）。 */
    val dfRootFailedFallbackTitle: String,
    /**
     * **达阈值**（第 3 次）失败后的弹窗正文：已记录该版本 + 已关总开关 + 询问是否进向导。
     *
     * <p>参数 = **阈值**（同上，避免硬编码数字漂移）。
     */
    val dfRootFailedFallbackBody: (Int) -> String,
    /**
     * **未达阈值**（第 1、2 次）失败后的弹窗正文。
     *
     * <p>参数 = (当前连续失败次数, 阈值)。
     *
     * <p>必须与 [dfRootFailedFallbackBody] 分开：那种情况**没有**关总开关、
     * 也**没有**进入黑名单，若沿用后者的正文（「已记录该版本并自动关闭 DFRoot 总开关」）
     * 就是**对用户说假话** —— 用户会以为被锁了，实际可以直接重试。
     */
    val dfRootFailedRetryBody: (Int, Int) -> String,
    /** 同上弹窗的确认按钮（进入 SRK 的 Root 流程）。 */
    val dfRootFailedFallbackConfirm: String,
    /** 系统升级后自动重新启用 DFRoot 的提示。 */
    val dfRootAutoReenabled: String,
    // ── 管理器闸门（round35 方案 B：检测 → 装 → 才跑链）─────────────────────────
    //
    // 2026-10-05 的原需求是「df 取得 Root **后**补装管理器」；2026-10-06 改向：
    // 链内装不上、应用侧在「还没 root」时 su 不可用 ⇒ 一律改走 PackageInstaller（③），
    // 且**必须在跑链之前**（③ 依赖前台弹系统确认窗，链一跑就可能把进程带走）。
    // 于是「Root 成功但管理器没装上」这种状态**不再可能出现**（装不成当场就中止），
    // 原 dfRootManagerInstalled / dfRootManagerInstallFailed 两条随之失效，已删除。
    /** 用户取消了系统安装确认窗 ⇒ 中止，未执行提权。 */
    val dfRootManagerCancelled: String,
    /** 系统安装失败（或安装过程异常）⇒ 中止，未执行提权。 */
    val dfRootManagerFailed: String,
    /** 缺「安装未知应用」授权，无法发起安装 ⇒ 引导去设置页。 */
    val dfRootManagerNoPermission: String,
)

data class AppStrings(
    // ── Navigation ──
    val navHome: String,
    val navHistory: String,
    val navSettings: String,

    // ── Disclaimer Screen ──
    val disclaimerTitle: String,
    val disclaimerWarning: String,
    val disclaimerSubtitle: String,
    val disclaimerScrollPrompt: String,
    val disclaimerCountdown: (Int) -> String,
    val disclaimerNeedBoth: String,
    val disclaimerNeedCaptcha: String,
    val disclaimerScrollDown: String,
    val disclaimerAgreeTerms: String,
    val disclaimerAcceptRisk: String,
    val disclaimerCaptchaLabel: (Int, Int, String) -> String,
    val disclaimerCaptchaWrong: String,
    val disclaimerExit: String,
    val disclaimerConfirm: String,
    // F7310-style disclaimer extras
    val disclaimerBannerTitle: String,
    val disclaimerNoResaleTitle: String,
    val disclaimerNoResaleBody: String,
    val disclaimerWaitCountdown: (Int) -> String,
    val disclaimerReadTimeOk: String,
    val disclaimerScrollToBottom: String,
    val disclaimerMathLabel: (String) -> String,
    val disclaimerMathHint: String,
    val disclaimerMathInvalid: String,
    val disclaimerMathWrong: String,
    val disclaimerUnlockRequirements: String,
    val disclaimerEnter: String,
    val disclaimerSections: List<Pair<String, String>>,

    // ── Shizuku Screen ──
    val shizukuTitle: String,
    val shizukuSubtitle: String,
    val shizukuNoPc: String,
    val shizukuInstalled: String,
    val shizukuDetected: String,
    val shizukuNotInstalled: String,
    val shizukuRunning: String,
    val shizukuRunningOk: String,
    val shizukuNotRunning: String,
    val shizukuAuthorized: String,
    val shizukuGranted: String,
    val shizukuNeedGrant: String,
    val shizukuReady: String,
    val shizukuReadyDesc: String,
    val shizukuDownload: String,
    val shizukuDownloadDesc: String,
    val shizukuOpen: String,
    val shizukuOpenDesc: String,
    val shizukuRequest: String,
    val shizukuRequestDesc: String,
    val shizukuManualHint: String,
    val shizukuRecheck: String,
    val shizukuHowToTitle: String,
    val shizukuHowToBody: String,
    val shizukuOpenWirelessDebug: String,
    val shizukuOpenWirelessDebugDesc: String,
    val shizukuWifiNotConnected: String,
    val shizukuRebootGuideTitle: String,
    val shizukuRebootGuideBody: String,

    // ── Model Select Screen ──
    val modelSelectTitle: String,
    val modelDeviceInfo: String,
    val modelDeviceModel: String,
    val modelDeviceBoard: String,
    val modelDeviceKernel: String,
    val modelDeviceFingerprint: String,
    val modelAutoMatch: String,
    val modelNoMatchError: String,
    val modelAutoDetect: String,
    val modelAutoDetectDesc: String,
    val modelMatched: String,
    val modelFirmware: (String) -> String,
    val modelBoard: (String) -> String,
    val modelStatus: (String) -> String,
    val modelRegion: (String) -> String,
    val modelConfirmRoot: String,
    val modelManualSelect: String,
    val modelCollapse: String,
    val modelExpand: (Int) -> String,
    val modelNoLocalPayload: String,

    // ── Executing Screen ──
    val execStepEnvCheck: String,
    val execStepEnvCheckDesc: String,
    val execStepPush: String,
    val execStepPushDesc: String,
    val execStepExploit: String,
    val execStepExploitDesc: String,
    val execStepKsu: String,
    val execStepKsuDesc: String,
    val execRooting: String,
    val execKeepScreenOn: String,
    val execRunning: String,
    val execStageDeviceCheck: String,
    val execStagePayloadMatch: String,
    val execStagePushFiles: String,
    val execStageWarmup: String,
    val execStageExploit: String,
    val execStageVerifyRoot: String,
    val execStageKsuLoad: String,
    val execStageInstallApp: String,
    val execStageDone: String,
    val execStageFailed: String,
    val execLiveLog: String,
    val execPreparing: String,

    // ── Result Screen ──
    val resultSuccess: String,
    val resultFailed: String,
    val resultSuccessDesc: String,
    val resultFailedDesc: String,
    val resultComplete: String,
    val resultIncomplete: String,
    val resultTempRoot: String,
    val resultRebootRetry: String,
    val resultUsageNotes: String,
    val resultUsageBody: String,
    val resultExecLog: String,
    val resultRunAgain: String,
    val resultCancel: String,
    val resultRetry: String,
    val resultRebootHint: String,

    // ── Home Page ──
    val homeKsuLoaded: String,          // "KernelSU 已加载"（KSU 已工作）
    val homeKsuNextLoaded: String,      // "KernelSU-Next 已加载"（Next 在工作，2026-10-01 深夜新增）
    val homeKsuNotLoaded: String,       // "KernelSU 未加载"（三态：有root无KSU / 无root无KSU 共用标题）
    val homeNotRooted: String,
    val homeRootMemoryDesc: String,     // "ROOT 为内存态，重启后失效"
    val homeSystemNotRooted: String,    // "当前系统未ROOT"（三态-1：无root无KSU 副标题）
    val homeSystemRooted: String,       // "当前系统已ROOT"（有 root 权限时的副标题）
    val homeTapToStart: String,
    val homeNotAuthorizedTitle: String,
    val homeNotAuthorizedDesc: String,
    val homeShizukuNotRunningDesc: String,
    val homeShizukuRunning: String,
    val homeShizukuNotRunning: String,
    val homeShizukuLabel: String,
    val homeSdkLabel: String,
    val homeAndroidLabel: String,
    // ── KSU 变体入口（首页两个按钮：红=KernelSU / 蓝=KernelSU-Next；大字为变体名、小字为动作）──
    val ksuVariantKsu: String,
    val ksuVariantNext: String,
    val homeStartGuideSub: String,
    val homeDevice: String,
    val homeFirmwareFp: String,
    val homeFirmwareVersion: String,
    val homeKernel: String,
    val homeLoading: String,
    val homeDeviceUnavailable: String,
    val homeShizukuAuthorized: String,
    val homeShizukuAuthorizedNotRunning: String,
    val homeShizukuNeedAuth: String,
    val homeHowStep1Title: String,
    val homeHowStep1Desc: String,
    val homeHowStep2Title: String,
    val homeHowStep2Desc: String,
    val homeHowStep3Title: String,
    val homeHowStep3Desc: String,
    val homeHowItWorks: String,
    val homeSafetyTitle: String,
    val homeSafetyBody: String,

    // ── DFReroot 持久化集成 ──
    // ⚠️ 这 33 个文案**不要**直接搬回主构造器。原因见类体里转发属性的注释：
    //    主构造器参数数达到 255 时，D8 生成的 invoke-direct/range 寄存器计数
    //    溢出 8 位 AA 段，静默变成 0，产出非法 dex（v5.1.1 闪退根因）。
    //    主构造器当前 223 个参数（含本字段），距上限还有余量。
    val df: DfStrings,

    // ── df（DFRoot / df.root）集成（2026-10-04 P3）──
    // 与上面的 df（= DFReroot）是**两个不同的 App**，文案集也不共用，见 [DfRootStrings] 注释。
    val dfRoot: DfRootStrings,

    // ── Theme Mode Selector ──
    val themeModeSystem: String,
    val themeModeLight: String,
    val themeModeDark: String,

    // ── RunLog (History) Page ──
    // 2026-10-05：运行记录页从「只有 SRK 向导的 root 尝试」扩成「SRK / df / dfe 三源」，
    // 结局也从两态（成功/失败）扩成三态（+ 运行中断），故新增下列文案。
    val historyTitle: String,
    val historyClear: String,
    val historyEmpty: String,
    val historyEmptyDesc: String,
    val historySuccess: String,
    val historyFailed: String,
    /** 跑到一半进程就没了（被杀 / 软重启 / 崩溃）——既非成功也非失败，单列一态。 */
    val historyAborted: String,
    /** 标题下的一行提示：告诉用户「长按可导出/分享」（长按是隐藏操作，必须写出来）。 */
    val historyLongPressHint: String,
    /** 长按弹窗里的「导出到文件」（SAF，用户自选保存位置）。 */
    val historyExport: String,
    /** 长按弹窗里的「分享」（系统分享面板）。 */
    val historyShare: String,
    /** 分享主题 / 导出文件名语义（如「SamsungRootKit 运行日志」）。 */
    val historyExportSubject: String,
    /** 导出到文件成功 / 失败的一次性提示。 */
    val historyExportDone: String,
    val historyExportFailed: String,
    /** 来源显示名（SRK / DFRoot / DFReroot）。品牌名两语言一致，故用函数而非三个常量。 */
    val historySourceName: (RunSource) -> String,

    // ── Settings Page ──
    val settingsTitle: String,
    val settingsAppearance: String,
    val settingsDynamicColor: String,
    val settingsDynamicColorDesc: String,
    val settingsGeneral: String,
    val settingsLanguage: String,
    val settingsLanguageDesc: String,
    val settingsClearLog: String,
    val settingsClearLogDesc: String,
    val settingsReDisclaimer: String,
    val settingsReDisclaimerDesc: String,
    val settingsAbout: String,
    val settingsAboutDesc: String,
    val settingsAdvanced: String,
    // ── 设置页「分组标题」文案（2026-10-05 恢复分组）──
    // 四组依次：通用（复用上方 [settingsGeneral]）/ DFRoot / DFReroot / 其它。
    // 后三个是新键；前两组品牌名两语言一致（与 historySourceName 同例），
    // 仍集中在本类而不是散在 UI 里，便于统一改版。
    val settingsDfRootGroup: String,
    val settingsDfRerootGroup: String,
    val settingsOther: String,

    // ── Language Labels ──
    val langFollowSystem: String,
    val langChinese: String,
    val langEnglish: String,

    // ── Theme Labels ──
    val themeFollowSystem: String,
    val themeLight: String,
    val themeDark: String,

    // ── About Dialog ──
    val aboutTitle: String,
    val aboutVersion: (String) -> String,
    val aboutDescription: String,
    val aboutSupportedModels: String,
    val aboutKsuDesc: String,
    val aboutRmgDesc: String,
    val aboutSpecialThanks: String,
    val aboutSpecialThanks1: String,
    val aboutSpecialThanks2: String,
    val aboutSpecialThanks3: String,
    val aboutSpecialThanks4: String,
    val aboutNoResale: String,
    val aboutAuthor: String,
    val aboutDismiss: String,

    // ── Back Navigation ──
    val commonBack: String,
    val commonCancel: String,
    val commonConfirm: String,
    val execCancelButton: String,
    val execCancelDialogTitle: String,
    val execCancelDialogBody: String,
    val execCancelDialogContinue: String,
    val execCancelDialogAbort: String,
    val disclaimerBack: String,
    val modelCancelButton: String,
    val firmwareMismatchTitle: String,
    val firmwareMismatchBody: (String, String) -> String,
    val firmwareMismatchForce: String,
    val firmwareMismatchCancel: String,
) {
    // ── 转发属性（v5.1.1）──
    // 这 33 个文案已迁到 [DfStrings]。保留同名只读属性，使既有引用点
    // （`strings.dfEntryTitle` 等）零改动；R8 会把它们内联掉。
    //
    // 之所以不能直接留在主构造器：主构造器参数已达 224 个，D8 为构造调用生成的
    // invoke-direct/range 还要额外占一个寄存器放 receiver，参数数一旦到 255，
    // 寄存器需求就是 256 —— 而该指令的 AA 计数段只有 8 位（上限 255），
    // D8 会静默截断成 0，产出非法 dex，ART 校验器直接抛
    //   VerifyError: invalid arg count (0) in invoke-direct/range
    // v5.1.1 就是这样在启动时闪退的（见 BuildNotes / 修复报告）。
    val dfEntryTitle: String get() = df.dfEntryTitle
    val dfEntrySub: String get() = df.dfEntrySub
    val dfEntryDisabledHint: String get() = df.dfEntryDisabledHint
    val dfEntryInjectedHint: String get() = df.dfEntryInjectedHint
    val dfEntrySubSwitch: (String) -> String get() = df.dfEntrySubSwitch
    val dfStatusLabel: String get() = df.dfStatusLabel
    val dfStatusActive: String get() = df.dfStatusActive
    val dfStatusNotConfigured: String get() = df.dfStatusNotConfigured
    val dfStatusInstalledNotActive: String get() = df.dfStatusInstalledNotActive
    val dfStatusConfigured: String get() = df.dfStatusConfigured
    val dfStatusInjected: String get() = df.dfStatusInjected
    val dfEnableTitle: String get() = df.dfEnableTitle
    val dfEnableBody: String get() = df.dfEnableBody
    val dfEnableConfirm: String get() = df.dfEnableConfirm
    val dfEnableCancel: String get() = df.dfEnableCancel
    val dfInjectOk: String get() = df.dfInjectOk
    val dfSoftRebootTitle: String get() = df.dfSoftRebootTitle
    val dfSoftRebootBody: String get() = df.dfSoftRebootBody
    val dfSoftRebootConfirm: String get() = df.dfSoftRebootConfirm
    val dfInstallTitle: String get() = df.dfInstallTitle
    val dfInstallBody: String get() = df.dfInstallBody
    val dfInstallOk: String get() = df.dfInstallOk
    val dfErrorPrefix: String get() = df.dfErrorPrefix
    val dfNotActiveTitle: String get() = df.dfNotActiveTitle
    val dfNotActiveBody: String get() = df.dfNotActiveBody
    val dfNotActiveReboot: String get() = df.dfNotActiveReboot
    val dfSuNeededTitle: String get() = df.dfSuNeededTitle
    val dfSuNeededBody: String get() = df.dfSuNeededBody
    val dfSuNeededOpenManager: String get() = df.dfSuNeededOpenManager
    val dfSuNeededRetry: String get() = df.dfSuNeededRetry
    val dfUnsupportedTitle: String get() = df.dfUnsupportedTitle
    val dfUnsupportedBody: String get() = df.dfUnsupportedBody
    val dfUnsupportedConfirm: String get() = df.dfUnsupportedConfirm
    val dfVariantSwitchTitle: String get() = df.dfVariantSwitchTitle
    val dfVariantSwitchBody: (String) -> String get() = df.dfVariantSwitchBody
    val dfVariantSwitchConfirm: String get() = df.dfVariantSwitchConfirm
    val dfVariantSwitchCancel: String get() = df.dfVariantSwitchCancel
    val dfOpenFailed: String get() = df.dfOpenFailed
    val settingsDfUninstall: String get() = df.settingsDfUninstall
    val settingsDfUninstallDesc: String get() = df.settingsDfUninstallDesc
    val settingsDfEntryEnabled: String get() = df.settingsDfEntryEnabled
    val settingsDfEntryEnabledDesc: String get() = df.settingsDfEntryEnabledDesc
    val settingsDfEntryEnabledLockedTitle: String get() = df.settingsDfEntryEnabledLockedTitle
    val settingsDfEntryEnabledLockedBody: String get() = df.settingsDfEntryEnabledLockedBody
    val settingsDfEntryAutostart: String get() = df.settingsDfEntryAutostart
    val settingsDfEntryAutostartDesc: String get() = df.settingsDfEntryAutostartDesc
    val settingsDfEntryAutostartUnavailable: String get() = df.settingsDfEntryAutostartUnavailable
    val settingsDfEntryAutostartFailed: String get() = df.settingsDfEntryAutostartFailed
    val settingsDfEntryD2Fix: String get() = df.settingsDfEntryD2Fix
    val settingsDfEntryD2FixDesc: String get() = df.settingsDfEntryD2FixDesc
    val settingsDfEntryD2FixUnavailable: String get() = df.settingsDfEntryD2FixUnavailable
    val settingsDfEntryD2FixFailed: String get() = df.settingsDfEntryD2FixFailed
    val settingsDfEntryAutoSoftReboot: String get() = df.settingsDfEntryAutoSoftReboot
    val settingsDfEntryAutoSoftRebootDesc: String get() = df.settingsDfEntryAutoSoftRebootDesc
    val settingsDfEntryAutoSoftRebootUnavailable: String
        get() = df.settingsDfEntryAutoSoftRebootUnavailable
    val settingsDfEntryAutoSoftRebootFailed: String
        get() = df.settingsDfEntryAutoSoftRebootFailed
    val settingsDfOpen: String get() = df.settingsDfOpen
    val settingsDfOpenDesc: String get() = df.settingsDfOpenDesc
    val dfUninstallTitle: String get() = df.dfUninstallTitle
    val dfUninstallBody: String get() = df.dfUninstallBody
    val dfUninstallConfirm: String get() = df.dfUninstallConfirm
    val dfUninstallDone: String get() = df.dfUninstallDone
    val dfUninstallNothing: String get() = df.dfUninstallNothing
    val dfUninstallDoneTitle: String get() = df.dfUninstallDoneTitle
    val dfUninstallDoneHint: String get() = df.dfUninstallDoneHint

    // ── df（DFRoot，本包内嵌的 root 引擎）转发 ──
    // 单包整合后本区只剩 28 项：三张开关卡的文案 + 状态值 + 主页按钮小字 + 跑链相关文案
    // + 切换确认正文（df 专用）/ 已 root 提示 + 失败版本门控（2026-10-05 需求③④②新增）。
    // 原「打开/安装/卸载 / 广播回传 / 兜底文件 / 系统安装器」相关的 36 项已随集成删除。
    val settingsDfRootEnabled: String get() = dfRoot.settingsDfRootEnabled
    val settingsDfRootEnabledDesc: String get() = dfRoot.settingsDfRootEnabledDesc
    val settingsDfRootAutostart: String get() = dfRoot.settingsDfRootAutostart
    val settingsDfRootAutostartDesc: String get() = dfRoot.settingsDfRootAutostartDesc
    val settingsDfRootAutoSoftReboot: String get() = dfRoot.settingsDfRootAutoSoftReboot
    val settingsDfRootAutoSoftRebootDesc: String get() = dfRoot.settingsDfRootAutoSoftRebootDesc
    val dfRootStatusLabel: String get() = dfRoot.dfRootStatusLabel
    val dfRootStatusSupported: String get() = dfRoot.dfRootStatusSupported
    val dfRootStatusNotConfigured: String get() = dfRoot.dfRootStatusNotConfigured
    val dfRootStatusNotSupported: String get() = dfRoot.dfRootStatusNotSupported
    val dfRootEntrySub: String get() = dfRoot.dfRootEntrySub
    val dfRootEntrySubSwitch: (String) -> String get() = dfRoot.dfRootEntrySubSwitch
    val dfRootRunTitle: String get() = dfRoot.dfRootRunTitle
    val dfRootRunning: String get() = dfRoot.dfRootRunning
    val dfRootResultOk: String get() = dfRoot.dfRootResultOk
    val dfeRunTitle: String get() = dfRoot.dfeRunTitle
    val dfeRunning: String get() = dfRoot.dfeRunning
    val dfeRunOk: String get() = dfRoot.dfeRunOk
    val dfeRunFail: String get() = dfRoot.dfeRunFail
    val dfRootResultFail: String get() = dfRoot.dfRootResultFail
    val dfRootRunClose: String get() = dfRoot.dfRootRunClose
    val dfRootSetFailed: String get() = dfRoot.dfRootSetFailed
    val dfRootSwitchNextBoot: (String) -> String get() = dfRoot.dfRootSwitchNextBoot
    val dfRootVariantSwitchBody: (String) -> String get() = dfRoot.dfRootVariantSwitchBody
    val dfRootAlreadyRootedTitle: String get() = dfRoot.dfRootAlreadyRootedTitle
    val dfRootAlreadyRootedBody: String get() = dfRoot.dfRootAlreadyRootedBody
    val dfRootUnsupportedTitle: String get() = dfRoot.dfRootUnsupportedTitle
    val dfRootUnsupportedBody: (Int) -> String get() = dfRoot.dfRootUnsupportedBody
    val dfRootFailedFallbackTitle: String get() = dfRoot.dfRootFailedFallbackTitle
    val dfRootFailedFallbackBody: (Int) -> String get() = dfRoot.dfRootFailedFallbackBody
    val dfRootFailedRetryBody: (Int, Int) -> String get() = dfRoot.dfRootFailedRetryBody
    val dfRootFailedFallbackConfirm: String get() = dfRoot.dfRootFailedFallbackConfirm
    val dfRootAutoReenabled: String get() = dfRoot.dfRootAutoReenabled
    val dfRootManagerCancelled: String get() = dfRoot.dfRootManagerCancelled
    val dfRootManagerFailed: String get() = dfRoot.dfRootManagerFailed
    val dfRootManagerNoPermission: String get() = dfRoot.dfRootManagerNoPermission
}


object AppStringResources {
    val zh = AppStrings(
        navHome = "主页",
        navHistory = "运行记录",
        navSettings = "设置",

        disclaimerTitle = "安全免责声明",
        disclaimerWarning = "重要安全警告 · 请务必先阅读",
        disclaimerSubtitle = "在继续使用之前，请务必逐条仔细阅读以下条款。点击下方「确认并继续」即代表您已完全理解并自愿接受全部内容。",
        disclaimerScrollPrompt = "请向下滚动阅读完整条款",
        disclaimerCountdown = { s -> "请等待阅读倒计时结束（还剩 ${s}s）" },
        disclaimerNeedBoth = "请勾选下列两项确认",
        disclaimerNeedCaptcha = "请完成安全验证",
        disclaimerScrollDown = "\u2193 请滚动阅读至底部",
        disclaimerAgreeTerms = "我已逐条阅读并完全理解上述全部条款与风险。",
        disclaimerAcceptRisk = "我理解使用临时 ROOT 的风险，并自愿承担一切后果。",
        disclaimerCaptchaLabel = { a, b, op -> "安全验证：计算 $a $op $b = ?" },
        disclaimerCaptchaWrong = "答案不正确，请重新计算",
        disclaimerExit = "退出",
        disclaimerConfirm = "确认并继续",
        disclaimerBannerTitle = "风险提示",
        disclaimerNoResaleTitle = "禁止倒卖声明",
        disclaimerNoResaleBody = "本软件仅供个人技术研究使用，严禁以任何形式进行倒卖、转售或商业化分发。任何未经授权的二次打包与售卖行为均属违规，开发者保留追究法律责任的权利。",
        disclaimerWaitCountdown = { s -> "请阅读全部条款（还需 ${s} 秒）" },
        disclaimerReadTimeOk = "已可确认，请阅读至底部并勾选",
        disclaimerScrollToBottom = "请滑动至页面底部以继续",
        disclaimerMathLabel = { q -> "安全验证：请计算 $q" },
        disclaimerMathHint = "请输入计算结果",
        disclaimerMathInvalid = "请输入有效的数字",
        disclaimerMathWrong = "答案不正确，请重新计算",
        disclaimerUnlockRequirements = "需勾选两项确认并完成安全验证后方可进入",
        disclaimerEnter = "进入",
        disclaimerSections = listOf(
            "设备安全" to "本工具通过内核级漏洞获取临时 ROOT 权限，涉及对系统的底层修改。操作可能带来系统不稳定、应用崩溃或数据丢失，极端情况下可能导致内核错误（Kernel Panic）或无法开机。获取 ROOT 本质上绕过了 Android 安全机制，绝非零风险操作。",
            "使用限制" to "严禁将本工具用于任何违法活动，包括未授权修改系统分区、刷入非官方固件、破解或欺骗应用与服务、侵犯他人合法权益、网络攻击或窃取信息。特别注意：严禁刷入 Magisk 等会造成永久修改的 ROOT 方案——三星内核完整性校验与 AVB/dm-verity 会使 boot 分区修改必然引发 Kernel Panic 或反复重启。本工具仅提供「重启即失效」的临时 ROOT。",
            "保修与 Knox" to "您确认理解操作可能导致变砖或不可恢复的数据丢失；设备厂商可能因此拒绝保修；三星 Knox 熔断将永久禁用 Samsung Pay、安全文件夹等依赖项。",
            "责任声明" to "开发者不对任何直接或间接的财产损失、数据丢失或设备损坏承担责任，一切后果由您自行承担。本工具按「现状」提供，不提供任何明示或暗示的担保。",
            "技术局限" to "本工具基于公开披露的漏洞 CVE-2026-43499，厂商随时可能通过系统更新进行修补。ROOT 成功率与稳定性取决于具体机型与固件版本，不保证在所有设备上可用。操作前请务必备份重要数据。",
            "知情同意" to "继续操作即表示您确认：已满 18 周岁并具有完全民事行为能力；已充分理解上述全部风险；自愿承担一切后果；不会将本工具用于任何非法目的。若您不同意上述任何条款，请立即退出并卸载本应用。",
        ),

        shizukuTitle = "Shizuku 授权",
        shizukuSubtitle = "通过 Shizuku 获取 ADB 级权限",
        shizukuNoPc = "无需连接电脑即可执行 root 操作",
        shizukuInstalled = "Shizuku 已安装",
        shizukuDetected = "已检测到 Shizuku",
        shizukuNotInstalled = "未安装 Shizuku",
        shizukuRunning = "Shizuku 服务运行中",
        shizukuRunningOk = "服务正常运行",
        shizukuNotRunning = "Shizuku 未启动，请打开 Shizuku 并启动服务",
        shizukuAuthorized = "已授权本应用",
        shizukuGranted = "权限已授予",
        shizukuNeedGrant = "请在 Shizuku 中授权本应用",
        shizukuReady = "已就绪",
        shizukuReadyDesc = "点击继续开始 Root",
        shizukuDownload = "下载 Shizuku",
        shizukuDownloadDesc = "前往 Shizuku 官网下载",
        shizukuOpen = "打开 Shizuku",
        shizukuOpenDesc = "启动 Shizuku 服务",
        shizukuRequest = "请求授权",
        shizukuRequestDesc = "在 Shizuku 中授权本应用",
        shizukuManualHint = "若未弹出 Shizuku 授权框，请手动打开 Shizuku \u2192 已授权应用 \u2192 找到「Samsung Root」并开启",
        shizukuRecheck = "重新检查",
        shizukuHowToTitle = "Shizuku 启动方式",
        shizukuHowToBody = "方式一：通过无线调试启动（Android 11+）\n方式二：通过 USB 连接电脑用 adb 启动",
        shizukuOpenWirelessDebug = "打开「无线调试」设置",
        shizukuOpenWirelessDebugDesc = "一键跳转到系统「无线调试」，开启后即可本地配对启动 Shizuku",
        shizukuWifiNotConnected = "未检测到已连接的 WLAN 网络：三星固件开启「无线调试」前必须先连上一个 Wi-Fi 网络（仅需局域网配对，无需互联网）。请先连接 WLAN 再点击上方按钮。",
        shizukuRebootGuideTitle = "重启后如何再次 Root（无需连电脑）",
        shizukuRebootGuideBody = "① 连接任意一个 Wi-Fi 网络（仅需局域网配对，不需要互联网）\n② 打开本 App → 点击上方「打开无线调试」\n③ 在系统设置中开启「无线调试」并点击「使用配对码配对设备」\n④ 配对成功后 Shizuku 自动启动，即可直接执行 Root\n\n注意：三星固件在开启「无线调试」前会要求先连上 WLAN 网络，否则会提示「请连接到 WLAN 网络」。",

        modelSelectTitle = "选择机型",
        modelDeviceInfo = "设备信息",
        modelDeviceModel = "型号",
        modelDeviceBoard = "板名",
        modelDeviceKernel = "内核",
        modelDeviceFingerprint = "指纹",
        modelAutoMatch = "自动匹配",
        modelNoMatchError = "未能自动匹配到对应机型 payload，请使用手动选择",
        modelAutoDetect = "自动读取并匹配",
        modelAutoDetectDesc = "读取手机型号与版本并选择 payload",
        modelMatched = "已匹配",
        modelFirmware = { "固件: $it" },
        modelBoard = { "板名: $it" },
        modelStatus = { "状态: $it" },
        modelRegion = { "地区: $it" },
        modelConfirmRoot = "确认无误，开始 Root",
        modelManualSelect = "手动选择",
        modelCollapse = "收起列表",
        modelExpand = { n -> "展开机型列表 ($n)" },
        modelNoLocalPayload = "无本地 payload（需联网下载）",

        execStepEnvCheck = "环境检查",
        execStepEnvCheckDesc = "检测设备连接与 Shizuku 权限",
        execStepPush = "推送文件",
        execStepPushDesc = "上传 payload 与 root helper 到设备",
        execStepExploit = "触发漏洞",
        execStepExploitDesc = "执行 CVE-2026-43499 exploit",
        execStepKsu = "安装 KernelSU",
        execStepKsuDesc = "加载内核模块并安装管理器",
        execRooting = "正在 Root",
        execKeepScreenOn = "请保持屏幕常亮，不要退出应用",
        execRunning = "执行中...",
        execStageDeviceCheck = "正在检查设备连接",
        execStagePayloadMatch = "正在匹配 payload",
        execStagePushFiles = "正在推送文件",
        execStageWarmup = "正在预热内核",
        execStageExploit = "正在触发 exploit",
        execStageVerifyRoot = "正在验证 root",
        execStageKsuLoad = "正在加载 KernelSU",
        execStageInstallApp = "正在安装 KernelSU Manager",
        execStageDone = "操作完成",
        execStageFailed = "操作失败",
        execLiveLog = "实时日志",
        execPreparing = "准备中...",

        resultSuccess = "Root 成功",
        resultFailed = "Root 失败",
        resultSuccessDesc = "KernelSU 已加载，重启后权限失效",
        resultFailedDesc = "本次 exploit 未成功，可重启后重试",
        resultComplete = "操作完成",
        resultIncomplete = "操作未完成",
        resultTempRoot = "点击打开 KernelSU",
        resultRebootRetry = "请重启设备后再次运行本应用",
        resultUsageNotes = "使用须知",
        resultUsageBody = "root 是内存态，重启后失效；再次 root 直接重跑本 App\n二次运行若 operation failed：先重启设备再跑",
        resultExecLog = "执行日志",
        resultRunAgain = "再次执行",
        resultCancel = "取消",
        resultRetry = "重试",
        resultRebootHint = "若本次未成功：请先重启设备，再重新进入本 App 执行一次（root 为内存态，二次运行需重启恢复内核状态）。",

        homeKsuLoaded = "KernelSU 已加载",
        homeKsuNextLoaded = "KernelSU-Next 已加载",
        homeKsuNotLoaded = "KernelSU 未加载",
        homeNotRooted = "尚未获取 Root",
        homeRootMemoryDesc = "ROOT 为内存态，重启后失效",
        homeSystemNotRooted = "当前系统未ROOT",
        homeSystemRooted = "当前系统已ROOT",
        homeTapToStart = "点击下方开始 Root 引导",
        homeNotAuthorizedTitle = "尚未获取 Shizuku 授权",
        homeNotAuthorizedDesc = "点击开始授权",
        homeShizukuNotRunningDesc = "点击跳转 Shizuku",
        homeShizukuRunning = "运行中",
        homeShizukuNotRunning = "未运行",
        homeShizukuLabel = "Shizuku",
        homeSdkLabel = "SDK",
        homeAndroidLabel = "Android",
        ksuVariantKsu = "KernelSU",
        ksuVariantNext = "KernelSU-Next",
        homeStartGuideSub = "开始 Root 引导",
        homeDevice = "设备",
        homeFirmwareFp = "固件指纹",
        homeFirmwareVersion = "固件版本",
        homeKernel = "内核",
        homeLoading = "读取中\u2026",
        homeDeviceUnavailable = "请授予 Shizuku 权限后重试",
        homeShizukuAuthorized = "已授权",
        homeShizukuAuthorizedNotRunning = "未运行 · 已授权",
        homeShizukuNeedAuth = "待授权",
        homeHowStep1Title = "检测设备与 Shizuku 授权",
        homeHowStep1Desc = "确认机型匹配并获取高级权限",
        homeHowStep2Title = "触发内核漏洞 CVE-2026-43499",
        homeHowStep2Desc = "利用公开漏洞获取临时内核权限",
        homeHowStep3Title = "加载 KernelSU LKM",
        homeHowStep3Desc = "挂载内核模块完成 ROOT",
        homeHowItWorks = "工作原理",
        homeSafetyTitle = "设备安全提醒",
        homeSafetyBody = "请确保已经清除锁屏密码并已备份数据，避免 root 不成功无法进入刷机模式，本工具仅提供临时 root,重启后失效。",
        df = DfStrings(
            dfEntryTitle = "DFReroot",
            dfEntrySub = "持久化 Root · 重启免引导",
            dfEntryDisabledHint = "不可用 · 需先完成配置",
            dfEntryInjectedHint = "已注入 · 点此安装 DFReroot",
            dfEntrySubSwitch = { variant -> "持久化 Root · 切换到 $variant" },

            dfStatusLabel = "DFReroot 持久化",
            dfStatusActive = "可用",
            dfStatusNotConfigured = "未配置",
            dfStatusInstalledNotActive = "已安装未生效 · 需重启",
            dfStatusConfigured = "已配置",
            dfStatusInjected = "已注入",

            dfEnableTitle = "启用 DFReroot 持久化？",
            dfEnableBody = "将把本应用的签名证书注入系统 /data/system/packages.xml 里 " +
                "android.uid.system 的记录中，使 DFReroot 能以系统 UID 常驻。\n\n" +
                "· 这是一次不可逆的系统级修改（仅保留单份 .bak-df-installer 备份）\n" +
                "· 注入后需要软重启（屏幕会短暂黑一下）\n" +
                "· 配置完成后，每次重启都能在没有 Shizuku、没有临时 root 的情况下恢复 root\n" +
                "· 可在「设置 - 卸载 DFReroot」中撤销",
            dfEnableConfirm = "注入并继续",
            dfEnableCancel = "暂不启用",

            dfInjectOk = "签名证书已写入 /data/system/packages.xml。下一步需要软重启系统，让 PackageManager 重新读取 packages.xml。",
            dfSoftRebootTitle = "执行软重启？",
            dfSoftRebootBody = "软重启会终止并重启系统框架（system_server），屏幕会黑一下并在数秒后恢复。" +
                "内核不重启，已加载的 KernelSU 不受影响。",
            dfSoftRebootConfirm = "软重启",

            dfInstallTitle = "安装 DFReroot",
            dfInstallBody = "证书已注入并已完成软重启，现在安装 DFReroot。" +
                "安装后它会以系统 UID 运行，此后每次重启都能在没有 Shizuku、没有临时 root 的情况下恢复 root。",
            dfInstallOk = "DFReroot 已安装并以系统 UID 运行，持久化已生效。",

            dfErrorPrefix = "失败",

            dfNotActiveTitle = "DFReroot 尚未生效",
            dfNotActiveBody = "DFReroot 已安装，但还没有完成软重启，所以它仍以普通应用身份运行，无法用于提权。\n\n" +
                "需要软重启一次系统框架（屏幕会短暂黑一下），之后它才会以系统 UID 生效。",
            dfNotActiveReboot = "立即软重启",

            dfSuNeededTitle = "需要给 SamsungRootKit 授予 root",
            dfSuNeededBody = "读写 /data/system/packages.xml 必须由 root 完成，但当前调用 su 失败：" +
                "系统里找不到可用的 su，或本应用还没有获得授权。\n\n" +
                "请到 KernelSU / KernelSU-Next 管理器的「超级用户」列表里给 SamsungRootKit 授予 root 权限，" +
                "然后回到这里重试。",
            dfSuNeededOpenManager = "继续，打开管理器",
            dfSuNeededRetry = "重试",

            dfUnsupportedTitle = "暂不适配",
            dfUnsupportedBody = "当前机型属于 S24 系列（Android 6.1 内核），DFReroot 持久化暂不适配。\n\n" +
                "本设备不会注入签名证书，也不会安装 DFReroot。",
            dfUnsupportedConfirm = "知道了",

            dfVariantSwitchTitle = "切换 Root 管理器",
            dfVariantSwitchBody = { target ->
                "检测到当前正在工作的是另一支管理器，且「开机自启动」已打开。\n" +
                    "下次开机自动 root 是否切换到 $target 管理器？\n" +
                    "确认后将在跳转 DFReroot 时写入对应的 ksud 文件。"
            },
            dfVariantSwitchConfirm = "确认切换",
            dfVariantSwitchCancel = "取消",
            dfOpenFailed = "无法打开 DFReroot。请确认已安装 DFReroot 后重试。",

            settingsDfUninstall = "卸载 DFReroot",
            settingsDfUninstallDesc = "移除注入的密钥并卸载DF",
            settingsDfOpen = "配置 DFReroot",
            settingsDfOpenDesc = "配置或进入 DFReroot",
            settingsDfEntryEnabled = "DFReroot 总开关",
            settingsDfEntryEnabledDesc = "关闭后不支持root持久化",
            settingsDfEntryEnabledLockedTitle = "无法关闭",
            settingsDfEntryEnabledLockedBody = "DFReroot持久化功能没卸载，不支持关闭，请卸载后再关闭",
            settingsDfEntryAutostart = "开机自启动",
            // 2026-10-07 用户定调：小字改为此文案（原「开机过程中由 DFReroot 自动恢复 root」）。
            settingsDfEntryAutostartDesc = "开机过程中自动恢复root",
            settingsDfEntryAutostartUnavailable = "读取失败（需要 root 授权）",
            settingsDfEntryAutostartFailed = "开机自启动写入失败，请重试。",
            settingsDfEntryD2Fix = "D2 修复",
            // 2026-10-07 用户定调：小字改为此文案（原「开机自动解除 Odin 下载模式的 D2 错误锁」）。
            settingsDfEntryD2FixDesc = "开机自动移除Odin刷机模式锁",
            settingsDfEntryD2FixUnavailable = "读取失败（需要 root 授权）",
            settingsDfEntryD2FixFailed = "D2 修复开关写入失败，请重试。",
            // 2026-10-08 用户定调：加在「开机自启动」下面，与它同构。
            settingsDfEntryAutoSoftReboot = "自动软重启",
            settingsDfEntryAutoSoftRebootDesc = "root成功后自动软重启",
            settingsDfEntryAutoSoftRebootUnavailable = "读取失败（需要 root 授权）",
            settingsDfEntryAutoSoftRebootFailed = "自动软重启开关写入失败，请重试。",
            dfUninstallTitle = "卸载 DFReroot 持久化？",
            dfUninstallBody = "将按顺序执行：\n" +
                "1. 卸载 DFReroot 应用\n" +
                "2. 从 /data/system/packages.xml 移除本应用的签名证书\n\n" +
                "两处都是系统级修改，建议完成后重启一次使其完全生效。" +
                "若想再次使用，重新走一次 root 流程即可恢复。",
            dfUninstallConfirm = "卸载",
            dfUninstallDone = "已卸载。建议重启一次，使密钥移除完全生效。",
            dfUninstallNothing = "当前未配置 DFReroot：密钥未注入、应用未安装，无需卸载。",
            dfUninstallDoneTitle = "卸载完成",
            dfUninstallDoneHint = "建议软重启一次，使 packages.xml 中的证书移除完全生效" +
                "（PMS 只在开机时读取该文件）。",
        ),

        // ── df（DFRoot / df.root）──
        dfRoot = DfRootStrings(
            settingsDfRootEnabled = "DFRoot 总开关",
            settingsDfRootEnabledDesc = "关闭后取消DFRoot功能",
            settingsDfRootAutostart = "开机自启动",
            settingsDfRootAutostartDesc = "开机过程中自动root",
            settingsDfRootAutoSoftReboot = "自动软重启",
            settingsDfRootAutoSoftRebootDesc = "root成功后自动软重启",
            dfRootStatusLabel = "DFRoot",
            dfRootStatusSupported = "已支持",
            dfRootStatusNotConfigured = "未配置",
            dfRootStatusNotSupported = "暂不支持",
            dfRootEntrySub = "一键 Root（DFRoot）",
            dfRootEntrySubSwitch = { variant -> "DFRoot 一键 Root · 切换到 $variant" },
            dfRootRunTitle = "DFRoot 执行",
            dfRootRunning = "正在跑 DFRoot 链，请勿退出…",
            dfRootResultOk = "DFRoot 已成功获取 Root。",
            dfeRunTitle = "DFReroot 执行",
            dfeRunning = "正在跑 DFReroot 链，请勿退出…",
            dfeRunOk = "DFReroot 已成功获取 Root。",
            dfeRunFail = "DFReroot 跑链失败",
            dfRootResultFail = "DFRoot 跑链失败",
            dfRootRunClose = "关闭",
            dfRootSetFailed = "开关写入失败，请重试。",
            dfRootSwitchNextBoot = { name -> "已切换 · 下次开机自动 Root 使用 $name" },
            dfRootVariantSwitchBody = { target ->
                "检测到当前正在工作的是另一支管理器，且「开机自启动」已打开。\n" +
                    "下次开机自动 root 是否切换到 $target 管理器？\n" +
                    "确认后将在 DFRoot 下次开机自动 Root 时写入对应的 ksud 文件。"
            },
            dfRootAlreadyRootedTitle = "当前系统已 Root",
            dfRootAlreadyRootedBody = "当前系统已经 root，请勿重复操作。",
            dfRootUnsupportedTitle = "当前系统版本不支持",
            dfRootUnsupportedBody = { n ->
                "DFRoot 在当前系统版本上已连续 $n 次提权失败，暂不能开启 DFRoot。\n" +
                    "系统升级后会自动重新启用。"
            },
            dfRootFailedFallbackTitle = "DFRoot 提权失败",
            dfRootFailedFallbackBody = { n ->
                "DFRoot 在当前系统版本上已连续 $n 次提权失败，" +
                    "已记录该版本并自动关闭 DFRoot 总开关。\n是否进入 SRK 的 Root 流程？"
            },
            dfRootFailedRetryBody = { cur, total ->
                "本次 DFRoot 提权失败（连续第 $cur 次；满 $total 次才会记录该版本并自动关闭 DFRoot 总开关）。\n" +
                    "此漏洞本身存在单次失败的可能，不代表下次不成功 —— 建议重启设备后重试。\n" +
                    "是否进入 SRK 的 Root 流程？"
            },
            dfRootFailedFallbackConfirm = "进入 Root 流程",
            dfRootAutoReenabled = "系统版本已变化，DFRoot 已自动重新启用。",
            dfRootManagerCancelled = "已取消安装管理器，本次未执行提权。",
            dfRootManagerFailed = "管理器安装失败，本次未执行提权。",
            dfRootManagerNoPermission = "需要「安装未知应用」授权才能安装管理器，请在设置中允许后重试。",
        ),

        themeModeSystem = "跟随系统",
        themeModeLight = "浅色",
        themeModeDark = "深色",

        historyTitle = "运行记录",
        historyClear = "清空日志",
        historyEmpty = "暂无记录",
        historyEmptyDesc = "SRK / DFRoot / DFReroot 的每次运行都会记录在这里",
        historySuccess = "运行成功",
        historyFailed = "运行失败",
        historyAborted = "运行中断",
        historyLongPressHint = "长按任意记录可导出或分享",
        historyExport = "导出",
        historyShare = "分享",
        historyExportSubject = "SamsungRootKit 运行日志",
        historyExportDone = "已导出到所选位置",
        historyExportFailed = "导出失败",
        historySourceName = { src ->
            when (src) {
                RunSource.SRK -> "SRK"
                RunSource.DF -> "DFRoot"
                RunSource.DFE -> "DFReroot"
            }
        },

        settingsTitle = "设置",
        settingsAppearance = "外观",
        settingsDynamicColor = "使用系统动态颜色",
        settingsDynamicColorDesc = "根据壁纸自动生成配色",
        settingsGeneral = "通用",
        settingsLanguage = "语言",
        settingsLanguageDesc = "界面语言",
        settingsClearLog = "清空日志",
        settingsClearLogDesc = "清除全部运行记录",
        settingsReDisclaimer = "重新阅读免责声明",
        settingsReDisclaimerDesc = "查看完整风险条款",
        settingsAbout = "关于",
        settingsAboutDesc = "版本 / 漏洞 / 支持版本 / 致谢",
        settingsAdvanced = "高级",
        settingsDfRootGroup = "DFRoot",
        settingsDfRerootGroup = "DFReroot",
        settingsOther = "其它",

        langFollowSystem = "跟随系统",
        langChinese = "中文",
        langEnglish = "English",

        themeFollowSystem = "跟随系统",
        themeLight = "浅色",
        themeDark = "深色",

        aboutTitle = "关于 Samsung Root",
        aboutVersion = { "版本: $it" },
        aboutDescription = "基于公开漏洞 CVE-2026-43499 和 CVE-2026-43284 实现内核级临时 ROOT。",
        aboutSupportedModels = "payload适配支持机型",
        aboutKsuDesc = "内核级 ROOT 方案官网",
        aboutRmgDesc = "UI 设计参考与致谢",
        aboutSpecialThanks = "特别感谢",
        aboutSpecialThanks1 = "Root 所用的 payload 文件大多数收集于网络，感谢原作者为此项目作出的巨大贡献。",
        aboutSpecialThanks2 = "特别感谢\"yettocom\"(电报)适配了 KernelSU 升级版本和 KernelSU-Next 版本。",
        aboutSpecialThanks3 = "感谢\"Www\"(电报)为此 APP 构建了初始版本。",
        aboutSpecialThanks4 = "感谢所有参与机型适配、协助测试的机友。",
        aboutNoResale = "禁止以任何形式对本软件进行倒卖或商用。",
        aboutAuthor = "作者：大尾巴狼，Www",
        aboutDismiss = "知道了",

        commonBack = "返回",
        commonCancel = "取消",
        commonConfirm = "确认",
        execCancelButton = "取消执行",
        execCancelDialogTitle = "确认取消 Root 流程？",
        execCancelDialogBody = "执行中强制取消可能让设备处于不一致状态，且本次进度不会保存。建议让流程自然结束。",
        execCancelDialogContinue = "继续执行",
        execCancelDialogAbort = "仍然取消",
        disclaimerBack = "返回",
        modelCancelButton = "取消",
        firmwareMismatchTitle = "固件不匹配",
        firmwareMismatchBody = { expected, actual -> "当前固件 ($actual) 与所选机型 ($expected) 不一致。强行继续极可能 Root 失败甚至触发内核错误。是否强制继续？" },
        firmwareMismatchForce = "强制继续",
        firmwareMismatchCancel = "取消并返回",
    )

    val en = AppStrings(
        navHome = "Home",
        navHistory = "History",
        navSettings = "Settings",

        disclaimerTitle = "Safety Disclaimer",
        disclaimerWarning = "Important Safety Warning \u00b7 Please Read First",
        disclaimerSubtitle = "Please read the following terms carefully before proceeding. Clicking \"Confirm & Continue\" means you fully understand and voluntarily accept all content.",
        disclaimerScrollPrompt = "Please scroll down to read all terms",
        disclaimerCountdown = { s -> "Wait for the reading countdown (${s}s remaining)" },
        disclaimerNeedBoth = "Please check both confirmations",
        disclaimerNeedCaptcha = "Please complete security verification",
        disclaimerScrollDown = "\u2193 Scroll down to the bottom",
        disclaimerAgreeTerms = "I have read and fully understand all the terms and risks above.",
        disclaimerAcceptRisk = "I understand the risks of temporary ROOT and voluntarily accept all consequences.",
        disclaimerCaptchaLabel = { a, b, op -> "Security check: $a $op $b = ?" },
        disclaimerCaptchaWrong = "Incorrect answer, please try again",
        disclaimerExit = "Exit",
        disclaimerConfirm = "Confirm & Continue",
        disclaimerBannerTitle = "Risk Warning",
        disclaimerNoResaleTitle = "No Resale Notice",
        disclaimerNoResaleBody = "This software is for personal technical research only. Resale, redistribution, or commercial distribution in any form is strictly prohibited. Unauthorized repackaging or selling is a violation; the developer reserves the right to pursue legal action.",
        disclaimerWaitCountdown = { s -> "Please read all terms (${s}s remaining)" },
        disclaimerReadTimeOk = "Ready to confirm — scroll to the bottom and check",
        disclaimerScrollToBottom = "Scroll to the bottom of the page to continue",
        disclaimerMathLabel = { q -> "Security check: solve $q" },
        disclaimerMathHint = "Enter the result",
        disclaimerMathInvalid = "Please enter a valid number",
        disclaimerMathWrong = "Incorrect answer, please try again",
        disclaimerUnlockRequirements = "Check both boxes and complete the security check to enter",
        disclaimerEnter = "Enter",
        disclaimerSections = listOf(
            "Device Safety" to "This tool obtains temporary ROOT privileges through kernel-level vulnerabilities, involving low-level system modifications. Operations may cause system instability, app crashes, or data loss. In extreme cases, it may lead to Kernel Panic or failure to boot. Obtaining ROOT inherently bypasses Android security mechanisms and is never risk-free.",
            "Usage Restrictions" to "It is strictly prohibited to use this tool for any illegal activities, including unauthorized modification of system partitions, flashing unofficial firmware, cracking or spoofing apps and services, infringing on others' rights, network attacks, or information theft. Special note: DO NOT flash permanent ROOT solutions like Magisk \u2014 Samsung's kernel integrity checks and AVB/dm-verity will cause the boot partition modification to inevitably trigger Kernel Panic or boot loops. This tool only provides temporary ROOT that \u201cdisappears on reboot\u201d.",
            "Warranty & Knox" to "You acknowledge that operations may cause bricking or irreversible data loss; the device manufacturer may refuse warranty; Knox fuse blowing will permanently disable Samsung Pay, Secure Folder, and other dependent features.",
            "Disclaimer of Liability" to "The developer assumes no responsibility for any direct or indirect property loss, data loss, or device damage. All consequences are borne by you. This tool is provided \"as-is\" without any express or implied warranty.",
            "Technical Limitations" to "This tool is based on the publicly disclosed CVE-2026-43499 vulnerability, which the vendor may patch at any time via system updates. ROOT success rate and stability depend on the specific device model and firmware version; availability on all devices is not guaranteed. Please back up important data before operation.",
            "Informed Consent" to "By proceeding, you confirm that: you are 18+ years old with full civil capacity; you fully understand all the risks above; you voluntarily assume all consequences; you will not use this tool for any illegal purpose. If you disagree with any of the above terms, please exit and uninstall this app immediately.",
        ),

        shizukuTitle = "Shizuku Authorization",
        shizukuSubtitle = "Get ADB-level permission via Shizuku",
        shizukuNoPc = "Root without connecting to a PC",
        shizukuInstalled = "Shizuku Installed",
        shizukuDetected = "Shizuku detected",
        shizukuNotInstalled = "Shizuku not installed",
        shizukuRunning = "Shizuku Running",
        shizukuRunningOk = "Service running normally",
        shizukuNotRunning = "Shizuku is not running. Please open Shizuku and start the service",
        shizukuAuthorized = "App Authorized",
        shizukuGranted = "Permission granted",
        shizukuNeedGrant = "Please authorize this app in Shizuku",
        shizukuReady = "Ready",
        shizukuReadyDesc = "Tap to start Root",
        shizukuDownload = "Download Shizuku",
        shizukuDownloadDesc = "Go to Shizuku website to download",
        shizukuOpen = "Open Shizuku",
        shizukuOpenDesc = "Start Shizuku service",
        shizukuRequest = "Request Authorization",
        shizukuRequestDesc = "Authorize this app in Shizuku",
        shizukuManualHint = "If the Shizuku authorization dialog doesn't appear, manually open Shizuku \u2192 Authorized Apps \u2192 find \"Samsung Root\" and enable it",
        shizukuRecheck = "Re-check",
        shizukuHowToTitle = "How to Start Shizuku",
        shizukuHowToBody = "Method 1: Via Wireless Debugging (Android 11+)\nMethod 2: Via USB ADB from a computer",
        shizukuOpenWirelessDebug = "Open Wireless Debugging",
        shizukuOpenWirelessDebugDesc = "Jump to system Wireless Debugging; enable it to pair locally and start Shizuku",
        shizukuWifiNotConnected = "No connected WLAN detected: Samsung firmware requires a Wi-Fi network connected before enabling Wireless Debugging (LAN pairing only, internet not needed). Please connect to WLAN first, then tap the button above.",
        shizukuRebootGuideTitle = "Root again after reboot (no PC needed)",
        shizukuRebootGuideBody = "① Connect to any Wi-Fi network (LAN pairing only, no internet needed)\n② Open this app → tap \"Open Wireless Debugging\" above\n③ In system settings, enable Wireless Debugging and tap \"Pair device with pairing code\"\n④ Once paired, Shizuku starts automatically and you can run Root directly\n\nNote: Samsung firmware asks you to connect to WLAN before enabling Wireless Debugging, otherwise it shows \"Please connect to a WLAN network\".",

        modelSelectTitle = "Select Model",
        modelDeviceInfo = "Device Info",
        modelDeviceModel = "Model",
        modelDeviceBoard = "Board",
        modelDeviceKernel = "Kernel",
        modelDeviceFingerprint = "Fingerprint",
        modelAutoMatch = "Auto Match",
        modelNoMatchError = "No matching payload found. Please use manual selection",
        modelAutoDetect = "Auto Detect & Match",
        modelAutoDetectDesc = "Read device model & firmware to select payload",
        modelMatched = "Matched",
        modelFirmware = { "Firmware: $it" },
        modelBoard = { "Board: $it" },
        modelStatus = { "Status: $it" },
        modelRegion = { "Region: $it" },
        modelConfirmRoot = "Confirm & Start Root",
        modelManualSelect = "Manual Select",
        modelCollapse = "Collapse",
        modelExpand = { n -> "Expand list ($n)" },
        modelNoLocalPayload = "No local payload (download required)",

        execStepEnvCheck = "Environment Check",
        execStepEnvCheckDesc = "Check device connection & Shizuku permission",
        execStepPush = "Push Files",
        execStepPushDesc = "Upload payload & root helper to device",
        execStepExploit = "Trigger Exploit",
        execStepExploitDesc = "Execute CVE-2026-43499 exploit",
        execStepKsu = "Install KernelSU",
        execStepKsuDesc = "Load kernel module & install manager",
        execRooting = "Rooting...",
        execKeepScreenOn = "Keep screen on, do not exit the app",
        execRunning = "Running...",
        execStageDeviceCheck = "Checking device connection",
        execStagePayloadMatch = "Matching payload",
        execStagePushFiles = "Pushing files",
        execStageWarmup = "Warming up kernel",
        execStageExploit = "Triggering exploit",
        execStageVerifyRoot = "Verifying root",
        execStageKsuLoad = "Loading KernelSU",
        execStageInstallApp = "Installing KernelSU Manager",
        execStageDone = "Complete",
        execStageFailed = "Failed",
        execLiveLog = "Live Log",
        execPreparing = "Preparing...",

        resultSuccess = "Root Success",
        resultFailed = "Root Failed",
        resultSuccessDesc = "KernelSU loaded. Permissions lost after reboot",
        resultFailedDesc = "Exploit was unsuccessful. Reboot and retry",
        resultComplete = "Complete",
        resultIncomplete = "Incomplete",
        resultTempRoot = "Tap to open KernelSU",
        resultRebootRetry = "Please reboot the device and run this app again",
        resultUsageNotes = "Usage Notes",
        resultUsageBody = "Root is memory-based, lost on reboot. To root again, simply re-run this app.\nIf you see \"operation failed\" on second run: reboot the device first, then run again.",
        resultExecLog = "Execution Log",
        resultRunAgain = "Run Again",
        resultCancel = "Cancel",
        resultRetry = "Retry",
        resultRebootHint = "If this run did not succeed: reboot the device first, then re-open this app and run once more (root is in-memory; a second run needs a reboot to recover kernel state).",

        homeKsuLoaded = "KernelSU Loaded",
        homeKsuNextLoaded = "KernelSU-Next Loaded",
        homeKsuNotLoaded = "KernelSU Not Loaded",
        homeNotRooted = "Not Rooted",
        homeRootMemoryDesc = "ROOT is memory-based, lost on reboot",
        homeSystemNotRooted = "System not rooted",
        homeSystemRooted = "System rooted",
        homeTapToStart = "Tap below to start Root guide",
        homeNotAuthorizedTitle = "Shizuku not authorized",
        homeNotAuthorizedDesc = "Tap to grant authorization",
        homeShizukuNotRunningDesc = "Tap to open Shizuku",
        homeShizukuRunning = "Running",
        homeShizukuNotRunning = "Not running",
        homeShizukuLabel = "Shizuku",
        homeSdkLabel = "SDK",
        homeAndroidLabel = "Android",
        ksuVariantKsu = "KernelSU",
        ksuVariantNext = "KernelSU-Next",
        homeStartGuideSub = "Start Root Guide",
        homeDevice = "Device",
        homeFirmwareFp = "Firmware Fingerprint",
        homeFirmwareVersion = "Firmware Version",
        homeKernel = "Kernel",
        homeLoading = "Reading\u2026",
        homeDeviceUnavailable = "Please grant Shizuku permission and retry",
        homeShizukuAuthorized = "Authorized",
        homeShizukuAuthorizedNotRunning = "Not running · authorized",
        homeShizukuNeedAuth = "Need auth",
        homeHowStep1Title = "Check device & Shizuku authorization",
        homeHowStep1Desc = "Confirm model match and obtain elevated permissions",
        homeHowStep2Title = "Trigger kernel exploit CVE-2026-43499",
        homeHowStep2Desc = "Use publicly disclosed exploit for temporary kernel access",
        homeHowStep3Title = "Load KernelSU LKM",
        homeHowStep3Desc = "Mount kernel module to complete ROOT",
        homeHowItWorks = "How It Works",
        homeSafetyTitle = "Device Safety Reminder",
        homeSafetyBody = "Ensure the lock screen password is REMOVED and data is backed up, to avoid being locked out of recovery/download mode if root fails. This tool only provides temporary root, lost on reboot.",
        df = DfStrings(
            dfEntryTitle = "DFReroot",
            dfEntrySub = "Persistent root · survives reboot",
            dfEntryDisabledHint = "Unavailable · configure first",
            dfEntryInjectedHint = "Injected · tap to install DFReroot",
            dfEntrySubSwitch = { variant -> "Persistent root · switch to $variant" },

            dfStatusLabel = "DFReroot",
            dfStatusActive = "Ready",
            dfStatusNotConfigured = "Not configured",
            dfStatusInstalledNotActive = "Installed, not active · reboot",
            dfStatusConfigured = "Configured",
            dfStatusInjected = "Injected",

            dfEnableTitle = "Enable DFReroot persistence?",
            dfEnableBody = "This injects this app's signing certificate into the system's " +
                "/data/system/packages.xml record for android.uid.system, letting DFReroot run " +
                "with system UID.\n\n" +
                "· This is an irreversible system-level change (a single .bak-df-installer backup)\n" +
                "· A soft reboot is required afterwards (brief black screen)\n" +
                "· Once configured, root can be restored on every boot without Shizuku or temporary root\n" +
                "· Can be undone via Settings - Uninstall DFReroot",
            dfEnableConfirm = "Inject and continue",
            dfEnableCancel = "Not now",

            dfInjectOk = "Signing certificate is in /data/system/packages.xml. A soft reboot is required so PackageManager re-reads packages.xml.",
            dfSoftRebootTitle = "Soft reboot now?",
            dfSoftRebootBody = "This kills and restarts the system framework (system_server). The screen blinks " +
                "and recovers in a few seconds. The kernel does not restart, so loaded KernelSU is unaffected.",
            dfSoftRebootConfirm = "Soft reboot",

            dfInstallTitle = "Install DFReroot",
            dfInstallBody = "The certificate is injected and the soft reboot is done. Now install DFReroot. " +
                "Once installed it runs with system UID, so root can be restored on every boot " +
                "without Shizuku or temporary root.",
            dfInstallOk = "DFReroot is installed and running as system UID; persistence is active.",

            dfErrorPrefix = "Failed",

            dfNotActiveTitle = "DFReroot not active yet",
            dfNotActiveBody = "DFReroot is installed, but the soft reboot has not happened yet, so it still " +
                "runs as an ordinary app and cannot be used for privilege escalation.\n\n" +
                "A soft reboot of the system framework is required once (the screen blinks briefly); " +
                "after that it takes effect as system UID.",
            dfNotActiveReboot = "Soft reboot now",

            dfSuNeededTitle = "Grant root to SamsungRootKit",
            dfSuNeededBody = "Writing /data/system/packages.xml requires root, but invoking su failed: " +
                "no usable su was found, or this app has not been granted root yet.\n\n" +
                "Please grant root to SamsungRootKit in the superuser list of the KernelSU / " +
                "KernelSU-Next manager, then come back and retry.",
            dfSuNeededOpenManager = "Continue, open manager",
            dfSuNeededRetry = "Retry",

            dfUnsupportedTitle = "Not supported yet",
            dfUnsupportedBody = "This device is an S24 series model (Android 6.1 kernel). DFReroot " +
                "persistence is not supported yet.\n\n" +
                "No certificate will be injected and DFReroot will not be installed on this device.",
            dfUnsupportedConfirm = "OK",

            dfVariantSwitchTitle = "Switch root manager",
            dfVariantSwitchBody = { target ->
                "Another manager is currently active, and \"Auto-start on boot\" is enabled.\n" +
                    "Switch automatic root on next boot to $target?\n" +
                    "If confirmed, the matching ksud file is written when opening DFReroot."
            },
            dfVariantSwitchConfirm = "Switch",
            dfVariantSwitchCancel = "Cancel",
            dfOpenFailed = "Unable to open DFReroot. Make sure DFReroot is installed, then retry.",

            settingsDfUninstall = "Uninstall DFReroot",
            settingsDfUninstallDesc = "Remove injected key and uninstall DF",
            settingsDfOpen = "Configure DFReroot",
            settingsDfOpenDesc = "Configure or open DFReroot",
            settingsDfEntryEnabled = "DFReroot master switch",
            settingsDfEntryEnabledDesc = "When off, persistent root is unsupported",
            settingsDfEntryEnabledLockedTitle = "Cannot disable",
            settingsDfEntryEnabledLockedBody = "DFReroot persistence is still installed, so it cannot be disabled. Please uninstall it first.",
            settingsDfEntryAutostart = "Autostart on boot",
            settingsDfEntryAutostartDesc = "Restores root automatically during boot",
            settingsDfEntryAutostartUnavailable = "Read failed (root permission required)",
            settingsDfEntryAutostartFailed = "Failed to write autostart setting. Please retry.",
            settingsDfEntryD2Fix = "D2 fix",
            settingsDfEntryD2FixDesc = "Removes the Odin flash-mode lock at boot",
            settingsDfEntryD2FixUnavailable = "Read failed (root permission required)",
            settingsDfEntryD2FixFailed = "Failed to write D2 fix setting. Please retry.",
            settingsDfEntryAutoSoftReboot = "Auto soft reboot",
            settingsDfEntryAutoSoftRebootDesc =
                "Soft reboot automatically after root succeeds",
            settingsDfEntryAutoSoftRebootUnavailable = "Read failed (root permission required)",
            settingsDfEntryAutoSoftRebootFailed =
                "Failed to write auto soft reboot setting. Please retry.",
            dfUninstallTitle = "Uninstall DFReroot persistence?",
            dfUninstallBody = "This runs, in order:\n" +
                "1. Uninstall the DFReroot app\n" +
                "2. Remove this app's signing certificate from /data/system/packages.xml\n\n" +
                "Both are system-level changes; rebooting afterwards is recommended. " +
                "To use DFReroot again, just run the root flow once more.",
            dfUninstallConfirm = "Uninstall",
            dfUninstallDone = "Uninstalled. Rebooting once is recommended so the key removal fully takes effect.",
            dfUninstallNothing = "DFReroot is not configured: key not injected and app not installed — nothing to uninstall.",
            dfUninstallDoneTitle = "Uninstall complete",
            dfUninstallDoneHint = "A soft reboot is recommended so the certificate removal from " +
                "packages.xml fully takes effect (PMS only reads that file at boot).",
        ),

        // ── df (DFRoot / df.root) ──
        dfRoot = DfRootStrings(
            settingsDfRootEnabled = "DFRoot master switch",
            settingsDfRootEnabledDesc = "When off, DFRoot is disabled",
            settingsDfRootAutostart = "Autostart on boot",
            settingsDfRootAutostartDesc = "Root automatically during boot",
            settingsDfRootAutoSoftReboot = "Auto soft reboot",
            settingsDfRootAutoSoftRebootDesc = "Soft reboot automatically after root succeeds",
            dfRootStatusLabel = "DFRoot",
            dfRootStatusSupported = "Supported",
            dfRootStatusNotConfigured = "Not configured",
            dfRootStatusNotSupported = "Not supported",
            dfRootEntrySub = "One-tap root (DFRoot)",
            dfRootEntrySubSwitch = { variant -> "DFRoot one-tap root · switch to $variant" },
            dfRootRunTitle = "DFRoot run",
            dfRootRunning = "Running the DFRoot chain — do not leave…",
            dfRootResultOk = "DFRoot obtained root successfully.",
            dfeRunTitle = "DFReroot run",
            dfeRunning = "Running the DFReroot chain — do not leave…",
            dfeRunOk = "DFReroot obtained root successfully.",
            dfeRunFail = "DFReroot run failed",
            dfRootResultFail = "DFRoot run failed",
            dfRootRunClose = "Close",
            dfRootSetFailed = "Failed to save the switch state, please retry.",
            dfRootSwitchNextBoot = { name -> "Switched · next-boot auto-root uses $name" },
            dfRootVariantSwitchBody = { target ->
                "Another manager is currently working and \"Autostart on boot\" is on.\n" +
                    "Switch next-boot auto-root to $target?\n" +
                    "Once confirmed, DFRoot will use the matching ksud file on the next boot."
            },
            dfRootAlreadyRootedTitle = "System already rooted",
            dfRootAlreadyRootedBody = "The system is already rooted. Please do not repeat the operation.",
            dfRootUnsupportedTitle = "Current system version not supported",
            dfRootUnsupportedBody = { n ->
                "DFRoot has failed to escalate $n times in a row on the current system " +
                    "version, so DFRoot cannot be enabled for now.\nIt will be re-enabled " +
                    "automatically after a system update."
            },
            dfRootFailedFallbackTitle = "DFRoot escalation failed",
            dfRootFailedFallbackBody = { n ->
                "DFRoot has failed to escalate $n times in a row on the current system version. " +
                    "The version has been recorded and the DFRoot master switch was turned off " +
                    "automatically.\nProceed to the SRK root flow?"
            },
            dfRootFailedRetryBody = { cur, total ->
                "This DFRoot escalation attempt failed (consecutive failure #$cur; the version " +
                    "is recorded and the DFRoot master switch is turned off automatically only " +
                    "after $total).\nA single failure does not mean the next attempt will fail — " +
                    "rebooting the device and retrying is recommended.\nProceed to the SRK root flow?"
            },
            dfRootFailedFallbackConfirm = "Start root flow",
            dfRootAutoReenabled = "System version changed — DFRoot has been re-enabled automatically.",
            dfRootManagerCancelled = "Manager installation was cancelled — root has not been attempted.",
            dfRootManagerFailed = "Manager installation failed — root has not been attempted.",
            dfRootManagerNoPermission = "The \"Install unknown apps\" permission is required to install the manager. Please allow it in Settings and retry.",
        ),

        themeModeSystem = "System",
        themeModeLight = "Light",
        themeModeDark = "Dark",

        historyTitle = "History",
        historyClear = "Clear Logs",
        historyEmpty = "No Records",
        historyEmptyDesc = "Every run of SRK / DFRoot / DFReroot is logged here",
        historySuccess = "Succeeded",
        historyFailed = "Failed",
        historyAborted = "Interrupted",
        historyLongPressHint = "Long-press a record to export or share",
        historyExport = "Export",
        historyShare = "Share",
        historyExportSubject = "SamsungRootKit run logs",
        historyExportDone = "Exported to the selected location",
        historyExportFailed = "Export failed",
        historySourceName = { src ->
            when (src) {
                RunSource.SRK -> "SRK"
                RunSource.DF -> "DFRoot"
                RunSource.DFE -> "DFReroot"
            }
        },

        settingsTitle = "Settings",
        settingsAppearance = "Appearance",
        settingsDynamicColor = "Use system dynamic colors",
        settingsDynamicColorDesc = "Auto-generate colors from wallpaper",
        settingsGeneral = "General",
        settingsLanguage = "Language",
        settingsLanguageDesc = "Interface language",
        settingsClearLog = "Clear Logs",
        settingsClearLogDesc = "Clear all run records",
        settingsReDisclaimer = "Re-read Disclaimer",
        settingsReDisclaimerDesc = "View full risk terms",
        settingsAbout = "About",
        settingsAboutDesc = "Version / Exploit / Supported Versions / Credits",
        settingsAdvanced = "Advanced",
        settingsDfRootGroup = "DFRoot",
        settingsDfRerootGroup = "DFReroot",
        settingsOther = "Others",

        langFollowSystem = "Follow System",
        langChinese = "\u4e2d\u6587",
        langEnglish = "English",

        themeFollowSystem = "Follow System",
        themeLight = "Light",
        themeDark = "Dark",

        aboutTitle = "About Samsung Root",
        aboutVersion = { "Version: $it" },
        aboutDescription = "Kernel-level temporary ROOT via CVE-2026-43499 and CVE-2026-43284.",
        aboutSupportedModels = "Payload-Supported Models",
        aboutKsuDesc = "Kernel-level ROOT official site",
        aboutRmgDesc = "UI design reference & credits",
        aboutSpecialThanks = "Special Thanks",
        aboutSpecialThanks1 = "Most payload files used for ROOT were collected from the internet; we thank the original authors for their tremendous contributions to this project.",
        aboutSpecialThanks2 = "Special thanks to \"yettocom\" (Telegram) for adapting the KernelSU upgrade build and the KernelSU-Next build.",
        aboutSpecialThanks3 = "Thanks to \"Www\" (Telegram) for building the initial version of this app.",
        aboutSpecialThanks4 = "Thanks to all community members who participated in device adaptation and testing.",
        aboutNoResale = "Resale or commercial use of this software in any form is prohibited.",
        aboutAuthor = "Author: 大尾巴狼，Www",
        aboutDismiss = "Got it",

        commonBack = "Back",
        commonCancel = "Cancel",
        commonConfirm = "Confirm",
        execCancelButton = "Cancel Execution",
        execCancelDialogTitle = "Cancel Root Process?",
        execCancelDialogBody = "Force-cancelling mid-execution may leave the device in an inconsistent state, and progress won't be saved. It's recommended to let the process finish naturally.",
        execCancelDialogContinue = "Continue",
        execCancelDialogAbort = "Cancel Anyway",
        disclaimerBack = "Back",
        modelCancelButton = "Cancel",
        firmwareMismatchTitle = "Firmware Mismatch",
        firmwareMismatchBody = { expected, actual -> "Current firmware ($actual) does not match the selected model ($expected). Forcing root will most likely fail or trigger a kernel error. Continue anyway?" },
        firmwareMismatchForce = "Force Continue",
        firmwareMismatchCancel = "Cancel and Go Back",
    )

    fun forLanguage(lang: String): AppStrings = when (lang) {
        "en" -> en
        else -> zh
    }
}

/** CompositionLocal for accessing UI strings from any Composable. */
val LocalStrings = staticCompositionLocalOf { AppStringResources.zh }

package com.samsung.rootkit.ui

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import com.samsung.rootkit.ui.glass.GlassCard
import com.samsung.rootkit.ui.glass.GlassBottomBarContainer
import com.samsung.rootkit.ui.glass.GlassRadii
import com.samsung.rootkit.ui.glass.GlassLight
import com.samsung.rootkit.ui.glass.TitaniumDark
import com.samsung.rootkit.ui.glass.GlassSkyProvider
import com.samsung.rootkit.ui.glass.GlassSurface
import com.samsung.rootkit.ui.glass.LocalBackdrop
import com.samsung.rootkit.ui.glass.LocalGlassEnabled
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.clickable
// 运行记录卡片要「单击展开 + 长按弹操作」两个手势，故用组合点击而非普通 clickable。
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.material3.Switch
import androidx.compose.material3.RadioButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.drawBehind
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.rootkit.RootKitApp
import com.samsung.rootkit.data.PayloadEntry
import com.samsung.rootkit.data.PayloadManager
import com.samsung.rootkit.exploit.DeviceInfo
import com.samsung.rootkit.exploit.RootExecutor
// DFReroot 持久化集成（v5.1.0）：状态检测（DfBridge）+ 执行层（DfRunner）
// ⚠️ 简称纪律：`df.*` = **dfe**（DFReroot / com.polygraphene.df.reroot）。
import com.samsung.rootkit.df.DfBridge
import com.samsung.rootkit.df.DfRunner
// df 集成（2026-10-04 P3）：`dfroot.*` = **df**（DFRoot / df.root）。
// 两者是两个不同的 App，类名前缀也不同（Df* vs DfRoot*），请勿混用。
import com.samsung.rootkit.dfroot.DfRootManager
import com.samsung.rootkit.dfroot.DfManagerInstaller
import com.samsung.rootkit.dfroot.DfPostRootCleaner
import com.samsung.rootkit.dfroot.DfRootPrefs
import com.samsung.rootkit.dfroot.DfRootLaunchState
import com.samsung.rootkit.dfroot.DfRootLaunchResolver
import com.samsung.rootkit.dfroot.DfRootFailureRecord
import com.samsung.rootkit.dfroot.DfRootFailureRecordStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate
import com.samsung.rootkit.data.RunLogStore
import com.samsung.rootkit.data.RunAttempt
import com.samsung.rootkit.data.RunOutcome
import com.samsung.rootkit.data.RunSource
import com.samsung.rootkit.data.LogExporter
import com.samsung.rootkit.data.DeviceSupport
import com.samsung.rootkit.data.AppStringResources
import com.samsung.rootkit.data.AppStrings
import com.samsung.rootkit.data.LocalStrings
import com.samsung.rootkit.data.SettingsStore
import com.samsung.rootkit.ui.theme.AppThemeMode
import com.samsung.rootkit.ui.theme.AppGreen
import com.samsung.rootkit.BuildConfig
import com.samsung.rootkit.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.comparisons.minOf
import androidx.compose.runtime.MutableState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * 取已安装的 KernelSU / KernelSU-Next 管理器包名。
 *
 * 两者包名不同（me.weishu.kernelsu / com.rifsxd.ksunext），都要探测；
 * [preferredVariant] 非空时优先返回该变体对应的包名，用于「打开管理器」按当前变体唤起
 * （调用方才知道当前用的是哪个变体，这里不去猜）。
 *
 * 注意：能否探测到取决于 AndroidManifest 的 `<queries>` 是否声明了对应包名
 * （targetSdk 30+ 的包可见性过滤；两个包名都必须声明，否则 getLaunchIntentForPackage 恒为 null）。
 */
private fun getInstalledKernelSUPackage(
    context: Context,
    preferredVariant: PayloadManager.KsuVariant? = null,
): String? {
    val packages = buildList {
        preferredVariant?.let { add(PayloadManager.managerPackage(it)) }
        add(PayloadManager.PKG_KSU)
        add(PayloadManager.PKG_KSU_NEXT)
    }.distinct()
    for (pkg in packages) {
        try {
            if (context.packageManager.getLaunchIntentForPackage(pkg) != null) {
                return pkg
            }
        } catch (_: Exception) {}
    }
    return null
}

/** 用户通过 SAF 自选的 payload/root 文件 + 选定的 ksud，传递给 Wizard 构造 PayloadEntry */
data class CustomPayloadInfo(
    val payloadFile: File,
    val rootFile: File,
    val ksudName: String,  // 内置 ksud 文件名（PayloadManager.KSUD_*）或 "ksud-custom-user"（用户自带的 ksud 文件）
    val board: String,
    val firmware: String,
    val ksudFile: File? = null,  // 用户自带的 ksud 文件的精确路径（ksudName == "ksud-custom-user" 时非空）
)

/**
 * 自定义文件 root 流程的中间缓存：用户分步选文件时缓存已选部分,
 * 等全部选齐后一次性传给 Wizard 构造 PayloadEntry。
 * 所有字段 nullable——支持"缓存了 root 但还没选 payload"这类半成品状态。
 *
 * 三阶段：
 *   1. payload+root 都齐,ksud 待选 → 弹「缺少 ksud 文件」让用户补 ksud
 *   2. 只齐其中一个(payload 或 root) → 弹「缺少 X 文件」让用户补另一个
 *   3. 全部齐 → onCustomSelected 执行
 */
private data class PendingCustomPayload(
    val payloadFile: File?,
    val rootFile: File?,
    val board: String?,
    val firmware: String?,
)

@Composable
fun RootKitScreen(
    themeModeState: MutableState<AppThemeMode>,
    dynamicColorState: MutableState<Boolean>,
) {
    val context = LocalContext.current
    val app = context.applicationContext as RootKitApp
    val activity = context as? ComponentActivity
    val scope = rememberCoroutineScope()

    // 用 rememberSaveable 让切语言（Activity 重建）后能恢复页面状态，
    // 避免从设置页切语言后跳回主页面（默认 screen=Main + mainTab=Home）。
    var screen by rememberSaveable {
        mutableStateOf(
            if (SettingsStore.hasAcceptedCurrentDisclaimer(context)) Screen.Main
            else Screen.Disclaimer
        )
    }
    var mainTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var wizardPage by remember { mutableStateOf<Page>(Page.ShizukuCheck) }

    var deviceInfo by remember { mutableStateOf<DeviceInfo?>(null) }
    var payloads by remember { mutableStateOf<List<PayloadEntry>>(emptyList()) }
    var selectedPayload by remember { mutableStateOf<PayloadEntry?>(null) }
    var logs by remember { mutableStateOf(listOf<LogEntry>()) }
    var currentStage by remember { mutableStateOf(RootExecutor.Stage.DEVICE_CHECK) }
    var stageMessage by remember { mutableStateOf("") }
    var rootSuccess by remember { mutableStateOf(false) }

    // 本次流程使用的 KSU 变体（首页两个按钮决定：红色=KernelSU / 蓝色=KernelSU-Next）。
    var execVariant by remember { mutableStateOf(PayloadManager.KsuVariant.KSU) }

    // ── Back-navigation state ──
    // disclaimerFromSettings=true 时，Disclaimer 顶部显示「返回」按钮，回退到设置页；
    // 初始进入的 Disclaimer 是冷启动必经流程，顶部显示「退出」按钮，不允许直接返回。
    var disclaimerFromSettings by remember { mutableStateOf(false) }
    // 执行中取消确认对话框
    var showExecCancelDialog by remember { mutableStateOf(false) }
    // ── 固件不匹配确认对话框 ──
    // 与 run_root.sh 的 "force continue? (type YES)" 对齐：
    // 脚本在固件不匹配时交互式询问用户，不输入 YES 则退出；
    // App 之前静默继续，是"脚本成功但 App 失败"的原因之一——
    // 用户可能在不知情的情况下对不匹配固件强行 root，导致失败。
    var showFirmwareMismatchDialog by remember { mutableStateOf(false) }

    // （原 `fromDisclaimer` 入站标记已于 2026-10-02 删除：那次改动取消了"确认免责声明后
    //   强制进 Shizuku 授权流程"，冷启动直接进主页，该标记再无置 true 的路径。Shizuku 授权
    //   改由下面的 `startFromMainButton` 单一路径按需承载。）

    // 主页"开始 Root 引导"按钮的入站标记：Shizuku 未就绪跳到 ShizukuCheckPage 时置 true，
    // 授权完成（onReady）后让 wizard 继续进入 ModelSelect，而不是回主页。
    // onBack 必须重置 false，否则下次点"开始 Root 引导"会复用这个标记。
    var startFromMainButton by remember { mutableStateOf(false) }

    // ── Language state（上移到此：本 composable 的自动安装 LaunchedEffect(~L405) 引用 strings.dfInstallOk，
    //     必须保证 strings 在其之前声明，故把语言状态与 strings 提前到 DF 状态群之前）──
    var currentLanguage by remember { mutableStateOf(SettingsStore.getLanguage(context)) }
    val strings = AppStringResources.forLanguage(resolveEffectiveLanguage(currentLanguage))

    // ── DFReroot 持久化集成（v5.1.0）──
    // dfInstalled = APK 已装；dfActive = 已生效（以 uid 1000 出现）。
    // 两者是**不同**的事：装了但没注入 / 注入后没软重启时，它只是个 uid 10xxx 的普通 App，
    // 完全不可用 —— 所以首页入口的可用性只看 dfActive。
    //
    // 2026-10-02 F8 修复：机型支持度（DeviceSupport.dfSupported()，S24/android14-6.1 为 false）
    // 原先只在主页两个按钮上做门控，设置页 DF 卡片未考虑 —— 于是「不受支持机型 + 跨版本
    // 遗留 dfActive=true」时会出现分叉（主页回退 Root 向导，设置页仍能打开 DFReroot）。
    // 现提升为**全部 DF 入口共用**的单一判据：主页按钮 / 设置页卡片 / 设置页配置弹窗。
    // 机型与内核在进程生命周期内不变，用 remember 固定一次，避免每次重组重跑内核串解析。
    val dfSupportedOnDevice = remember { DeviceSupport.dfSupported() }
    var dfInstalled by remember { mutableStateOf(false) }
    var dfActive by remember { mutableStateOf(false) }
    /**
     * dfe（DFReroot）**总开关**（2026-10-05 新增）。
     *
     * <p>与 df 的 [dfRootEnabled] 对称：关闭 ⇒ dfe 路径整体停用 ——
     * 主页两个按钮不再走 dfe 分支（回退 Root 向导）、设置页「配置 DFReroot」卡不渲染、
     * SRK root 成功后不再弹持久化引导（后者是本开关最直接的可见效果）。
     * 语义上等价于用户说「我不需要持久化 root」。
     *
     * <p>初值取持久化值（默认 true）；与 [dfRootEnabled] 的「先 true 再刷新」策略不同 ——
     * 这里是**同步 CE prefs 读取**（零 IO 等待），不存在首帧闪烁问题。
     */
    var dfEntryEnabled by remember { mutableStateOf(SettingsStore.isDfEntryEnabled(context)) }
    /**
     * dfe（DFReroot）自己的「开机自启动」开关状态（2026-10-05 新增）。
     *
     * <p>三态：**null = 还没读到 / 读不到**（无 su 授权、文件不存在、命令超时）⇒
     * 设置页那张卡整体禁用、不允许拨动（状态都没读到就往回写，写进去的值不可信）。
     *
     * <p>⚠️ 这是**对端 App 的状态**，不是本应用的 prefs：唯一事实源是
     * `/data/system/df-boot-prefs` 的 `autostart` 键（DFReroot 私有区，目录属
     * `android.uid.system`），SRK 只能经已授权的 su 读改写。这一点与 df 的
     * [dfRootAutostart] 有本质区别 —— 后者是本应用自己的 DE prefs + 自己的
     * `DfBootReceiver` 组件态，全程零权限。
     */
    var dfEntryAutostart: Boolean? by remember { mutableStateOf(null) }
    /**
     * dfe（DFReroot）「D2 修复」开关状态（2026-10-07 新增，用户需求：与 DFReroot 里的
     * D2 开关联动）。
     *
     * <p>三态与 [dfEntryAutostart] **完全同构**：**null = 还没读到 / 读不到**（无 su 授权、
     * 文件不存在、命令超时）⇒ 设置页那张卡禁用、不允许拨动。
     *
     * <p>唯一事实源同样是 `/data/system/df-boot-prefs` 的 `d2fix` 键 —— 与 `autostart`
     * **写在同一个文件、同一次写入**（DFReroot `BootGuard.setPref` 的读-改-写会带上另一个键，
     * 所以两边互不影响）。D2 是「每次开机把 DMC Vault 的 at_command 置 1、解除 Odin 下载模式
     * 的 D2 错误锁」，与 root 链路无依赖关系，默认关。
     */
    var dfEntryD2Fix: Boolean? by remember { mutableStateOf(null) }
    /**
     * dfe（DFReroot）「自动软重启」开关状态（2026-10-08 新增，用户需求：与 DFReroot
     * 里的自动软重启开关联动）。
     *
     * <p>语义 = 跑完提权链（late-load 成功）后**自动软重启一次**，让 LSPosed 生效。
     * 三态与 [dfEntryAutostart] / [dfEntryD2Fix] **完全同构**：**null = 还没读到 / 读不到**
     * ⇒ 描述改为「读取失败」（**未知绝不能显示成「关」**，否则用户会以为不会自动重启，
     * 而 DFReroot 那边其实还开着）。
     *
     * <p>唯一事实源同样是 `/data/system/df-boot-prefs` 的 `softreboot` 键 —— 与
     * `autostart` / `d2fix` **写在同一个文件、同一次写入**（DFReroot `BootGuard.setPref`
     * 的读-改-写会带上另外几个键，所以两边互不影响）。**默认关**（2026-10-08 用户定调），
     * 与 `autostart` / `d2fix` 一致。
     */
    var dfEntryAutoSoftReboot: Boolean? by remember { mutableStateOf(null) }
    // 三个弹窗 + 忙碌标记 + 分步输出（失败时把 su 原始输出摊给用户，便于定位）
    var showDfEnableDialog by remember { mutableStateOf(false) }       // 唯一配置入口（root 成功后触发）
    var showDfSoftRebootDialog by remember { mutableStateOf(false) }   // 注入成功后
    var showDfInstallDialog by remember { mutableStateOf(false) }      // 软重启后重进 App
    var showDfUninstallDialog by remember { mutableStateOf(false) }    // 设置页「卸载 DFReroot」
    // 2026-10-04 修复：卸载**完成后**弹窗转为「取消 / 软重启」两个对称按钮，不再显示「卸载」。
    // 该标记只在弹窗生命周期内有效（关弹窗时复位），不是持久状态。
    var dfUninstallCompleted by remember { mutableStateOf(false) }
    // 机型不受支持（S24 系列）时点任一 DF 入口弹出的「暂不适配」纯提示弹窗。
    // 2026-10-02 F8 修复：09-26 版曾有同名弹窗（仅挂主页入口），随主页入口迁往设置页后被删除；
    // 现恢复并改为**全入口共用**（主页按钮 / 设置页卡片 / 设置页配置弹窗）。文案沿用既有
    // dfUnsupportedTitle/Body/Confirm 三键（此前已成为未被引用的死键，本次重新接回）。
    var showDfUnsupportedDialog by remember { mutableStateOf(false) }
    // 密钥是否已注入 packages.xml（null=未知：没 root 或 --check 失败）。
    // 设置页状态行用它区分「未配置 / 已注入\未配置 / 已配置」三态（修复⑤）。
    var dfKeyInjected by remember { mutableStateOf<Boolean?>(null) }
    var dfBusy by remember { mutableStateOf(false) }
    var dfOutput by remember { mutableStateOf("") }
    // 软重启后重进 App 时，"安装 DFReroot"弹窗本次会话只自动弹一次（用户关掉后不再打扰）
    var dfInstallPrompted by remember { mutableStateOf(false) }
    // 「su 不可用 / 未授权」→ 引导去管理器授予 root（真机高频路径）
    var showDfSuDialog by remember { mutableStateOf(false) }
    // 当前生效的 KSU 变体（供 su 引导弹窗跳到**对应那一支**管理器）
    var dfMgrVariant by remember { mutableStateOf<PayloadManager.KsuVariant?>(null) }

    // ── df（DFRoot / `df.root`）集成（2026-10-04 P3）────────────────────────
    // ⚠️ 简称纪律：**上面那一组 df* 全是 dfe（DFReroot）**；本组 `dfRoot*` 才是
    //    df（DFRoot）。两者是两个不同的 App、两套不同的执行层（DfRunner vs DfRootManager）。
    // 机型支持度也**分开判**：上面 `dfSupportedOnDevice`（dfe，看 ksud 资产）≠
    // `dfRootSupportedOnDevice`（df，看 DirtyFrag 漏洞利用面）。两者当前实现碰巧相同
    // （= !isS24()，拍板④沿用），但归属不同、未来解锁节奏也不同（见 DeviceSupport 注释）。
    val dfRootSupportedOnDevice = remember { DeviceSupport.dfRootSupported() }
    /** S8 启动态（机型支持度 × `/dev/df` × 内核自报版本码），见 DfRootLaunchState 注释。 */
    var dfRootLaunchState by remember { mutableStateOf(DfRootLaunchState.IDLE) }
    /**
     * DFRoot **总开关**（round24 新增）。
     *
     * <p>单包整合后 df 成为本应用的内嵌能力、不再是「装没装」的问题，
     * 于是原来由「已安装 df APK」承担的「主页按钮是否走 df 路」这个门控，
     * 改由这个**用户可见的开关**承担 —— 用户想用回退向导（dfe / Shizuku）时把它关掉即可。
     * 关闭会同时停用开机自启（见 [DfRootPrefs.syncBootReceiverComponent]）。
     *
     * <p>初值 true 而非 false：静态支持 DFRoot 的机型上默认就该走 df（用户定调「df 优先」），
     * 否则首次渲染会先显示「未配置」再被刷新纠正，出现一次可见的闪烁。
     */
    var dfRootEnabled by remember { mutableStateOf(true) }
    /** 本次开机已注入（`/dev/df` 存在）。 */
    var dfRootArmed by remember { mutableStateOf(false) }
    var dfRootAutostart by remember { mutableStateOf(false) }
    var dfRootAutoSoftReboot by remember { mutableStateOf(true) }
    var dfRootBusy by remember { mutableStateOf(false) }
    var dfRootOutput by remember { mutableStateOf("") }

    // ── dfe（DFReroot）**内联**跑链状态（2026-10-07 新增）────────────────────────
    // 需求：主页红/蓝按钮不再跳转 DFReroot 界面，而是经其 DfRunService 内联跑链，
    // 日志经广播逐行回流到本进程，显示在**红蓝按钮正下方那张卡**里（与 df 链同构、复用
    // 同一渲染组件 [DfRootLiveLogCard]）。
    //   · [dfeBusy]   —— 是否正在跑（判据 = 卡片让位显示日志）；
    //   · [dfeOutput] —— 追加式日志文本。
    // 启动路径见 [startDfeInline]，通道常量见 DfBridge 的"DfRunService 内联通道"段。
    var dfeBusy by remember { mutableStateOf(false) }
    var dfeOutput by remember { mutableStateOf("") }
    // ── 内联跑链的「运行记录」句柄 + 待处理结局（2026-10-07 真机修复 问题①）────────
    // 起因：内联路径原先只把日志灌进 [dfeOutput]（广播），**既没有成功提示、也不写运行记录**
    //   —— 而 df 链两样都有（见 runDfRootChain：RunLogStore.begin/finish + 结局 Toast）。
    //
    // [dfeAttempt]：与 df 链把 attempt 放协程局部变量不同，内联的"起"（[startDfeInline]，
    //   点击回调）与"止"（广播接收器 onReceive）**分处两地** ⇒ 必须提升为 composition 状态，
    //   两边才拿得到同一个 RunAttempt。
    // [dfeDoneRc]：DONE 帧到达时只把 rc 记在这里，真正的收尾（写运行记录 + 弹 Toast）交给
    //   下方 LaunchedEffect —— 因为它要用 `strings`，而广播接收器注册在 `DisposableEffect(Unit)`
    //   里、闭包捕获的是**首次 composition** 的 strings，不随语言切换更新（既有代码在
    //   DONE 分支里硬编码中文就是这个原因）。
    var dfeAttempt by remember { mutableStateOf<RunAttempt?>(null) }
    var dfeDoneRc by remember { mutableStateOf<Int?>(null) }

    // ── dfe 跑链结束后触发主页刷新的「计数键」（2026-10-08 修复 问题②）──────────────
    // 用户报障：dfe 的 Next 路 root 成功后，SRK 主页状态卡/红蓝按钮仍显示旧态，
    //   必须**手动切换下方 tab** 才会刷新。根因（已核代码）：跑链结束后**不触发任何
    //   主页状态重算** —— 主页状态的刷新时机只有 [LaunchedEffect(screen, mainTab,
    //   wizardPage)]（冷启动/切 tab/向导返回）与 ON_RESUME 那个 DisposableEffect；
    //   而 DONE 帧到达时两者都不会发生，"切一下 tab" 恰好改变 mainTab 才把前者拉动。
    //
    // 为什么用「计数键」而不是在 DONE 分支里直接调用 refreshDfState()：
    //   ① Kotlin 局部函数**不可前向引用** —— 本接收器（下方 DisposableEffect）声明在
    //      refreshDfState() 之前，直接调用编译不过；
    //   ② 自增本键作为下方 [LaunchedEffect] 的第 4 个 key，可原样复用那条**顺序已经正确**
    //      的刷新序列（refreshDfState → refreshDfRootState → 三个 entry），不复制一份。
    //
    // 依据：DfRunService.handleRun 的 finally 里**无条件** emit(TYPE_DONE, rc)（连"本次开机
    //   已挂钩"的提前返回也走 finally，rc=0）⇒ 只要链跑完，这条帧必达。
    var dfeRunFinishedTick by remember { mutableStateOf(0) }

    // ── dfe 内联日志通道：动态广播接收器（2026-10-07 新增）──────────────────────
    // DFReroot 的 DfRunService 跑链时把每一行日志经广播回吐（action = [DfBridge.RUN_LOG_ACTION]，
    // 用 setPackage 限本包，并以 [DfBridge.SERVICE_PERMISSION] 作为**发送方权限**）。
    // 本处注册时也声明同一个 broadcastPermission ⇒ 只有持有它的进程（= 同签名的 DFReroot）
    // 能把这条广播送达我们；第三方即使知道 action 也发不进来（"done" 帧不可被伪造）。
    //
    // RECEIVER_EXPORTED 是**必需**的：发送方 DFReroot 跑在 system_server（uid 1000），
    // 属另一 UID；若用 RECEIVER_NOT_EXPORTED，跨 UID 广播会被系统直接丢弃。
    // API 33 起 registerReceiver 强制要求显式 flag；33 以下走带 permission 的旧重载。
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                val it = i ?: return
                if (it.action != DfBridge.RUN_LOG_ACTION) return
                when (it.getStringExtra(DfBridge.LOG_EXTRA_TYPE)) {
                    DfBridge.LOG_TYPE_LOG -> {
                        it.getStringExtra(DfBridge.LOG_EXTRA_LINE)?.let { line -> dfeOutput += line }
                    }
                    DfBridge.LOG_TYPE_DONE -> {
                        // 结束帧：无论成功/失败都复位忙碌态（DfRunService 的 finally 里必发）。
                        // 收尾（结局行 / Toast / 运行记录）**不在这里做** —— 见 [dfeDoneRc] 的注释：
                        // 本闭包注册于 DisposableEffect(Unit)，捕获的是首次 composition 的 strings，
                        // 语言切换后不会更新。这里只落 rc，交给下方 LaunchedEffect 消费。
                        if (dfeBusy) {
                            dfeBusy = false
                            dfeDoneRc = it.getIntExtra(DfBridge.LOG_EXTRA_RESULT, Int.MIN_VALUE)
                        }
                        // ★ 2026-10-08 修复 问题②：无论本次是否由本端发起（内联路径 dfeBusy=true；
                        //   跳到 DFReroot 界面跑链、或自启跑链后回到前台时 dfeBusy=false），只要链结束
                        //   就自增本键 ⇒ 下方 [LaunchedEffect] 立刻重算主页状态，无需用户切 tab。
                        dfeRunFinishedTick++
                    }
                    DfBridge.LOG_TYPE_STATE -> {
                        // 状态快照：先只落 logcat 留痕。主页按钮小字的刷新仍走既有 su 读取路径，
                        // 避免两条状态源打架；保留本分支以便后续把刷新整体迁到这条通道上。
                        Log.i(
                            "RootKitScreen",
                            "dfe state: hooked=${it.getBooleanExtra("hooked", false)}" +
                                " loaded=${it.getStringExtra("loaded_variant")}" +
                                " preferred=${it.getStringExtra("preferred_variant")}" +
                                " autostart=${it.getBooleanExtra("autostart", false)}" +
                                " inflight=${it.getBooleanExtra("inflight", false)}",
                        )
                    }
                }
            }
        }
        val filter = IntentFilter(DfBridge.RUN_LOG_ACTION)
        runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(
                    receiver, filter, DfBridge.SERVICE_PERMISSION, null,
                    Context.RECEIVER_EXPORTED,
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                val ok = context.registerReceiver(receiver, filter, DfBridge.SERVICE_PERMISSION, null)
                if (ok == null) Log.w("RootKitScreen", "dfe log receiver returned null")
            }
        }.onFailure { Log.w("RootKitScreen", "registerReceiver(dfe log) failed", it) }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    /**
     * dfe 内联跑链的**收尾**（2026-10-07 真机修复 问题①：跑完没有提示、运行记录里没有日志）。
     *
     * <p>DONE 帧只负责把 rc 落到 [dfeDoneRc]；真正的「结局行 + 写运行记录 + Toast」在这里做。
     * 为什么分开：本 effect 的 lambda 每次重组都会重新读 `strings`（语言切换即时生效），
     * 而广播接收器的闭包注册在 `DisposableEffect(Unit)`、只在首次 composition 执行一次。
     *
     * <p>三步顺序刻意与 df 链（[runDfRootChain] 尾部）对齐：
     *   ① 结局行追加进 [dfeOutput]（此刻卡片已恢复成设备信息，日志不再可见）；
     *   ② [RunLogStore.finish] 落最终态（此前只有 [startDfeInline] 里 begin 的"进行中"暂存）；
     *   ③ Toast 说一句结论 —— 否则用户只看到日志"刷地一下没了"，无从判断结果。
     */
    LaunchedEffect(dfeDoneRc) {
        val rc = dfeDoneRc ?: return@LaunchedEffect
        dfeDoneRc = null
        val ok = rc == 0
        val finalMsg = if (ok) strings.dfeRunOk
        else strings.dfeRunFail + "（结果码=" + rc + "）"
        dfeOutput += "\n" + finalMsg + "\n"
        val attempt = dfeAttempt
        dfeAttempt = null
        if (attempt != null) {
            // finish 是 IO（追加最终记录 + 清暂存）—— 与 df 链一样不要压主线程。
            withContext(Dispatchers.IO) {
                runCatching {
                    RunLogStore.finish(
                        context,
                        attempt,
                        if (ok) RunOutcome.SUCCESS else RunOutcome.FAILED,
                        if (ok) "" else "结果码=$rc",
                        dfeOutput.split("\n"),
                    )
                }
            }
        }
        android.widget.Toast.makeText(
            context, finalMsg, android.widget.Toast.LENGTH_LONG,
        ).show()
    }

    /**
     * df 跑链日志**不再用弹窗**（2026-10-05 用户定调）：跑链期间日志直接显示在
     * 主页红蓝按钮下方那张卡片内（原「设备信息卡」的位置），跑完自动恢复设备信息。
     * 故这里不再有「弹窗是否显示」的状态 —— 判据就是 [dfRootBusy]。
     * 原 `showDfRootRunDialog` 随弹窗一并删除。
     */
    /**
     * df 失败记录（SRK 私有区，**只记失败**）。
     * [DfRootFailureRecordStore.read] 返回非 null ⇒ 当前固件上 df **已连续失败到阈值**
     * （[DfRootFailureRecord.FAIL_THRESHOLD] = 3 次，2026-10-08 起）：启动判定走回退、
     * 设置页总开关打不开。null = 没失败过 / 固件已变（系统升级即解锁）/
     * **失败次数还没到阈值**（此时可从 [dfRootFailureStrikes] 看已经失败几次）。
     */
    var dfRootFailureRecord by remember { mutableStateOf<DfRootFailureRecord?>(null) }
    /**
     * 当前固件上的**连续失败次数**（0 = 没失败过 / 固件已变）。
     *
     * <p>与 [dfRootFailureRecord] 的区别：本值**不套阈值**，未达 3 次也如实为 1 / 2。
     * 唯一的消费点是跑链失败后的弹窗 —— 未达阈值时要告诉用户「第 N 次，
     * 满 3 次才关总开关」，而不是像达阈值那样说「已自动关闭」。
     */
    var dfRootFailureStrikes by remember { mutableStateOf(0) }
    /** S7 系统区「支持」记录（df 链内代写；写入方未落地前恒 false）。 */
    var dfRootSystemZoneSupported by remember { mutableStateOf(false) }

    // ── 变体切换确认（2026-10-01 晚；round19 起同时服务 dfe / df 两路入口）────
    // 主页点 KSU/KSU-Next 按钮时，若对应 App 的「开机自启动」已打开、且目标变体 ≠
    // 当前正在工作的管理器，先弹窗确认「下次开机自动 root 切换到 XX」，确认后才继续。
    // pendingSwitchIsDfRoot 区分弹窗来源（确认后动作不同）：
    //   false = dfe（DFReroot）→ openDfWithVariant（写持久化变体 + 跳 dfe）
    //   true  = df（DFRoot）  → setDfRootVariant（记变体）+ runDfRootChain（继续跑链）
    var showVariantSwitchDialog by remember { mutableStateOf(false) }
    var pendingSwitchVariant by remember { mutableStateOf(PayloadManager.KsuVariant.KSU) }
    var pendingSwitchIsDfRoot by remember { mutableStateOf(false) }
    /**
     * 打开切换弹窗那一刻的「系统已 root」快照（2026-10-07 需求①）。
     *
     * <p>用途：dfe 分支确认后走 [openDfWithVariant]，而"要不要写运行记录"取决于点击时
     * 系统是否已 root。该事实由主页判出（[HomePage] 的 `systemRooted`），弹窗确认时已不在
     * 主页作用域 ⇒ 必须在打开弹窗时把它存下来，与 [pendingSwitchVariant] 同生命周期
     * （两者都只服务于"确认"这一次动作）。
     */
    var pendingSwitchSystemRooted by remember { mutableStateOf(false) }

    // ── df「已 root，无需重复操作」提示（2026-10-05 需求④）──
    // 主页 df 模式下系统已 root 时点红/蓝按钮：既不跑链、也不弹切换窗（切换窗只在
    // 「自启开 && 目标变体 ≠ 当前工作管理器」时出现）⇒ 用本弹窗如实告知用户。
    var showDfRootAlreadyRootedDialog by remember { mutableStateOf(false) }

    // ── df 失败版本门控（2026-10-05 需求②，两个弹窗 + 一个待跳变体）──
    /** 设置页手动打开总开关被「当前固件已失败记录」拦下时的提示。 */
    var showDfRootUnsupportedDialog by remember { mutableStateOf(false) }
    /**
     * 跑链失败后的引导弹窗（已自动关总开关）——确认 ⇒ 进入 SRK 的 Root 流程（向导）。
     * [dfRootFallbackVariant] 记住用户点的是哪一支按钮，进向导时用它（向导首屏会
     * 按变体选对应管理器/ksud）。
     */
    var showDfRootFailedFallbackDialog by remember { mutableStateOf(false) }
    var dfRootFallbackVariant by remember { mutableStateOf(PayloadManager.KsuVariant.KSU) }

    // DF 状态刷新（IO 线程）。包可见性已在 AndroidManifest 的 <queries> 声明，
    // 否则 getApplicationInfo 会抛异常、两项恒为 false。
    suspend fun refreshDfState() {
        withContext(Dispatchers.IO) {
            dfInstalled = DfBridge.isDfInstalled(context)
            dfActive = DfBridge.isDfActive(context)
            // 顺带取一次"当前生效的 KSU 变体"，供 su 引导弹窗跳对应那一支管理器。
            // 与首页状态卡同源（resolveWorkingVariant：内核自报版本码 ksud 探测 → df-boot-ok
            // → df-boot.log 加载记录 → 已安装唯一性；见 RootExecutor 注释）。
            dfMgrVariant = try { app.rootExecutor.resolveWorkingVariant() } catch (_: Exception) { null }
            // 密钥注入状态（设置页三态显示用，修复⑤）：--check 零写入，但要 root。
            // 拿不到 root / 执行失败 → 保持 null（未知），状态行回退为只看安装/生效。
            dfKeyInjected = try {
                val su = app.rootExecutor.resolveWorkingSu()
                if (su == null) null else {
                    val c = DfRunner.checkInjected(context, su)
                    if (c.ok) c.output.contains("all_injected=true") else null
                }
            } catch (_: Exception) {
                null
            }
            // dfe 总开关：纯 CE prefs 读取（零 IO 等待），随每次状态刷新一并回读。
            // 正常路径下 setter 已即时同步内存态，这里是防御性纠偏（多进程/外部修改）。
            dfEntryEnabled = SettingsStore.isDfEntryEnabled(context)
        }
    }

    /**
     * 打开 / 关闭 DFReroot **总开关**（2026-10-05）。
     *
     * <p>与 df 的 [setDfRootEnabled] 对称，但**没有异步与失败分支**：df 那条要写 DE prefs
     * 并派生自己的 `DfBootReceiver` 组件态（故走 [DfRootManager] + 协程 + Toast 兜底），
     * 而本开关只是一次 CE prefs 布尔写入 —— 不涉及 su、不涉及组件态、不涉及跨 App，
     * 同步写完即生效。仍保留一次回读，与项目「乐观更新 + 回读纠偏」的既有纪律一致。
     */
    fun setDfEntryEnabled(enable: Boolean) {
        // ★ 2026-10-07 需求④：**dfe 持久化已配置好时不允许关闭总开关**。
        //   UI 侧（SettingsPage 的总开关卡）已改为「点关时弹窗提醒、且不回调本函数」；
        //   这里是**逻辑兜底** —— 挡下任何绕过 UI 的调用点（含日后新增入口）。
        //   判据与 SettingsPage 弹窗同构（dfEntryReady = dfActive && 机型支持），
        //   且只拦「关」这一个方向，避免「已配置但开关是关的」历史状态连打开都做不到。
        if (!enable && dfActive && dfSupportedOnDevice) {
            Log.i("RootKitScreen", "dfe 总开关已锁定（dfe 持久化已配置），忽略关闭请求")
            return
        }
        dfEntryEnabled = enable
        SettingsStore.setDfEntryEnabled(context, enable)
        dfEntryEnabled = SettingsStore.isDfEntryEnabled(context)
    }

    /**
     * 刷新 dfe 的「开机自启动」状态（设置页 DFReroot 组那张开关卡，2026-10-05 新增）。
     *
     * <h2>为什么必须放在 IO 线程</h2>
     * 它是一次**经 su 的 `cat /data/system/df-boot-prefs`**
     * （[RootExecutor.isDfAutostartEnabled]，内部先 `resolveWorkingSu` 探一次 su 再用它读文件，
     * 最坏情况是两个 3s 超时串起来）—— 放主线程必然卡顿。
     *
     * <h2>为什么只在 [dfEntryReady] 时读</h2>
     * DFReroot 没装 / 没生效时，`/data/system/df-boot-prefs` 要么不存在要么无意义；
     * 而本函数会被 onResume 调用，无门控就等于每次回到前台都白起一次 su。
     * 不满足条件时把状态**显式置回 null**（而不是保留旧值）—— 卸载 DFReroot 后那张卡
     * 本就不再渲染，留着旧值只会让日后重新到达该状态时先闪一下错误的值。
     *
     * <p>⚠️ 依赖 [dfActive]，而 [dfActive] 由 [refreshDfState] 计算 ⇒ 调用顺序必须是
     * **先 refreshDfState 再 refreshDfEntryAutostart**（与 [refreshDfRootState] 同一条纪律）。
     */
    suspend fun refreshDfEntryAutostart() {
        if (!(dfActive && dfSupportedOnDevice)) {
            dfEntryAutostart = null
            return
        }
        dfEntryAutostart = withContext(Dispatchers.IO) {
            try { app.rootExecutor.isDfAutostartEnabled() } catch (_: Exception) { null }
        }
    }

    /**
     * 写 dfe 的「开机自启动」—— 与 DFReroot **自己界面上的那个开关是同一份状态**
     * （用户需求：两处联动）。
     *
     * <h2>联动是怎么成立的</h2>
     * [RootExecutor.setDfAutostart] 经 su 优先走 DFReroot 自带的状态通道
     * （`am start -n <pkg>/.MainActivity --ez autostart <bool>`，由其 `BootGuard.setPref`
     * 落盘），失败才直写 `df-boot-prefs` 兜底；两条路都**以回读 `autostart=1` 为唯一判据**。
     * 而 DFReroot 主界面的开关读的是同一份 `BootGuard.prefs()` ⇒ 写成功即等于「它界面里也变了」。
     * 反向（在 DFReroot 里拨、回到 SRK 设置页看到新值）由 onResume 的
     * [refreshDfEntryAutostart] 覆盖 —— 双向闭环。
     *
     * <p>先乐观更新让开关立即响应；失败则 Toast 提醒 + **回读纠偏**
     * （回读结果可能是 null ⇒ 卡片转为禁用态，如实反映「读不到了」）。
     */
    fun setDfEntryAutostart(enable: Boolean) {
        dfEntryAutostart = enable
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try { app.rootExecutor.setDfAutostart(enable) } catch (_: Exception) { false }
            }
            if (!ok) {
                android.widget.Toast.makeText(
                    context, strings.settingsDfEntryAutostartFailed, android.widget.Toast.LENGTH_LONG,
                ).show()
            }
            // 成败都回读一次：成功时确认真实落盘值，失败时把乐观值纠偏回来。
            refreshDfEntryAutostart()
        }
    }

    /**
     * 刷新 dfe 的「D2 修复」状态（设置页 DFReroot 组那张开关卡，2026-10-07 新增）。
     *
     * <p>与 [refreshDfEntryAutostart] **完全同构**（同一条 su 读、同一份文件、同一套门控与
     * 置 null 纪律），只是匹配的键从 `autostart` 换成 `d2fix`。分开写而不是合并成
     * 一个"读一次文件、解两个值"的函数，是为了让两条通道各自独立失败 —— 其中一个读不到
     * 不该把另一个也拖成禁用态。开销上多起一次 su（只在已 root 且 DFReroot 生效时才发生），可接受。
     *
     * <p>⚠️ 同 [refreshDfEntryAutostart]：依赖 [dfActive]，故调用顺序必须
     * **先 refreshDfState 再 refreshDfEntryD2Fix**。
     */
    suspend fun refreshDfEntryD2Fix() {
        if (!(dfActive && dfSupportedOnDevice)) {
            dfEntryD2Fix = null
            return
        }
        dfEntryD2Fix = withContext(Dispatchers.IO) {
            try { app.rootExecutor.isDfD2FixEnabled() } catch (_: Exception) { null }
        }
    }

    /**
     * 写 dfe 的「D2 修复」—— 与 DFReroot **自己界面上的 D2 开关是同一份状态**（用户需求：联动）。
     *
     * <h2>联动是怎么成立的</h2>
     * [RootExecutor.setDfD2Fix] 经 su 优先走 DFReroot 自带的状态通道
     * （`am start -n <pkg>/.MainActivity --ez d2fix <bool>`，由其 `BootGuard.setPref` 落盘），
     * 失败才直写 `df-boot-prefs` 兜底；两条路都**以回读 `d2fix=1` 为唯一判据**。
     * DFReroot 主界面的 D2 开关读的是同一份 `BootGuard.prefs()` ⇒ 写成功即等于「它界面里也变了」。
     * 反向（在 DFReroot 里拨、回到 SRK 设置页看到新值）由 onResume 的
     * [refreshDfEntryD2Fix] 覆盖 —— 双向闭环。
     *
     * <p>先乐观更新让开关立即响应；失败则 Toast 提醒 + **回读纠偏**。
     */
    fun setDfEntryD2Fix(enable: Boolean) {
        dfEntryD2Fix = enable
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try { app.rootExecutor.setDfD2Fix(enable) } catch (_: Exception) { false }
            }
            if (!ok) {
                android.widget.Toast.makeText(
                    context, strings.settingsDfEntryD2FixFailed, android.widget.Toast.LENGTH_LONG,
                ).show()
            }
            // 成败都回读一次：成功时确认真实落盘值，失败时把乐观值纠偏回来。
            refreshDfEntryD2Fix()
        }
    }

    /**
     * 刷新 dfe 的「自动软重启」状态（设置页 DFReroot 组那张开关卡，2026-10-08 新增）。
     *
     * <p>与 [refreshDfEntryAutostart] / [refreshDfEntryD2Fix] **完全同构**（同一条 su 读、
     * 同一份文件、同一套门控与置 null 纪律），只是匹配的键从 `autostart`/`d2fix` 换成
     * `softreboot`。分开写而不是合并成"读一次文件解三个值"，是为了让三条通道各自独立
     * 失败 —— 其中一个读不到不该把另两个也拖成"读取失败"态。
     *
     * <p>⚠️ 同前两者：依赖 [dfActive]，故调用顺序必须
     * **先 refreshDfState 再 refreshDfEntryAutoSoftReboot**。
     *
     * <p>门控**有意不叠加 dfe 总开关**：本函数只管"读值"，卡片显隐由渲染处按
     * [dfEntryEnabled] 决定。这样总开关一开，UI 立刻有真值可显示（不必等一次新刷新）。
     */
    suspend fun refreshDfEntryAutoSoftReboot() {
        if (!(dfActive && dfSupportedOnDevice)) {
            dfEntryAutoSoftReboot = null
            return
        }
        dfEntryAutoSoftReboot = withContext(Dispatchers.IO) {
            try { app.rootExecutor.isDfAutoSoftRebootEnabled() } catch (_: Exception) { null }
        }
    }

    /**
     * 写 dfe 的「自动软重启」—— 与 DFReroot **自己界面上的那个开关是同一份状态**（用户需求：联动）。
     *
     * <h2>联动是怎么成立的</h2>
     * [RootExecutor.setDfAutoSoftReboot] 经 su 优先走 DFReroot 自带的状态通道
     * （`am start -n <pkg>/.MainActivity --ez softreboot <bool>`，由其 `BootGuard.setPref`
     * 落盘），失败才直写 `df-boot-prefs` 兜底；两条路都**以回读 `softreboot=1` 为唯一判据**。
     * DFReroot 主界面的自动软重启开关读的是同一份 `BootGuard.prefs()` ⇒ 写成功即等于
     * 「它界面里也变了」。反向（在 DFReroot 里拨、回到 SRK 设置页看到新值）由 onResume 的
     * [refreshDfEntryAutoSoftReboot] 覆盖 —— 双向闭环。
     *
     * <p>先乐观更新让开关立即响应；失败则 Toast 提醒 + **回读纠偏**。
     *
     * <p>⚠️ 拨动**不会立刻触发重启**：它只写开关，链脚本（`stage1.S` chain_sh）在**下一次跑链**
     * 时才读它。卡片描述已如实说明是"root 成功后"生效。
     */
    fun setDfEntryAutoSoftReboot(enable: Boolean) {
        dfEntryAutoSoftReboot = enable
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try { app.rootExecutor.setDfAutoSoftReboot(enable) } catch (_: Exception) { false }
            }
            if (!ok) {
                android.widget.Toast.makeText(
                    context,
                    strings.settingsDfEntryAutoSoftRebootFailed,
                    android.widget.Toast.LENGTH_LONG,
                ).show()
            }
            // 成败都回读一次：成功时确认真实落盘值，失败时把乐观值纠偏回来。
            refreshDfEntryAutoSoftReboot()
        }
    }

    // ── df（DFRoot）状态刷新 ──
    // round24 起 df 已是**内嵌能力**：状态全部来自本应用自己的 DE 存储 + 组件态
    // （stat /dev/df、读 prefs、读组件态），零跨 App 查询、零 IPC 等待，
    // 故可以随页面 onResume 常规刷新。
    suspend fun refreshDfRootState() {
        // 系统升级 ⇒ 解锁标记。必须在 withContext 外声明：IO 块里赋值、块外弹 Toast。
        var reenabledAfterUpgrade = false
        withContext(Dispatchers.IO) {
            // ── 系统升级 ⇒ 失败记录作废 + **自动重新打开** DFRoot 总开关（2026-10-05 需求②）──
            // 用户原话：「读取到文件内记录版本号跟当前系统版本号不一致，则自动打开df功能
            // 手动到设置里打开df总开关也不拦截，直接放行」。
            // 判据用 readRaw（**不带固件校验**）——它存在的唯一意义就是让这里看出
            // 「有记录、但属于旧固件」。用 read 的话固件不符已返回 null，看不出这个事实。
            //
            // ★ 必须先清记录再写开关：否则每次 onResume 都会重新把 enabled 写回 true，
            //   用户在 OTA 后手动关掉开关，下一次回前台又被强行打开 —— 与用户对着干。
            val stale = DfRootFailureRecordStore.readRaw(context)
            if (stale != null && !stale.matchesCurrentFirmware) {
                DfRootFailureRecordStore.clear(context)
                reenabledAfterUpgrade = DfRootManager.setEnabled(context, true)
            }
            dfRootArmed = DfRootManager.isArmedThisBoot()
            val st = DfRootManager.readSwitchState(context)
            dfRootEnabled = st.enabled
            dfRootAutostart = st.autostart
            dfRootAutoSoftReboot = st.autoSoftReboot
            // 自愈：万一某次组件态同步失败（或用户在系统设置里手动改过本组件），
            // 每次刷新都按「总开关 && 自启」重新派生一次。方法内部已幂等短路，开销可忽略。
            DfRootPrefs.syncBootReceiverComponent(context)
            // 失败记录（私有区；**固件不一致 ⇒ null 视同从未失败**）+ 系统区「支持」记录。
            // 与静态机型判据互不混合（2026-10-04 用户定调）。
            // ★ 2026-10-08：read 现在**自带阈值** —— 固件一致 **且** 连续失败 ≥
            //   DfRootFailureRecord.FAIL_THRESHOLD 才非 null。故「启动判定回退」与
            //   「设置页拦下」两处无需各自再判次数，自动只在第 3 次失败后才生效。
            dfRootFailureRecord = DfRootFailureRecordStore.read(context)
            // 未达阈值的次数另取一份（只喂给失败弹窗的「连续第 N 次」文案）。
            dfRootFailureStrikes = DfRootFailureRecordStore.strikesOnCurrentFirmware(context)
            dfRootSystemZoneSupported = DfRootFailureRecordStore.readSystemZoneSupported()
        }
        if (reenabledAfterUpgrade) {
            android.widget.Toast.makeText(
                context, strings.dfRootAutoReenabled, android.widget.Toast.LENGTH_LONG,
            ).show()
        }
        // S8：重算启动态。它依赖 dfMgrVariant（内核自报版本码），而 dfMgrVariant 由
        // refreshDfState() 计算 ⇒ 调用顺序必须是「先 refreshDfState 再 refreshDfRootState」。
        // deviceSupported 用 df 自己的判据（dfRootSupportedOnDevice），不用 dfe 那份；
        // 失败记录作为第二级证据（实测压过静态预判），读序详见 DfRootLaunchResolver.resolve。
        dfRootLaunchState = DfRootLaunchResolver.resolve(
            deviceSupported = dfRootSupportedOnDevice,
            armedThisBoot = dfRootArmed,
            kernelVariant = dfMgrVariant,
            failureRecord = dfRootFailureRecord,
            systemZoneSupported = dfRootSystemZoneSupported,
        )
    }

    /**
     * 跑一次 df 链（**主页按钮与「变体切换确认」共用的唯一入口**）。
     *
     * <p>进度写进 [dfRootOutput]，由主页**红蓝按钮下方那张卡**在跑链期间实时显示
     * （2026-10-05 需求①：原先是个盖住整页的弹窗，现改为卡内动态日志，跑完自动
     * 恢复设备信息）——链可能跑分钟级，没有反馈用户会以为卡死并退出应用；
     * 而一旦中途被打断，`/dev/df` 的 O_EXCL 可能已被消费 ⇒ 本次开机再也拿不到 root。
     *
     * <p>结局（2026-10-05 需求②）：日志随卡恢复而消失，故结局改用 Toast 交代一句；
     * 若失败（[DfRootResult.chainFailedOnDevice]，此时失败记录与关总开关已由
     * [DfRootManager.runChain] 落定），再弹引导窗 → 用户确认后进入 SRK 的 Root 流程。
     *
     * <h2>2026-10-06 改向（round35 方案 B）：管理器移到「跑链之前」</h2>
     * round30 的时序是「跑链**后**补装」—— 因为链成功会**主动软重启并杀掉本进程**
     * （Next 路必需），只能靠持久化标记交给下次启动接力。但接力那两半实测都不成立：
     * <ul>
     *   <li>链内（wrapper 的 1b 段，UMH/permissive 里 `pm install`）—— **装不上**，
     *       round35 已整段删除；</li>
     *   <li>应用侧（su → Shizuku）—— 缺管理器的场景正是「**还没 root**」，
     *       `su` 结构性不可用（round31 实测），Shizuku 默认没装。</li>
     * </ul>
     * ⇒ 改用 **③ PackageInstaller 通道**（[DfManagerInstaller]）：无需 root、只需点一次
     * 系统安装确认。它依赖「应用在前台」，所以**必须**放在跑链**之前** —— 链一跑就可能
     * 把进程带走，那时再弹窗已经晚了。落实为：
     * <pre>
     *   ⓪ [startDfRootEntry] 检测 → 缺则安装（挂起等系统回执）；装不成 ⇒ 中止，不跑链；
     *   ① 本方法跑链；
     *   ② 提权成功后清 /data/local/tmp（round34，链内 + 应用侧两半都在）；
     *   ③ 收尾运行记录。
     * </pre>
     * 于是本方法里**不再有任何管理器逻辑**：闸门保证进来时管理器一定就绪。
     */
    fun runDfRootChain(variant: PayloadManager.KsuVariant) {
        if (dfRootBusy) return
        dfRootBusy = true
        dfRootOutput = ""
        val ksuNext = variant == PayloadManager.KsuVariant.KSU_NEXT
        scope.launch {
            // 运行记录 begin（DF 源）。model/firmware 走 Build.*：这条路径不经过
            // payload 选择页，没有 entry 可取（与 SRK 向导那条路的数据来源不同）。
            val attempt = RunLogStore.begin(
                context,
                RunSource.DF,
                RunLogStore.defaultModel(),
                RunLogStore.defaultFirmware(),
            )
            val res = DfRootManager.runChain(
                context,
                ksuNext = ksuNext,
                // 是否软重启沿用设置里的「自动软重启」—— 单一事实源。
                // （df 原版界面另有一个**不持久化**的「自动软重启」勾选框，只影响那一次点击；
                //   SRK 里没有与它对应的位置，硬搬进来只会多出一处随时可能不一致的状态。）
                softReboot = dfRootAutoSoftReboot,
                onProgress = { msg ->
                    dfRootOutput += msg
                    // 途中日志快照（RunLogStore 内部有 1.5s 节流，可放心每行都调）。
                    // 存在的意义：df 链成功时会**主动软重启**并杀掉本进程，
                    // 若恰好在写回执前被杀，至少留下跑到哪一步的现场。
                    RunLogStore.checkpoint(context, attempt, dfRootOutput.split("\n"))
                },
            )

            // willSoftReboot：**即将**被软重启杀掉（rc=OK 且链侧要软重启）。
            //   rc=ALREADY_ARMED 时链根本没跑 ksud，不会有软重启 ⇒ 不算在内。
            val willSoftReboot = res.ok && !res.alreadyArmed && res.softReboot

            // ── ①（round34 需求②）提权成功后的 /data/local/tmp 清理 ────────────────
            // 用户原话：「DF链路提权成功也需要清理tmp下缓存文件，如果打开自动软重启，则下次
            // 打开srk时自动清理tmp下缓存文件（自动软重启后肯定已经是root状态）」。
            //
            // 链内那一半已由包装器的「1a 段」在 UMH（uid 0 + permissive）里做完 ——
            // 零外部依赖、必然可执行（见 assets/dfroot/ksud_wrap.sh）。这里做应用侧那一半：
            //   · 进程还活着 ⇒ 立刻清一次（su → Shizuku，成败由事实核对判定）；
            //   · 马上要被软重启杀掉 ⇒ 落**持久化**标记，由下次启动接力
            //     （见 RootKitApp.recoverAndProvisionNow 的「②」段）。
            // 两半都不是可选项：链内覆盖「链刚跑完那一刻」，应用侧覆盖「链内之后新产生的文件」
            // 以及「链内那半万一没跑成」的第二次机会。
            if (res.ok) {
                if (willSoftReboot) {
                    if (DfRootPrefs.markTmpCleanArmed(context)) {
                        dfRootOutput += "系统即将软重启，/data/local/tmp 清理将在下次打开 SRK 时自动执行\n"
                    } else {
                        dfRootOutput += "待清理标记落盘失败（下次打开 SRK 时不会自动清理）\n"
                    }
                } else {
                    val cr = withContext(Dispatchers.IO) {
                        runCatching { DfPostRootCleaner.cleanNow(app.shizukuManager) }.getOrNull()
                    }
                    dfRootOutput += (cr?.lines?.joinToString("\n")
                        ?: "清理 /data/local/tmp 过程异常（详见 Logcat 标签 DFROOT）") + "\n"
                }
            }
            refreshDfRootState()
            dfRootBusy = false

            if (willSoftReboot) {
                // ★ 运行记录**有意不 finish** —— 留给重启后的收尾：
                //   此刻软重启还没发生，本流程并未真的走完（重启后的状态没人能在此刻断言）。
                //   若在这里就 finish，重启后 recoverPending 只会 no-op，
                //   待清理 /data/local/tmp 那一路的结果永远进不了日志。
                //   force = true 跳过节流：马上要被杀了，现场必须立刻落盘。
                dfRootOutput += "\n" + strings.dfRootResultOk
                RunLogStore.checkpoint(context, attempt, dfRootOutput.split("\n"), force = true)
                android.widget.Toast.makeText(
                    context, strings.dfRootResultOk, android.widget.Toast.LENGTH_LONG,
                ).show()
                return@launch
            }

            // ── ② 走到这里 = 本轮流程可以在进程内走完 ⇒ 这时才轮到日志收尾 ──
            //   round35：结局判据**不再**掺入管理器 —— 闸门（startDfRootEntry ⓪）已保证
            //   「能跑链 ⇒ 管理器一定就绪」，再算一遍只会引入永远为真的分支。
            val finalMsg = when {
                !res.ok -> strings.dfRootResultFail + "（" + res.summary + "）"
                else -> strings.dfRootResultOk
            }
            dfRootOutput += "\n" + finalMsg
            RunLogStore.finish(
                context,
                attempt,
                if (res.ok) RunOutcome.SUCCESS else RunOutcome.FAILED,
                res.summary,
                dfRootOutput.split("\n"),
            )
            // ── 结局反馈 ──
            // 卡片已恢复成设备信息，日志不再可见 ⇒ 用 Toast 把结论说一句（成功/失败都要），
            // 否则用户只看到日志「刷地一下没了」，无从判断结果。
            // 失败且是**本固件上的链失败** ⇒ 再弹引导窗（记录与关总开关已在
            // DfRootManager.runChain 内完成）；rc=-1 这类「链根本没跑」不引导，
            // 因为那是 SRK 自身环境问题，跟系统版本无关。
            android.widget.Toast.makeText(
                context, finalMsg, android.widget.Toast.LENGTH_LONG,
            ).show()
            if (res.chainFailedOnDevice) {
                dfRootFallbackVariant = variant
                showDfRootFailedFallbackDialog = true
            }
        }
    }

    /**
     * 主页红/蓝按钮的 df（DFRoot）入口 —— **点击即跑链**（round24）。
     *
     * <h2>与原「A 路线」的差别</h2>
     * 原实现是「打开 DFRoot 界面，用户在那边点双按钮跑链」—— 因为链在**另一个 App**
     * 的进程里。单包后 df 就是本应用自己，「打开界面」这一步不再有意义，
     * 于是主页按钮直接变成**跑链入口**（round24 用户定调）。
     *
     * <h2>保留的变体切换确认</h2>
     * 与 dfe 入口同构：若**自启已开**且目标变体 ≠ 当前工作管理器，先弹
     * 「下次开机自动 root 切换到 XX」确认（`/dev/df` 是 O_EXCL 互斥锁，
     * 本次开机已注入时无法重复注入，只能改自启变体让下次开机生效）；确认后记变体**并继续跑链**
     * —— 若本次开机尚未注入，这次点击本来就该跑；若已注入，链会立刻返回
     * `RC_ALREADY_ARMED`（弹窗里如实显示），两条路的用户预期都成立。
     *
     * <p>「当前工作管理器」判定统一走 resolveWorkingVariant（最高优先级 = 内核自报版本码，
     * D1 免疫；`ksu_next` 键是「下次开机的待生效选择」，拿它比对会反向——真机实锤）。
     *
     * <p>自启态**实时读**（[DfRootManager.readSwitchState]），不用页面缓存 [dfRootAutostart]
     * —— 缓存只在冷启动/切 tab 时刷新，用户在设置页刚拨过开关就回主页点击时会读到旧值，
     * 导致该弹的确认窗不弹（round22 需求②同类缺口）。
     *
     * <h2>★ round35（2026-10-06）：最前面多了一道「管理器闸门」⓪</h2>
     * 用户拍板走**方案 B**：检测 → 装 → 才跑链，且「管理器安装一律由 DF 入口按钮在跑链前的
     * 检测触发（**包括已 root 状态也能装**）」⇒ 闸门必须排在下面第 ①/② 分支**之前**，
     * 否则「已 root 但没管理器」这种残缺状态永远补不上。
     *
     * <p>装不成（用户取消 / 安装失败 / 缺「安装未知应用」授权）⇒ **中止，不跑链**
     * —— 这是用户 2026-10-06 的第二条拍板。
     *
     * <h2>2026-10-05 需求④：已 root 时不再重复跑链</h2>
     * 分支顺序**不可调换**：
     * <ol>
     *   <li>自启开 && 目标变体 ≠ 当前工作管理器 → **弹「切换下次开机管理器」确认**
     *       （用户明确要求「自启动打开时该功能要保留」；`/dev/df` 是 O_EXCL，本次已注入时
     *        只能靠改自启变体让下次开机生效）；</li>
     *   <li>否则若系统已 root（[systemRooted]，由 HomePage 判出后传入）→ 弹
     *       「当前系统已 Root，请勿重复操作」，**不进 root 流程**；</li>
     *   <li>都否 → 正常跑链。</li>
     * </ol>
     * 注意分支 1 必须排在分支 2 之前：两者可同时成立（已 root 且需要切换），
     * 此时按用户要求**优先保留切换功能**。
     *
     * @param variant       所点按钮对应的变体
     * @param systemRooted  主页已判出的「系统已 root」（hasRoot || ksuWorking）
     */
    fun startDfRootEntry(variant: PayloadManager.KsuVariant, systemRooted: Boolean) {
        if (dfRootBusy) return
        scope.launch {
            // ── ⓪ 管理器闸门（round35 方案 B）────────────────────────────────────
            // 先做一次**零成本**的 PackageManager 查询：绝大多数情况下管理器已装，
            // 直接放行，既不弹窗也不占用 dfRootBusy（不开日志卡，界面零扰动）。
            val ksuNext = variant == PayloadManager.KsuVariant.KSU_NEXT
            val needManager = withContext(Dispatchers.IO) {
                runCatching { !DfManagerInstaller.isInstalled(context, ksuNext) }.getOrDefault(false)
            }
            if (needManager) {
                // 只有真要装时才开日志卡 —— ③ 通道要挂起等用户点系统确认窗，
                // 期间必须让用户看到「在等什么」，否则界面像卡死。
                dfRootBusy = true
                dfRootOutput = ""
                val mgr = runCatching {
                    DfManagerInstaller.install(context, app.payloadManager, ksuNext)
                }.getOrNull()
                dfRootOutput += (mgr?.lines?.joinToString("\n")
                    ?: "管理器安装过程异常（详见 Logcat 标签 DFROOT）") + "\n"
                val abortMsg = when (mgr?.outcome) {
                    null, DfManagerInstaller.Outcome.FAILED -> strings.dfRootManagerFailed
                    DfManagerInstaller.Outcome.CANCELLED -> strings.dfRootManagerCancelled
                    DfManagerInstaller.Outcome.NO_PERMISSION -> strings.dfRootManagerNoPermission
                    // 已装 / 本轮装上 ⇒ 放行
                    DfManagerInstaller.Outcome.ALREADY, DfManagerInstaller.Outcome.INSTALLED -> null
                }
                dfRootBusy = false
                if (abortMsg != null) {
                    // ★ 用户拍板：取消 / 失败 ⇒ **中止本次流程，不跑链**。
                    //   如实记一条 ABORTED 运行记录 —— 这类「什么都没发生」的失败最难排查，
                    //   没有留痕用户只会觉得「点了没反应」。
                    dfRootOutput += "\n" + abortMsg + "\n"
                    val abortAttempt = RunLogStore.begin(
                        context,
                        RunSource.DF,
                        RunLogStore.defaultModel(),
                        RunLogStore.defaultFirmware(),
                    )
                    RunLogStore.finish(
                        context,
                        abortAttempt,
                        RunOutcome.ABORTED,
                        abortMsg,
                        dfRootOutput.split("\n"),
                    )
                    android.widget.Toast.makeText(
                        context, abortMsg, android.widget.Toast.LENGTH_LONG,
                    ).show()
                    if (mgr?.outcome == DfManagerInstaller.Outcome.NO_PERMISSION) {
                        // 缺「安装未知应用」授权 ⇒ 直接送进设置页那一项。
                        // 用户授权后返回 SRK（ON_RESUME），再点一次按钮就会重新走闸门。
                        DfManagerInstaller.openUnknownSourcesSettings(context)
                    }
                    return@launch
                }
                // 装好了 ⇒ 继续走原来的分支（可能跑链，也可能弹「已 root 请勿重复」）
            }

            val current = try {
                app.rootExecutor.resolveWorkingVariant()
            } catch (_: Exception) { null }
            val autostartOn = withContext(Dispatchers.IO) {
                DfRootManager.readSwitchState(context).autostart
            }
            when {
                // ① 保留的「切换下次开机管理器」（用户 2026-10-05 明确要求不砍）
                autostartOn && current != null && current != variant -> {
                    pendingSwitchVariant = variant
                    pendingSwitchIsDfRoot = true
                    showVariantSwitchDialog = true
                }
                // ② 已 root 且无需切换 ⇒ 不重复跑链，只提示
                systemRooted -> showDfRootAlreadyRootedDialog = true
                // ③ 未 root ⇒ 正常跑链
                else -> runDfRootChain(variant)
            }
        }
    }

    /**
     * 打开 / 关闭 DFRoot **总开关**（关 ⇒ 主页按钮不再走 df 路，开机自启同时停用）。
     *
     * <h2>2026-10-05 需求②：打开前先过「失败版本」门禁</h2>
     * 用户原话：「手动到设置里打开df总开关时，也读取文件，文件内版本号跟当前版本号一致
     * 且不支持的，这个df总开关打不开，弹窗显示当前系统版本不支持。因为系统可能升级，
     * 读取到文件内记录版本号跟当前系统版本号不一致，则自动打开df功能，手动到设置里打开
     * df总开关也不拦截，直接放行。」
     *
     * <p>对应实现只有一句判据：[DfRootFailureRecordStore.read] —— 它**自带固件校验，
     * 且自带失败次数阈值**：
     * <ul>
     *   <li>返回非 null ⇒ 当前固件上**已连续失败到 [DfRootFailureRecord.FAIL_THRESHOLD] 次**
     *       ⇒ **拦下**（开关保持关闭 + 弹窗）；</li>
     *   <li>返回 null ⇒ 没失败过 / 固件已变 / **失败次数还没到阈值** ⇒ 直接放行。</li>
     * </ul>
     * 于是「系统升级后不拦截」这条不需要任何额外分支，「自动打开」由
     * [refreshDfRootState] 里的陈旧记录清理完成。
     *
     * <p><b>★ 2026-10-08：拦截门槛由「1 次」放宽到「连续 3 次」。</b>
     * 用户指出漏洞利用本就存在单次失败，一次不中不代表固件被修补。由于阈值就做在
     * [DfRootFailureRecordStore.read] 里，**本函数一行都不用改** ——
     * 只失败过 1、2 次时 read 返回 null，这里自然放行。
     *
     * <p>关闭方向**不查记录也不拦截** —— 关是用户的退路，任何时候都必须能关。
     */
    fun setDfRootEnabled(enable: Boolean) {
        if (!enable) {
            dfRootEnabled = false
            scope.launch {
                if (!DfRootManager.setEnabled(context, false)) {
                    android.widget.Toast.makeText(
                        context, strings.dfRootSetFailed, android.widget.Toast.LENGTH_LONG,
                    ).show()
                }
                refreshDfRootState()
            }
            return
        }
        // 打开方向：记录是**文件 IO**，放 IO 线程读（与项目「不阻塞主线程」纪律一致），
        // 读完回主线程决定放行还是拦截。乐观更新放在门禁之后 —— 被拦下时开关必须
        // 保持关闭，若先乐观置 true 再回滚，用户会看到开关「弹上去又掉下来」。
        scope.launch {
            val blocked = withContext(Dispatchers.IO) {
                DfRootFailureRecordStore.read(context) != null
            }
            if (blocked) {
                dfRootEnabled = false
                showDfRootUnsupportedDialog = true
                // 顺手把状态刷成事实（万一内存态与磁盘态已经不一致）
                refreshDfRootState()
                return@launch
            }
            dfRootEnabled = true
            if (!DfRootManager.setEnabled(context, true)) {
                android.widget.Toast.makeText(
                    context, strings.dfRootSetFailed, android.widget.Toast.LENGTH_LONG,
                ).show()
            }
            refreshDfRootState()
        }
    }

    /**
     * 打开 / 关闭 DFRoot 的「开机自启」。
     *
     * <p>写本应用自己的 DE prefs，并派生 [com.samsung.rootkit.dfroot.DfBootReceiver]
     * 的组件启用态 —— 全程零 su、零权限（改的是自己的存储与自己的组件）。
     *
     * <p><b>round35：打开时顺带申请通知权限</b> —— 开机自启是**唯一**会用到通知的地方
     * （缺管理器时提醒，见 [com.samsung.rootkit.dfroot.DfManagerNotice]），
     * 在别的时机弹权限请求既无必要也惹人烦。未授权不影响自启本身（提醒是 best-effort）。
     */
    fun setDfRootAutostart(enable: Boolean) {
        // 先乐观更新让开关立即响应；失败由随后的回读纠正回来
        dfRootAutostart = enable
        if (enable) DfManagerInstaller.requestNotificationPermissionIfNeeded(context)
        scope.launch {
            if (!DfRootManager.setAutostart(context, enable)) {
                android.widget.Toast.makeText(
                    context, strings.dfRootSetFailed, android.widget.Toast.LENGTH_LONG,
                ).show()
            }
            refreshDfRootState()
        }
    }

    /** 打开 / 关闭 DFRoot 的「自动软重启」（写本应用 DE prefs 的 auto_soft_reboot）。 */
    fun setDfRootAutoSoftReboot(enable: Boolean) {
        dfRootAutoSoftReboot = enable
        scope.launch {
            if (!DfRootManager.setAutoSoftReboot(context, enable)) {
                android.widget.Toast.makeText(
                    context, strings.dfRootSetFailed, android.widget.Toast.LENGTH_LONG,
                ).show()
            }
            refreshDfRootState()
        }
    }

    /**
     * 记下**下次开机**自启所用的变体（不跑链）。
     *
     * <p>用于「变体切换确认弹窗」的确认路径：`/dev/df` 是 O_EXCL 互斥锁，
     * 本次开机必然无法重复注入 ⇒ 只能把变体记下、等下次开机生效。
     */
    fun setDfRootVariant(variant: PayloadManager.KsuVariant) {
        val ksuNext = variant == PayloadManager.KsuVariant.KSU_NEXT
        if (!DfRootManager.setVariant(context, ksuNext)) {
            android.widget.Toast.makeText(
                context, strings.dfRootSetFailed, android.widget.Toast.LENGTH_LONG,
            ).show()
            return
        }
        android.widget.Toast.makeText(
            context,
            strings.dfRootSwitchNextBoot(
                if (ksuNext) strings.ksuVariantNext else strings.ksuVariantKsu
            ),
            android.widget.Toast.LENGTH_LONG,
        ).show()
    }

    // 修复⑧：统一的「立即安装 DFReroot」执行体，三个入口共用：
    //   a) 软重启后重进 App 的自动安装（下方 LaunchedEffect）
    //   b) 主页 DF 入口**态2 点击**（已注入、未装 → 直接装，不再绕道"再弹一次软重启"）
    //   c) 安装弹窗的确认按钮（重试）
    // 兜底：若其实还没软重启过（PMS 未重读 packages.xml），pm install 会撞
    // INSTALL_FAILED_SHARED_USER_INCOMPATIBLE → 自动转「软重启确认」弹窗，
    // 重启后由 LaunchedEffect 接力自动安装，两条路都闭环。
    fun runDfInstallNow() {
        dfBusy = true
        dfOutput = ""
        scope.launch {
            val su = app.rootExecutor.resolveWorkingSu()
            if (su == null) {
                dfBusy = false
                showDfInstallDialog = false
                showDfSuDialog = true
                return@launch
            }
            // 运行记录 begin（DFE 源）：只记「真的开始执行」的步骤，
            // 上面的 su 预检不通过属于**守卫出口**、不是一次运行，不落记录。
            val attempt = RunLogStore.begin(
                context,
                RunSource.DFE,
                RunLogStore.defaultModel(),
                RunLogStore.defaultFirmware(),
            )
            val steps = DfRunner.installDf(context, su)
            dfOutput = steps.joinToString("\n") {
                (if (it.ok) "[OK] " else "[X] ") + it.name + "：" + it.cmd.summary
            }
            val allOk = steps.isNotEmpty() && steps.all { it.ok }
            // ── round34 需求③：DFE 链路提权成功 ⇒ 清理 /data/local/tmp ──────────────
            // 用户原话：「DFE链路提权成功也需要清理tmp下缓存文件」。
            // 「提权成功」在本流程的落点 = 首次配置的**安装步全部走完**（在那之前 injectCert /
            // softReboot 都还要靠 su，还没到「系统已经是 root」这个稳定态）。
            // 位置刻意在 RunLogStore.finish **之前**：清理结果要一并落进这条运行记录，
            // 否则记录里根本看不到它。本路径必然有 su（上面那条 pm install 就是用 su 跑的），
            // 故通道① 必成；失败只可能是「清理后仍有残留」这种**事实级**结论。
            if (allOk) {
                val cr = withContext(Dispatchers.IO) {
                    runCatching { DfPostRootCleaner.cleanNow(app.shizukuManager) }.getOrNull()
                }
                val cleanLines = cr?.lines
                    ?: listOf("清理 /data/local/tmp 过程异常（详见 Logcat 标签 DFROOT）")
                dfOutput = if (dfOutput.isEmpty()) cleanLines.joinToString("\n")
                else dfOutput + "\n" + cleanLines.joinToString("\n")
            }
            // 运行记录收尾：失败原因取第一条不 OK 的步骤（分步结果里首个异常最接近根因）。
            RunLogStore.finish(
                context,
                attempt,
                if (allOk) RunOutcome.SUCCESS else RunOutcome.FAILED,
                steps.firstOrNull { !it.ok }?.let { it.name + "：" + it.cmd.summary } ?: "",
                dfOutput.split("\n"),
            )
            if (allOk) SettingsStore.setDfPendingInstall(context, false)
            refreshDfState()
            dfBusy = false
            when {
                allOk -> {
                    android.widget.Toast.makeText(
                        context, strings.dfInstallOk, android.widget.Toast.LENGTH_LONG,
                    ).show()
                    showDfInstallDialog = false
                }
                steps.any { it.cmd.suMissing } -> {
                    showDfInstallDialog = false
                    showDfSuDialog = true
                }
                steps.any { it.cmd.output.contains("SHARED_USER_INCOMPATIBLE") } -> {
                    SettingsStore.setDfPendingInstall(context, true)
                    showDfInstallDialog = false
                    showDfSoftRebootDialog = true
                }
                // 其它失败：弹窗保持打开，dfOutput 里是分步结果，用户可点"重试"
            }
        }
    }

    // 触发一次 DFE 链：**优先内联**（不跳界面），不可用时回落跳转 DFReroot 界面。
    // 供主页按钮（onOpenDf）与「变体切换确认弹窗」的确认按钮共用。
    //
    // ⚠️ 实现见**下方** openDfWithVariant —— 它被刻意放在最后：Kotlin **局部函数无定义提升**，
    //    必须先定义 startDfeInline / openDfActivity，再写调用它们的 openDfWithVariant，
    //    否则编译期直接 Unresolved reference（已真机踩过）。

    /**
     * 内联触发 DFE 链（经 DFReroot `DfRunService`）。
     *
     * @param recordRunLog 是否写入「运行记录」（2026-10-07 需求①）。
     *   false = **系统已 root**，本次点击只是空转（DFReroot 侧见 `/dev/df` 存在即回
     *   rc=0、不重跑），不应在运行记录里留下一条假 SUCCESS。判据由调用方传入
     *   （主页按钮知道自己判出的 `systemRooted`）。
     * @return true = 请求已成功交给 DFReroot（或已有一次在跑、本次被忽略）；
     *         false = 回落条件命中，调用方应改走 [openDfActivity]。
     *
     * <h2>三条回落条件</h2>
     *   ① DFReroot 未装 / 版本旧（无 DfRunService）—— [DfBridge.isInlineCapable]；
     *   ② `startService` **同步**抛异常 —— 权限未授予、后台启动限制、组件被停用等。
     *      AMS 的权限与组件校验都在 `startService` 调用点同步完成 ⇒ 异常是可靠判据，
     *      这也是"先试内联、失败即跳转"能安全成立的原因；
     *   ③ 已在内联运行中 —— 忽略本次点击并返回 true（**不**改走跳转，
     *      否则会把正在跑的链再触发一遍）。
     *
     * <p>⚠️ 内联与跳转最终都落到 DFReroot 的同一把单飞门闩（`ExploitRunner.tryBegin`），
     * 所以即使判据偶尔错判，也不会出现两条链并发改写同一批页缓存。
     */
    fun startDfeInline(variant: DfBridge.KsuVariant, recordRunLog: Boolean = true): Boolean {
        if (dfeBusy) {
            Log.i("RootKitScreen", "dfe 内联已在运行，忽略重复触发")
            return true
        }
        if (!DfBridge.isInlineCapable(context)) {
            Log.i("RootKitScreen", "DFReroot 无 DfRunService（旧版），回落跳转")
            return false
        }
        return try {
            dfeOutput = ""
            dfeBusy = true
            // 运行记录 begin（DFE 源；2026-10-07 真机修复 问题①）。
            // 位置刻意在 startService **之前**：DONE 帧回来时要用同一个 attempt 收尾，
            // 若放在 startService 之后，理论上存在「服务极快跑完、DONE 帧先到」的窗口。
            // begin 只写一条很小的 pending 暂存（与 df 链同一个 RunLogStore），主线程开销
            // 可忽略；项目内既有先例：setDfEntryEnabled 也是主线程同步写 prefs。
            //
            // ★ 2026-10-07 需求①：`recordRunLog=false`（**系统已 root**）时**不落记录**。
            //   「运行记录」的语义是"一次 root 尝试"；而已 root 时点红/蓝按钮只是空转
            //   （DFReroot 侧见 /dev/df 存在即回 rc=0、不重跑任何东西），记进去只会把
            //   列表刷成一堆重复的 SUCCESS，掩盖真正的失败尝试。
            //   注意 [dfeAttempt] 必须显式置 null：它是「是否有待收尾记录」的唯一判据，
            //   若留着上一次的引用，DONE 帧会把一条**别人**的记录收成这次的结果。
            dfeAttempt = if (recordRunLog) {
                RunLogStore.begin(
                    context,
                    RunSource.DFE,
                    RunLogStore.defaultModel(),
                    RunLogStore.defaultFirmware(),
                )
            } else {
                Log.i("RootKitScreen", "dfe 内联：系统已 root，本次不写运行记录")
                null
            }
            context.startService(DfBridge.buildRunServiceIntent(context, variant))
            Log.i("RootKitScreen", "dfe 内联已发起：variant=$variant")
            true
        } catch (t: Throwable) {
            // 失败即回落：必须复位忙碌态，否则卡片会永久停在"运行中"。
            // 刚 begin 的那条"进行中"记录也要一并收成失败 —— 否则它会作为悬空暂存，
            // 被下次启动的 recoverPending 记成「中断」，凭空多出一条假记录。
            dfeBusy = false
            val pending = dfeAttempt
            dfeAttempt = null
            if (pending != null) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            RunLogStore.finish(
                                context, pending, RunOutcome.FAILED,
                                "内联启动失败（已回落跳转）", emptyList(),
                            )
                        }
                    }
                }
            }
            Log.w("RootKitScreen", "dfe 内联启动失败，回落跳转", t)
            false
        }
    }

    /**
     * 跳转 DFReroot 界面（**回落**路径，正文即原 [openDfWithVariant] 的实现）。
     *
     * 2026-10-02 F6 的修复在此保留：原先 `runCatching` 静默吞异常 —— 打开失败时
     * 用户点了毫无反馈；现在失败分支写日志 + Toast。
     */
    fun openDfActivity(variant: DfBridge.KsuVariant) {
        runCatching {
            context.startActivity(DfBridge.buildOpenIntent(variant))
        }.onFailure { e ->
            Log.w("RootKitScreen", "openDfActivity failed: variant=$variant", e)
            android.widget.Toast.makeText(
                context, strings.dfOpenFailed, android.widget.Toast.LENGTH_LONG,
            ).show()
        }
    }

    /**
     * 触发一次 DFE 链：**优先内联**（不跳界面），不可用时回落跳转 DFReroot 界面。
     *
     * <h2>两条路径</h2>
     * · **内联**（2026-10-07 新增，默认）：经 DFReroot 的 `DfRunService` 发起，链在
     *   DFReroot 自己的进程内跑（`ExploitRunner.runCore`，与开机自启**同一条**），
     *   日志经广播逐行回流 → [dfeOutput]，显示在 [dfeBusy] 时红蓝按钮下方那张卡里。
     * · **跳转**（回落，原行为）：`startActivity(DfBridge.buildOpenIntent(...))` ——
     *   在 DFReroot 版本旧（无 Service）、或 `startService` 被系统拒绝时启用。
     *
     * @param preferInline false = 强制走跳转（保留给"设置页 → 打开 DFReroot 界面"这类
     *   明确要看界面的入口）。
     * @param recordRunLog 仅内联路径有效：是否写「运行记录」（2026-10-07 需求①，
     *   见 [startDfeInline]）。主页按钮在**系统已 root** 时根本不会走到本函数
     *   （那个点击被「已 root 小提示」弹窗消化，见 HomePage 的 dfe onClick）；
     *   「切换下次开机管理器」确认路仍会在已 root 时传 false（2026-10-07 原样，
     *   用户 2026-10-08 定版：该弹窗功能不动）。
     *
     * <p>⚠️ 本函数必须**写在 [startDfeInline] / [openDfActivity] 之后** ——
     * Kotlin 局部函数没有定义提升。
     */
    fun openDfWithVariant(
        variant: PayloadManager.KsuVariant,
        preferInline: Boolean = true,
        recordRunLog: Boolean = true,
    ) {
        // 变体落盘：与既有行为逐字一致（无论最终走哪条路，先记下"最后使用变体"）。
        SettingsStore.setKsuVariant(
            context,
            if (variant == PayloadManager.KsuVariant.KSU_NEXT) 1 else 0
        )
        val bridgeVariant =
            if (variant == PayloadManager.KsuVariant.KSU_NEXT) DfBridge.KsuVariant.KSU_NEXT
            else DfBridge.KsuVariant.KSU
        if (preferInline && startDfeInline(bridgeVariant, recordRunLog)) return
        openDfActivity(bridgeVariant)
    }

    // 刷新时机：冷启动 / 切回主页 tab / 从向导返回 —— 三处覆盖了
    // 「首次进入」「root 流程结束回到主页」「软重启后重进」。装完 DFReroot 时另有显式刷新。
    // ★ 2026-10-08 修复 问题②：第四个 key [dfeRunFinishedTick] —— dfe 跑链结束（DONE 帧）时自增，
    //   使「root 成功」这一刻就重算主页状态，而不再依赖用户手动切 tab。
    LaunchedEffect(screen, mainTab, wizardPage, dfeRunFinishedTick) {
        refreshDfState()
        // df（DFRoot）状态同样在此刷新：冷启动 / 切 tab / 从向导返回三处已覆盖
        // 「安装后回来」「root 流程结束回到主页」「软重启后重进」。
        // 全部为零副作用读取（包可见性 + stat /dev/df + 读组件态 + 读兜底文件），可以常刷。
        refreshDfRootState()
        // dfe 的「开机自启动」（2026-10-05 新增）：必须排在 refreshDfState **之后**
        // —— 它的门控取 dfActive，而 dfActive 正是上面那一步算出来的。
        // 仅 dfEntryReady 时才真的起 su 去读（见函数注释）。
        refreshDfEntryAutostart()
        // dfe 的「D2 修复」（2026-10-07 新增）：与上一条同门控、同顺序纪律，
        // 独立发一次 su 读（见函数注释：分开写是为了两条通道各自独立失败）。
        refreshDfEntryD2Fix()
        // dfe 的「自动软重启」（2026-10-08 新增）：同样同门控、同顺序、独立发一次 su 读。
        refreshDfEntryAutoSoftReboot()
        // 软重启后重进：证书已注入（标记在 SharedPreferences 里，进程被杀也不会丢），
        // 此时才轮到安装 DFReroot —— PMS 已在重启时重读了 packages.xml。
        // 修复④：**自动安装**，不再等人点确认（弹窗仍显示进度/结果）。
        // su 授权在 KSU 里是持久的，软重启后依然有效，直接装即可。
        //
        // ★ 2026-10-05：本路径**有意不吃** dfe 总开关（与 root 成功弹窗的门控不同）。
        //   理由：走到这里说明 packages.xml 的证书**已经注入**（系统级改动已发生），
        //   这是「完成既定动作」，不是「发起新动作」—— 半途停下只会留下
        //   「证书已注入 + DFReroot 未装」的中间态；而该中间态恰恰**够不着**
        //   设置页的「卸载 DFReroot」卡（它的门控 dfEntryReady 要求已生效）。
        //   装完则 dfActive 成立，用户随后关掉总开关也能正常卸载。
        //   （被删的修复⑦「卸载卡无条件渲染」不在此恢复 —— round20 用户已定调废弃。）
        if (!dfInstallPrompted && !dfInstalled && DeviceSupport.dfSupported() &&
            SettingsStore.isDfPendingInstall(context)) {
            dfInstallPrompted = true
            showDfInstallDialog = true
            runDfInstallNow()
        }
    }

    // ── round24：返回前台刷新 df 状态 ──
    // 从别的页面 / 其它 App 返回时刷新一次。df 已内嵌，刷新是**纯本地读取**
    // （stat /dev/df + 读本应用 DE prefs + 组件态），零 IPC、可安全常刷。
    // 典型用途：开机自启跑完链后回到前台，状态卡立刻反映「本次开机已 Root」。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    // ★ 2026-10-08 修复 问题②：本块原先**漏了 refreshDfState()**，直接调下面几个刷新。
                    //   它有两个后果：① 主页状态卡 / 红蓝按钮（判据 dfMgrVariant，由本函数算）回到前台
                    //   时不刷新 —— 正是"root 成功后必须切 tab 才刷新"的一半成因；
                    //   ② 违反本文件自己写下的顺序纪律（见 refreshDfEntryAutostart 等函数的注释：
                    //   "依赖 dfActive，而 dfActive 由 refreshDfState 计算 ⇒ 必须先 refreshDfState"）
                    //   —— 缺它则下面三个 entry 刷新用的是**陈旧 dfActive**。
                    //   补在最前，既补齐顺序，也让「root 完成 → Next 管理器被 ksud 拉起 → 用户回到 SRK」
                    //   这一次 ON_RESUME 把变体刷成正确的 Next。
                    refreshDfState()
                    refreshDfRootState()
                    // dfe 的「开机自启动」也在此刷新 —— 这正是「两处联动」的反向半环：
                    // 用户从设置页点「配置 DFReroot」跳到 DFReroot 界面、在那边拨了自启开关
                    // 再回到 SRK，只有 ON_RESUME 这一次刷新才能把新值取回来
                    // （LaunchedEffect 只在冷启动/切 tab/返回向导时触发，回前台不触发）。
                    refreshDfEntryAutostart()
                    // D2 修复同理（2026-10-07）：在 DFReroot 界面拨了 D2 开关回来，靠这一次取回。
                    refreshDfEntryD2Fix()
                    // 自动软重启同理（2026-10-08）：在 DFReroot 主界面拨了它回来，靠这一次取回。
                    refreshDfEntryAutoSoftReboot()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── ksud 缺失兜底：执行阶段检测到 ksud 文件缺失时弹窗让用户手动选择 ──
    // state 声明在 startExecuting 之前（被 startExecuting 内 try-catch 引用）；
    // launcher 注册在 startExecuting 函数声明之后（launcher lambda 引用 startExecuting）
    var showKsudMissingDialog by remember { mutableStateOf(false) }
    var pendingKsudEntry by remember { mutableStateOf<PayloadEntry?>(null) }
    // 外部 ksud 不适配提示：选中的外部 ksud 版本号与设备内核主版本不匹配时弹窗
    var showKsudMismatchDialog by remember { mutableStateOf(false) }
    var mismatchKsudEntry by remember { mutableStateOf<PayloadEntry?>(null) }
    // 全局持久：用户通过 ksudFallbackLauncher 选定的 ksud 文件（覆盖整个 Wizard 流程）。
    // 预检测场景：用户进入 ModelSelectPage 时内置 ksud 缺失 → 弹窗让用户先选一个 → 之后所有
    // startExecuting 调用都会自动带上这个 customKsudFile,不再重复弹窗。
    var globalCustomKsudFile by remember { mutableStateOf<java.io.File?>(null) }
    // 预检测只弹一次：避免用户取消弹窗后,后续切回 ModelSelect 又重复弹出。
    var ksudPreCheckDone by remember { mutableStateOf(false) }

    // ── 启动路由：仅依据「是否已确认当前免责声明版本」决定进主页还是免责声明页 ──
    // 无论系统是否已 root，首次运行都必须先阅读并确认免责声明，不允许跳过。
    LaunchedEffect(Unit) {
        // 启动时清理上次运行时残留的自定义文件缓存,避免 cacheDir 越用越大。
        // SAF 选的文件复制到 cacheDir/custom_payloads/,如果不清会随使用累积。
        withContext(Dispatchers.IO) {
            val customDir = java.io.File(context.cacheDir, "custom_payloads")
            if (customDir.exists()) {
                customDir.listFiles()?.forEach { it.delete() }
                // 目录本身保留,便于本次 SAF 复制
            }
        }
        withContext(Dispatchers.IO) {
            if (SettingsStore.hasAcceptedCurrentDisclaimer(context)) {
                // 只在屏幕状态异常（如 rememberSaveable 意外恢复成 Disclaimer）时修正；
                // 不重置 mainTab——Activity 重建后用户可能停留在 Settings tab，强制重置会跳回 Home
                if (screen == Screen.Disclaimer) screen = Screen.Main
            }
        }
    }

    // ── 预检测：进入 ModelSelectPage 时主动检查内置 ksud 文件是否存在 ──
    // 触发条件:用户进入机型选择页面 + 未做过预检测 + 未保存用户 ksud + 内置 ksud 全部缺失
    // 弹窗让用户先选一个 ksud,选完后存到 globalCustomKsudFile 供后续所有 startExecuting 使用。
    // 只弹一次(ksudPreCheckDone),避免用户取消后切回 ModelSelect 又重复弹。
    // 关键:hasAnyBuiltinKsud() 内部会调 assets.open,在 IO 线程跑避免阻塞 Main 线程导致检测丢失。
    LaunchedEffect(wizardPage) {
        if (!ksudPreCheckDone && wizardPage == Page.ModelSelect && globalCustomKsudFile == null) {
            val hasBuiltin = withContext(Dispatchers.IO) {
                app.payloadManager.hasAnyBuiltinKsud()
            }
            if (!hasBuiltin) {
                ksudPreCheckDone = true
                showKsudMissingDialog = true
            }
        }
    }
    var pendingFirmwareEntry by remember { mutableStateOf<PayloadEntry?>(null) }

    // Wizard 页统一返回：执行中需要二次确认
    val backToHome: () -> Unit = {
        screen = Screen.Main
        mainTab = MainTab.Home
    }
    val backFromExecuting: () -> Unit = {
        if (currentStage != RootExecutor.Stage.DONE && currentStage != RootExecutor.Stage.FAILED) {
            showExecCancelDialog = true
        } else {
            wizardPage = Page.ModelSelect
        }
    }
    val backFromModelSelect: () -> Unit = {
        // 跳过 ShizukuCheck 直接回到主页（脚本里也是这一步直接跳选机型，不强制回 shizuku 检查）
        backToHome()
    }
    val backFromResult: () -> Unit = {
        // 完成页「直接返回」（系统返回键/手势）→ 回主页，与顶部返回箭头(onBackHome)一致。
        // 「再次执行」按钮另走 onRetry → 选择机型页（保持现有逻辑不动）。
        backToHome()
    }

    BackHandler(enabled = screen == Screen.Wizard) {
        when (wizardPage) {
            Page.ShizukuCheck -> backToHome()
            Page.ModelSelect -> backFromModelSelect()
            Page.Executing -> backFromExecuting()
            Page.Result -> backFromResult()
            Page.RiskWarning -> backToHome()
        }
    }
    BackHandler(enabled = screen == Screen.Disclaimer && disclaimerFromSettings) {
        screen = Screen.Main
        mainTab = MainTab.Settings
        disclaimerFromSettings = false
    }

    var isExecuting by remember { mutableStateOf(false) }
    // 玻璃渲染开关：仅由执行页自动停用驱动（2026-09-26 移除「提权性能模式」「提权时自动熄屏」
    // 两个设置项后，非执行页恒为 true）；false 时降级为静态磨砂，消除 backdrop 捕获/模糊的 GPU 开销。
    var glassEnabled by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // ── 执行页玻璃停用 + 60Hz 锁定 ──
    // 进入 Executing 页时：关闭玻璃渲染（GlassCard 降级为简单玻璃，消除 backdrop GPU 开销）
    //   + 锁定刷新率至 60Hz（降低功耗与发热，root 执行不需要高刷）。
    // 离开 Executing 页时（进入 Result 或任何其他页）：恢复玻璃渲染 + 解锁刷新率。
    // 使用 Window.preferredDisplayModeId 查找 60Hz 模式并设置（API 31+ 兼容）。
    LaunchedEffect(wizardPage) {
        val activity = context as? android.app.Activity ?: return@LaunchedEffect
        val window = activity.window
        if (wizardPage == Page.Executing) {
            glassEnabled = false
            // 锁 60Hz：在支持的显示模式中查找 refreshRate ≈ 60 的模式，并通过 setAttributes 重新提交给 WindowManager。
            // ⚠️ 关键修复（Round 实测）：直接写 window.attributes.preferredDisplayModeId = X 只改了
            //   Window.mWindowAttributes 字段但从未调用 setAttributes()，WindowManager 不会重新下发，
            //   刷新率改动被静默丢弃。必须「取出 params → 改字段 → 整体 setAttributes(params)」才生效。
            try {
                val displayManager = context.getSystemService(android.content.Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
                val display = displayManager?.getDisplay(android.view.Display.DEFAULT_DISPLAY)
                val modes = display?.supportedModes
                // 优先精确匹配 60Hz；找不到则回落到支持模式中刷新率最低（且 <= 60Hz）的模式，避免落到 LTPO 1Hz。
                val mode60 = modes?.find { it.refreshRate >= 59.5f && it.refreshRate <= 60.5f }
                    ?: modes?.filter { it.refreshRate <= 60.5f }?.minByOrNull { it.refreshRate }
                Log.d("RootKit/60Hz", "Entering Executing: supportedModes=${modes?.size}, chosen modeId=${mode60?.modeId}, refreshRate=${mode60?.refreshRate}, current=${display?.refreshRate}")
                if (mode60 != null) {
                    val params = window.attributes
                    params.preferredDisplayModeId = mode60.modeId
                    window.attributes = params   // 关键：整体提交，触发 WMS 重新下发并切换刷新率
                } else {
                    Log.w("RootKit/60Hz", "No ~60Hz display mode found; skip lock")
                }
            } catch (e: Exception) {
                Log.e("RootKit/60Hz", "Lock 60Hz failed", e)
            }
        } else {
            // 离开执行页：恢复玻璃渲染（执行页自动停用是玻璃关闭的唯一来源）+ 恢复默认刷新率
            glassEnabled = true
            try {
                val params = window.attributes
                params.preferredDisplayModeId = 0
                window.attributes = params
                Log.d("RootKit/60Hz", "Restored default display mode")
            } catch (e: Exception) {
                Log.e("RootKit/60Hz", "Unlock 60Hz failed", e)
            }
        }
    }

    fun startExecuting(entry: PayloadEntry, forceFirmware: Boolean = false, overrideKsudName: String? = null, customKsudFile: File? = null, variant: PayloadManager.KsuVariant = PayloadManager.KsuVariant.KSU) {
        execVariant = variant
        if (isExecuting) return // 防重入守卫：后台任务仍在进行时防止并发启动引发 payload 竞争损坏
        // 固件不匹配检查（与 run_root.sh 的 "force continue? (type YES)" 对齐）
        // 脚本在固件不匹配时交互式询问用户，不输入 YES 则退出；
        // App 之前静默继续 → 用户可能在不知情下对不匹配固件强行 root 导致失败。
        // 自定义文件条目跳过固件校验：firmware 由用户选择/填写，可能与当前设备不完全一致。
        if (!forceFirmware && !entry.isCustom && deviceInfo != null &&
            !deviceInfo!!.fingerprint.orEmpty().contains(entry.firmware, ignoreCase = true)
        ) {
            pendingFirmwareEntry = entry
            showFirmwareMismatchDialog = true
            return
        }
        selectedPayload = entry
        logs = emptyList()
        currentStage = RootExecutor.Stage.DEVICE_CHECK
        stageMessage = ""
        wizardPage = Page.Executing
        isExecuting = true
        // ── 运行记录 begin（2026-10-05：三源统一）──────────────────────────────
        // 先落一条「进行中」的暂存记录，结束时由 RunLogStore.finish 改写成最终态。
        // 若本次跑到一半进程就没了（软重启 / 被系统清理 / 崩溃），暂存记录会留在盘上，
        // 由下次启动的 RunLogStore.recoverPending 收尾成「运行中断」——
        // 这类记录原先根本不会出现在运行记录页里（只在成功返回后才写）。
        val runAttempt = RunLogStore.begin(
            context,
            RunSource.SRK,
            entry.board,
            entry.firmware,
        )
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    app.rootExecutor.onProgress = { s, m ->
                        mainHandler.post {
                            currentStage = s
                            stageMessage = m
                        }
                    }
                    app.rootExecutor.onLog = { l, m ->
                        mainHandler.post {
                            logs = logs + LogEntry(l, m, System.currentTimeMillis())
                        }
                    }
rootSuccess = try {
                app.rootExecutor.executeRoot(entry, overrideKsudName, customKsudFile, execVariant)
            } catch (e: Exception) {
                // ksud 文件缺失兜底：内置 ksud 被用户删除 / 自定义 ksud 文件丢失时,让用户手动选一个 ksud 重试。
                // 不动 RootExecutor 的核心逻辑,仅在 UI 层捕获异常做兜底。
                val msg = e.message.orEmpty()
                val isKsudMissing = (e is IllegalStateException && msg.contains("ksud", ignoreCase = true)) ||
                                    (e is java.io.FileNotFoundException && msg.contains("tools/ksud")) ||
                                    msg.contains("ksud not found", ignoreCase = true)
                if (isKsudMissing) {
                    logs = logs + LogEntry(RootExecutor.LogLevel.ERR, "ksud 文件缺失: ${msg}", System.currentTimeMillis())
                    pendingKsudEntry = entry
                    showKsudMissingDialog = true
                    currentStage = RootExecutor.Stage.FAILED
                    stageMessage = "缺少 ksud 文件,请手动选择后重试"
                } else {
                    logs = logs + LogEntry(RootExecutor.LogLevel.ERR, "执行异常: ${e.javaClass.simpleName}: ${msg}", System.currentTimeMillis())
                }
                false
            }
            if (rootSuccess) {
                // 成功后清理 cacheDir/custom_payloads/ 本地副本:文件已推送设备,本地副本不再需要,
                // 避免下次选择新文件时与旧文件累积。同时清空 ksudFallbackLauncher 的全局 ksud 缓存,
                // 让用户下次进入 ModelSelectPage 重新走预检测/缓存流程(若内置 ksud 缺失)。
                val customDir = File(context.cacheDir, "custom_payloads")
                if (customDir.exists()) customDir.listFiles()?.forEach { it.delete() }
                globalCustomKsudFile = null
                ksudPreCheckDone = false
            }
                }
                // 运行记录收尾：结局 + 原因 + 全量日志。
                // 失败原因取 stageMessage（RootExecutor 在置 FAILED 阶段时同步写入的一句话）；
                // 它为空的兜底是「最后一条日志」—— onProgress 是 post 到主线程的，
                // 收尾这一刻可能还没轮到执行，取日志末行比留空更接近事实。
                // 成功则留空 —— 列表里没必要为成功再占一行。
                RunLogStore.finish(
                    context,
                    runAttempt,
                    if (rootSuccess) RunOutcome.SUCCESS else RunOutcome.FAILED,
                    if (rootSuccess) "" else stageMessage.ifBlank { logs.lastOrNull()?.message.orEmpty() },
                    logs.map { it.message },
                )
                wizardPage = Page.Result
                // ── 集成版（v5.1.0）：root 成功后提示启用 DFReroot 持久化 ──
                // root 成功后自动弹一次（主动引导）；此外首页第三个入口未生效时虽然灰着
                // 但**可点**，点击同样弹这个配置弹窗（修复②）——两条路都通向同一弹窗。
                // 仅在「尚未安装」时提示；已安装（含已生效）不再打扰。
                if (rootSuccess) {
                    refreshDfState()
                    // S24 系列（android14-6.1）无 6.1 内核 ksud 资产，DFReroot 整体不支持：
                    // root 成功后不再弹 DF 安装提醒（2026-09-26）。
                    // 2026-10-05：再加 **DFReroot 总开关**门控 —— 关掉总开关即「不要持久化
                    // root」，此时再弹引导就是打扰。这是该开关最直接的可见效果（用户定调）。
                    // 用实时读而非组合期状态快照：与紧邻的 dfInstalled 同理，取「此刻的事实」。
                    if (SettingsStore.isDfEntryEnabled(context) &&
                        !dfInstalled && DeviceSupport.dfSupported()
                    ) {
                        showDfEnableDialog = true
                    }
                }
            } finally {
                isExecuting = false
            }
        }
    }

    // ── ksud 缺失兜底：执行阶段检测到 ksud 文件缺失（用户自行删除内置 ksud / 自定义 ksud 丢失）时 ──
    // 弹窗让用户手动选择 ksud 文件,选完后用 customKsudFile 重新执行(走之前方案 A 的精确路径通道)
    // 位置：在 startExecuting 函数声明之后才注册 launcher,避免 lambda 引用未声明的局部变量(forward reference)
    // ksud fallback 单文件 SAF launcher（与 ModelSelectPage 的 safLauncher 独立,避免干扰多选文件流程）
    // 处理两种场景：
    //   1. retry 场景（pendingKsudEntry != null）：执行阶段 ksud 缺失 → 用户选完后立即重试该 entry
    //   2. 预选场景（pendingKsudEntry == null）：进入 ModelSelect 时内置 ksud 全缺失 → 选完后存到
    //      globalCustomKsudFile,后续所有 startExecuting 都自动带上
    val ksudFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val retryEntry = pendingKsudEntry
            scope.launch(Dispatchers.IO) {
                // 取文件名用于适配性检测（从文件名提取内核版本号与设备比对）
                val ksudDisplayName = try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                        if (c.moveToFirst()) {
                            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (idx >= 0) c.getString(idx) else null
                        } else null
                    }
                } catch (_: Exception) { null }

                val customDir = File(context.cacheDir, "custom_payloads").apply { mkdirs() }
                val ksudFile = File(customDir, "ksud-fallback-${System.currentTimeMillis()}")
                val ok = try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        ksudFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    true
                } catch (_: Exception) { false }
                if (!ok) {
                    rootSuccess = false
                    if (retryEntry != null) wizardPage = Page.Result
                    return@launch
                }

                // 适配性检测：从 ksud 文件名提取主版本号（如 6.1），与设备 kernel 主版本比对。
                // 提取不到版本号时返回 null = 无法判断,放行（不误伤任意命名的文件）。
                val ksudVer = ksudDisplayName?.let { Regex("""(\d+\.\d+)""").find(it)?.groupValues?.get(1) }
                val kernelMain = deviceInfo?.kernel?.let { Regex("""^(\d+\.\d+)""").find(it)?.value }
                val mismatch = ksudVer != null && kernelMain != null && ksudVer != kernelMain

                if (mismatch) {
                    // 不适配：弹窗提示,保留 mismatchKsudEntry 供用户重新选
                    showKsudMissingDialog = false
                    showKsudMismatchDialog = true
                    mismatchKsudEntry = retryEntry
                    return@launch
                }

                pendingKsudEntry = null
                showKsudMissingDialog = false
                if (retryEntry != null) {
                    // retry 场景:重置 UI 状态,再次走完整 startExecuting 流程
                    // 关键:必须传 overrideKsudName="ksud-custom-user",否则 executeRoot 会
                    // 根据 kmi/kernel 重新算出内置 ksud 名,getKsudAsset 忽略 customKsudFile
                    // 继续读已删除的内置文件 → 反复失败死循环。
                    isExecuting = false
                    logs = emptyList()
                    currentStage = RootExecutor.Stage.DEVICE_CHECK
                    stageMessage = ""
                    startExecuting(retryEntry, overrideKsudName = "ksud-custom-user", customKsudFile = ksudFile, variant = execVariant)
                } else {
                    // 预选场景:存到 globalCustomKsudFile,后续 startExecuting 会自动用
                    globalCustomKsudFile = ksudFile
                }
            }
        } else {
            // 用户取消选择,保持当前失败状态
            showKsudMissingDialog = false
            pendingKsudEntry = null
        }
    }

    // 清除 RootKitScreen 层级的自定义文件缓存（cacheDir/custom_payloads/）。
    // 执行阶段弹窗（缺少 ksud / ksud 不适配）的「取消」按钮都调用它,避免 ksud-fallback 文件堆积。
    fun clearRootCache() {
        scope.launch(Dispatchers.IO) {
            val customDir = File(context.cacheDir, "custom_payloads")
            if (customDir.exists()) customDir.listFiles()?.forEach { it.delete() }
        }
    }

    fun startWizard(shizukuReady: Boolean = false, variant: PayloadManager.KsuVariant = PayloadManager.KsuVariant.KSU) {
        execVariant = variant
        // 记录「最后一次使用的变体」，供设置页「打开 DFReroot」联动（2026-10-01）。
        SettingsStore.setKsuVariant(context, if (variant == PayloadManager.KsuVariant.KSU_NEXT) 1 else 0)
        logs = emptyList()
        currentStage = RootExecutor.Stage.DEVICE_CHECK
        stageMessage = ""
        rootSuccess = false
        if (!shizukuReady) {
            // 主页入口 + Shizuku 未就绪：跳到 ShizukuCheckPage 引导用户完成授权
            // startFromMainButton=true 让 ShizukuCheckPage onReady 继续 wizard 进入 ModelSelect
            startFromMainButton = true
            wizardPage = Page.ShizukuCheck
        } else {
            // 主页入口 + Shizuku 已就绪：直接进 ModelSelect
            startFromMainButton = false
            wizardPage = Page.ModelSelect
            scope.launch {
                withContext(Dispatchers.IO) {
                    deviceInfo = app.rootExecutor.getDeviceInfo()
                    app.payloadManager.extractPayloads()
                    payloads = app.payloadManager.getPayloadList()
                }
                // 不再支持重启续跑：每次进入引导都从机型选择重新开始
                wizardPage = Page.ModelSelect
            }
        }
        screen = Screen.Wizard
    }

    CompositionLocalProvider(LocalStrings provides strings) {
    // 玻璃总开关提升至顶层：同时覆盖 Screen.Main（首页/设置/底部胶囊栏）与 Screen.Wizard。
    // 修复：此前 Provider 只包在 Screen.Wizard 内，设置/首页读到的 LocalGlassEnabled 恒为默认 true。
    // （2026-09-26 移除「提权性能模式」设置项后，glassEnabled 仅由执行页自动停用驱动。）
    CompositionLocalProvider(LocalGlassEnabled provides glassEnabled) {
    when (screen) {
        Screen.Main -> {
            // 下栏常驻显示（已取消自动隐藏）
            GlassSkyProvider(
                overlay = {
                    Box(
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.BottomCenter)
                            .padding(bottom = 28.dp)
                    ) {
                        MainBottomBar(
                            selected = mainTab,
                            onSelect = { mainTab = it },
                        )
                    }
                }
            ) {
                AnimatedContent(targetState = mainTab, label = "main-tab") { tab ->
                    when (tab) {
                        MainTab.Home -> HomePage(
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(bottom = 16.dp),
                            rootExecutor = app.rootExecutor,
                            shizukuManager = app.shizukuManager,
                            onStartWizard = { variant, shizukuReady ->
                                startWizard(shizukuReady = shizukuReady, variant = variant)
                            },
                            // DFReroot 持久化状态：仅供状态卡第四行显示。
                            dfInstalled = dfInstalled,
                            dfActive = dfActive,
                            // 2026-10-05：dfe（DFReroot）总开关。关掉 ⇒ 两个按钮的 dfe 分支
                            // 整体失效（回退「开始 Root 引导」）、状态卡第四行显示「未配置」，
                            // 与设置页 dfe 组的显隐、root 成功后的弹窗门控同一事实源。
                            dfEntryEnabled = dfEntryEnabled,
                            // 持久化可用时主页按钮直达 DFReroot（2026-10-01 晚）：
                            // 先把所点变体写入持久化（与 startWizard 同一联动状态，
                            // 保证设置页「打开 DFReroot」/ DFReroot 内 AUTO 兜底都对齐），
                            // 再走与设置页完全相同的 buildOpenIntent(variant) 路径。
                            onOpenDf = { variant, systemRooted ->
                                // ── 2026-10-07 需求①：主页把「系统已 root」一并带上来 ──
                                // 用于决定**要不要写运行记录**：已 root 时点按钮只是空转
                                // （DFReroot 侧见 /dev/df 存在即回 rc=0、不重跑），不该在
                                // 「运行记录」里留一条假 SUCCESS。判据与 df 链的 systemRooted
                                // 完全同源（[HomePage] 的 hasRoot || ksuWorking）。
                                //
                                // 2026-10-01 晚：切换版本确认 ——
                                //   先检测 DFReroot「开机自启动」是否打开，若打开且目标变体 ≠
                                //   当前工作管理器，弹窗确认「下次开机自动 root 切换到 XX」，
                                //   确认后才跳 DFReroot（跳转时 DFReroot 会覆盖写对应 ksud）。
                                //   自启未打开（开机后手动 root）→ 不弹窗，直接跳。
                                //
                                // ★ 2026-10-01 深夜三修（真机实锤）：「当前工作管理器」判定统一走
                                //   resolveWorkingVariant（2026-10-02 起最高优先级 = 内核自报版本码
                                //   ksud 探测；其后 df-boot-ok → df-boot.log 加载记录 → 已安装唯一性）。
                                //   variant 键是「下次开机的待生效选择」而非
                                //   当前工作（真机：工作中=Next、prefs=ksu），拿它比对会反向。
                                //   另注：两支 LKM 在内核里同名 kernelsu（KernelSU-Next 上游
                                //   同样是 kernelsu.ko），旧"内核模块名区分变体"的判据恒错。
                                scope.launch(Dispatchers.IO) {
                                    val autostart = app.rootExecutor.isDfAutostartEnabled()
                                    val current = app.rootExecutor.resolveWorkingVariant()
                                    val target = variant
                                    val needConfirm = autostart == true && current != null && current != target
                                    withContext(Dispatchers.Main) {
                                        if (needConfirm) {
                                            pendingSwitchVariant = target
                                            // 2026-10-07 需求①：把「点击时是否已 root」一起
                                            // 存档 —— 确认按钮在弹窗里，那里已取不到主页的
                                            // systemRooted，而它决定要不要写运行记录。
                                            pendingSwitchSystemRooted = systemRooted
                                            // ★ 必须显式置 false：pendingSwitchIsDfRoot 是
                                            //   「弹窗来源」的唯一判据，确认分支据此二选一。
                                            //   df 路径（startDfRootEntry）会把它置 true，若不在此复位，
                                            //   用户先走过一次 df 切换、再走 dfe 切换时，确认后会被
                                            //   误当成 df 处理（setDfRootVariant + runDfRootChain），
                                            //   而不是 openDfWithVariant —— 属于静默走错分支。
                                            pendingSwitchIsDfRoot = false
                                            showVariantSwitchDialog = true
                                        } else if (systemRooted) {
                                            // ★ 2026-10-08 二次调整（用户定版）：已 root ⇒ **不跳 UI、
                                            //   也不内联**，与 df 链同款的小提示弹窗如实告知
                                            //   （复用 ⑧ 的 [showDfRootAlreadyRootedDialog]，文案
                                            //   「当前系统已经 root，请勿重复操作。」两者通用）。
                                            //   上一版曾改回「跳 DFE UI」，用户定版：不跳。
                                            showDfRootAlreadyRootedDialog = true
                                        } else {
                                            // 未 root ⇒ 内联跑链（2026-10-07 定义的跑链入口不变，
                                            //   写运行记录 —— 「已 root 不写记录」的豁免只属于已 root）。
                                            openDfWithVariant(target)
                                        }
                                    }
                                }
                    },
                    // ── df（DFRoot）状态与按钮入口（round24：df 已内嵌，按钮即跑链入口）──
                    // 优先级：df（总开关开 && 机型支持）> dfe（持久化生效）> Root 向导。
                    // 服务端接线：先查自启/当前工作管理器决定要不要弹「下次开机切换」确认，
                    // 然后**在本进程内跑链**。
                    dfRootEnabled = dfRootEnabled,
                    dfRootArmed = dfRootArmed,
                    // 跑链中的实时日志（2026-10-05 需求①）：交给主页那张「设备信息卡」
                    // 在跑链期间临时让位显示，跑完（busy=false）自动恢复设备信息。
                    dfRootBusy = dfRootBusy,
                    dfRootOutput = dfRootOutput,
                    onRunDfRoot = { variant, rooted -> startDfRootEntry(variant, rooted) },
                    // dfe（DFReroot）**内联**跑链的实时日志（2026-10-07）：与 df 链共用同一张
                    // 卡片 —— 谁在跑就显示谁的日志。两条链互斥（都跑同一个 DFReroot 单飞门闩）。
                    dfeBusy = dfeBusy,
                    dfeOutput = dfeOutput,
                )
                MainTab.RunLog -> RunLogPage(
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(bottom = 16.dp),
                            context = context,
                        )
                        MainTab.Settings -> SettingsPage(
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(bottom = 16.dp),
                            context = context,
                            themeModeState = themeModeState,
                            dynamicColorState = dynamicColorState,
                            onReenterDisclaimer = {
                                disclaimerFromSettings = true
                                screen = Screen.Disclaimer
                            },
                            onLanguageChanged = { currentLanguage = it },
                            // DFReroot 卸载入口：真正执行在父级的弹窗里（避免 SettingsPage
                            // 直接依赖 DfRunner 与协程作用域），这里只负责把弹窗打开。
                            dfInstalled = dfInstalled,
                            dfActive = dfActive,
                            dfKeyInjected = dfKeyInjected,
                            // 2026-10-02 F8 修复：设置页 DF 卡片与主页两个按钮共用同一判据
                            // （dfActive && dfSupportedOnDevice），消除"主页拦截、设置页放行"的分叉。
                            dfEntryReady = dfActive && dfSupportedOnDevice,
                            // 2026-10-05：dfe 总开关 —— 设置页 dfe 组第一张卡（与 df 组同构）。
                            // 注意与 dfEntryReady 的**分工**：
                            //   · dfEntryEnabled  = 用户意愿（要不要持久化 root）→ 门控
                            //                        「配置 DFReroot」卡与主页 dfe 分支；
                            //   · dfEntryReady    = 客观能力（已生效 && 机型支持）→ 门控
                            //                        「卸载 DFReroot」卡（关掉总开关也必须能卸）。
                            dfEntryEnabled = dfEntryEnabled,
                            onDfEntryEnabledChange = { setDfEntryEnabled(it) },
                            // dfe 的「开机自启动」（2026-10-05 新增）：三态，null = 读不到（无 su）⇒ 卡片禁用。
                            // 与 DFReroot 自身界面上的开关是同一份状态（见 setDfEntryAutostart 注释）。
                            dfEntryAutostart = dfEntryAutostart,
                            onDfEntryAutostartChange = { setDfEntryAutostart(it) },
                            // dfe 的「D2 修复」（2026-10-07 新增）：与上一条同构、同门控、同三态。
                            dfEntryD2Fix = dfEntryD2Fix,
                            onDfEntryD2FixChange = { setDfEntryD2Fix(it) },
                            // dfe 的「自动软重启」（2026-10-08 新增）：与上面两条同构、同三态、
                            // 同一份状态文件；卡片位置在「开机自启动」**下面**（用户定的），
                            // 并与另外两张行为卡一样**随 dfe 总开关隐藏**。
                            dfEntryAutoSoftReboot = dfEntryAutoSoftReboot,
                            onDfEntryAutoSoftRebootChange = { setDfEntryAutoSoftReboot(it) },
                            onUninstallDf = {
                                // 每次从设置页进入都从「确认态」开始（复位上次的完成态标记）
                                dfUninstallCompleted = false
                                showDfUninstallDialog = true
                            },
                            // 「打开 DFReroot」入口（2026-10-01）：主页入口按钮迁来此处，
                            // 打开 DFReroot UI 并把当前选中的变体经 extra 传入。
                            // 2026-10-02 F6 修复（同类缺陷第二处）：这里原先自带一份 runCatching
                            // 静默吞异常。统一改走 openDfWithVariant —— 与主页入口同一路径、
                            // 同一失败提示，避免两条入口行为不一致。变体仍取持久化的「最后使用变体」。
                            // 2026-10-02 F8：补 dfSupported() 门控，不受支持机型一律弹「暂不适配」。
                            onOpenDf = {
                                if (!dfSupportedOnDevice) {
                                    showDfUnsupportedDialog = true
                                } else {
                                    // ★ 2026-10-07 真机修复（问题②）：必须显式传 preferInline = false。
                                    //   本卡的语义就是「打开 DFReroot 界面」——它上行状态值读的是
                                    //   dfEntryReady，即「已生效 ⇒ 点击直达 UI」（见 L6669 注释）。
                                    //   此前漏传该参数 ⇒ 走了默认 true（内联），结果是：设置页里
                                    //   **不跳界面**、链在后台悄悄跑，而日志卡长在主页、此处看不到，
                                    //   用户观感就是"点了一点反应都没有、DFReroot 界面再也进不去"。
                                    //   分工（2026-10-08 修订）：主页两个红蓝按钮 = 未 root 时内联
                                    //   跑链、已 root 时跳 UI（原规则回归）；本卡 = 恒 UI 入口。
                                    openDfWithVariant(
                                        if (SettingsStore.getKsuVariant(context) == 1)
                                            PayloadManager.KsuVariant.KSU_NEXT
                                        else PayloadManager.KsuVariant.KSU,
                                        preferInline = false,
                                    )
                                }
                            },
                            // 「配置 DFReroot」入口（2026-10-01 深夜）：未加载时点卡片
                            // 弹配置弹窗（与 root 完成后的 showDfEnableDialog 同源）。
                            // 2026-10-02 F8：补 dfSupported() 门控 —— 该弹窗确认后会走入
                            // 「注入证书 → 软重启 → 装 DFReroot」链路，与 root 成功弹窗
                            // （其前置已门控）同源，故必须同门控，否则 S24 可由此处绕过拦截。
                            onConfigureDf = {
                                if (dfSupportedOnDevice) {
                                    showDfEnableDialog = true
                                } else {
                                    showDfUnsupportedDialog = true
                                }
                            },
                            // ── df（DFRoot）区（round24：单包内嵌后只剩开关）──
                            dfRootEnabled = dfRootEnabled,
                            dfRootAutostart = dfRootAutostart,
                            dfRootAutoSoftReboot = dfRootAutoSoftReboot,
                            onDfRootEnabledChange = { setDfRootEnabled(it) },
                            onDfRootAutostartChange = { setDfRootAutostart(it) },
                            onDfRootAutoSoftRebootChange = { setDfRootAutoSoftReboot(it) },
                        )
                    }
                }
            }
        }
        else -> {
            GlassSkyProvider {
            when (screen) {
        Screen.Main -> {}
        Screen.Disclaimer -> RiskWarningPage(
            onConfirm = {
                SettingsStore.acceptCurrentDisclaimer(context)
                if (disclaimerFromSettings) {
                    // 从设置页查看免责 → 确认后回到主页,不进入流程
                    screen = Screen.Main
                    mainTab = MainTab.Settings
                    disclaimerFromSettings = false
                } else {
                    // 冷启动首次确认 → **直接进主页**（2026-10-02 按要求取消"确认后强制进
                    // Shizuku 授权流程"这一步：首次装完 App 就该看到主页，而不是被推着去装
                    // /授权 Shizuku）。
                    //
                    // Shizuku 授权改为**按需**：用户在主页点「开始 Root 引导」时若 Shizuku
                    // 未就绪，才跳到 ShizukuCheckPage（见 [startWizard] 的 `!shizukuReady` 分支，
                    // 那里会置 `startFromMainButton`，授权完成后继续进 ModelSelect）。
                    screen = Screen.Main
                    mainTab = MainTab.Home
                }
            },
            onCancel = {
                if (disclaimerFromSettings) {
                    screen = Screen.Main
                    mainTab = MainTab.Settings
                    disclaimerFromSettings = false
                } else {
                    activity?.finish()
                }
            },
            showBackButton = disclaimerFromSettings,
        )

        Screen.Wizard -> {
            // LocalGlassEnabled 已提升至顶层 Provider（覆盖 Main+Wizard 全屏），此处直接按 wizardPage 渲染。
            when (wizardPage) {
                Page.RiskWarning -> { /* unused in current flow */ }
                Page.ShizukuCheck -> ShizukuCheckPage(
                    shizukuManager = app.shizukuManager,
                    onReady = {
                        // 本页现只由「主页点开始 Root 引导 + Shizuku 未就绪」这一条路径进入，
                        // 而该路径必置 startFromMainButton=true ⇒ 授权完成后继续 wizard 进 ModelSelect。
                        // else（回主页）保留作防御：万一将来新增入站路径忘了置标记，也不至于把用户
                        // 无故留在向导里。
                        if (startFromMainButton) {
                            startFromMainButton = false
                            wizardPage = Page.ModelSelect
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    deviceInfo = app.rootExecutor.getDeviceInfo()
                                    app.payloadManager.extractPayloads()
                                    payloads = app.payloadManager.getPayloadList()
                                }
                                // 不再支持重启续跑：每次进入引导都从机型选择重新开始
                                wizardPage = Page.ModelSelect
                            }
                        } else {
                            backToHome()
                        }
                    },
                    onBack = {
                        // 用户在 ShizukuCheckPage 点返回：无论来路都回到主页，重置入站标记防止复用
                        startFromMainButton = false
                        backToHome()
                    },
                )

                Page.ModelSelect -> ModelSelectPage(
                    deviceInfo = deviceInfo,
                    payloads = payloads,
                    shizukuManager = app.shizukuManager,
                    payloadManager = app.payloadManager,
                    variant = execVariant,
                    onPayloadSelected = { entry -> startExecuting(entry, customKsudFile = globalCustomKsudFile, variant = execVariant) },
                    onCustomSelected = { info ->
                        // 自选外部文件：构造自定义 PayloadEntry，isCustom=true 跳过 SHA256 校验；
                        // 固件校验也在 startExecuting 内部对自定义条目跳过（board/firmware 由用户填写）。
                        // 优先用 info.ksudFile（用户本次 SAF 选带的 ksud），没有则用 globalCustomKsudFile（预选场景）
                        val entry = com.samsung.rootkit.data.PayloadManager.customEntry(
                            key = info.payloadFile.name.removeSuffix("__payload"),
                            board = info.board,
                            firmware = info.firmware,
                            payloadFile = info.payloadFile,
                            rootFile = info.rootFile
                        )
                        startExecuting(
                            entry,
                            overrideKsudName = info.ksudName,
                            customKsudFile = info.ksudFile ?: globalCustomKsudFile,
                            variant = execVariant,
                        )
                    },
                    onBack = { backToHome() },
                )

                Page.Executing -> ExecutingPage(
                    stage = currentStage,
                    stageMessage = stageMessage,
                    logs = logs,
                    onBack = { backFromExecuting() },
                )

                Page.Result -> ResultPage(
                    success = rootSuccess,
                    logs = logs,
                    // 【测试版】提权耗时（预热结束 → 提权成功），由 RootExecutor 统计
                    rootElapsedMs = app.rootExecutor.rootElapsedMs,
                    onRetry = {
                        logs = emptyList()
                        wizardPage = Page.ModelSelect
                    },
                    onClose = { wizardPage = Page.ModelSelect },
                    onBackHome = { backToHome() },
                    onOpenKsu = {
                        // 按当前变体优先唤起（execVariant 在本作用域可见）
                        val ksuPkg = getInstalledKernelSUPackage(context, execVariant)
                        if (ksuPkg != null) {
                            try {
                                app.shizukuManager.execShell("am start -n $ksuPkg/.ui.MainActivity")
                            } catch (_: Exception) {}
                        }
                    },
                )
            }
        }
    } // 内层 when (screen) 关闭（Wizard / Disclaimer 页面选择）
    } // GlassSkyProvider（else 分支的玻璃背景容器）关闭

    } // else -> 分支（Wizard / Disclaimer）关闭
    } // when (screen) 关闭
    } // CompositionLocalProvider(LocalGlassEnabled) 关闭 —— 覆盖 Main+Wizard 全屏
    } // CompositionLocalProvider(LocalStrings) 关闭

    // ═══════════════════════════════════════════════════════════════════
    // ★ 弹窗区（自 else -> 分支整体迁出，2026-09-26）
    //   原因：此前所有弹窗都写在 when(screen) 的 else 分支内 —— screen==Main
    //   （主页/设置/运行记录）时该分支不组合，状态置 true 也不渲染：
    //   设置页点某操作无反应；直到切到 Wizard 屏（如点 KSU 入口）
    //   弹窗才突然出现。迁到函数顶层后任何 screen 下都能正常弹出。
    // ═══════════════════════════════════════════════════════════════════
    // ── 执行中取消确认对话框 ──
    if (showExecCancelDialog) {
        GlassAlertDialog(
            onDismissRequest = { showExecCancelDialog = false },
            title = { Text(strings.execCancelDialogTitle) },
            text = { Text(strings.execCancelDialogBody) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    GlassPillButton(
                        text = strings.execCancelDialogAbort,
                        onClick = {
                                showExecCancelDialog = false
                                // 仍然取消：仅回退到 ModelSelect（不取消 RootExecutor 协程，
                                // 原因：executeRoot 内部含 push/chmod/exploit/late-load 多阶段，
                                // 在内核态运行中强行中断极易让设备处于不一致状态。
                                // 让协程继续在后台跑完直到 DONE/FAILED，用户切回时会看到结果页。
                                wizardPage = Page.ModelSelect
                        },
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )
                    GlassPillButton(
                        text = strings.execCancelDialogContinue,
                        onClick = { showExecCancelDialog = false },
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }

    // ── 固件不匹配确认对话框 ──
    // 与 run_root.sh 第 293-297 行对齐：脚本在固件不匹配时 read -r -p "force continue? (type YES)"
    // 不输入 YES 则 die "cancelled"。App 之前缺少此交互，静默继续会导致不匹配固件强行 root 失败。
    if (showFirmwareMismatchDialog && pendingFirmwareEntry != null) {
        val entry = pendingFirmwareEntry!!
        GlassAlertDialog(
            onDismissRequest = {
                showFirmwareMismatchDialog = false
                pendingFirmwareEntry = null
                wizardPage = Page.ModelSelect
            },
            title = { Text(strings.firmwareMismatchTitle) },
            text = {
                Text(
                    strings.firmwareMismatchBody(
                        entry.firmware,
                        deviceInfo?.fingerprint ?: "unknown"
                    )
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    GlassPillButton(
                        text = strings.firmwareMismatchCancel,
                        onClick = {
                                showFirmwareMismatchDialog = false
                                pendingFirmwareEntry = null
                                wizardPage = Page.ModelSelect
                        },
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )
                    GlassPillButton(
                        text = strings.firmwareMismatchForce,
                        onClick = {
                                showFirmwareMismatchDialog = false
                                pendingFirmwareEntry = null
                                // 用户选择强制继续（等价于脚本中输入 YES）
                                startExecuting(entry, forceFirmware = true, variant = execVariant)
                        },
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }

    // ── ksud 文件缺失兜底对话框 ──
    // 触发条件:executeRoot 抛异常且异常信息表明是 ksud 文件不存在（内置被删/外部丢失）。
    // 用户从 SAF 选择一个新的 ksud 文件后,会通过 ksudFallbackLauncher 复制到 cacheDir 并重试。
    // ══════════════════════════════════════════════════════════════════════
    // DFReroot 持久化集成（v5.1.0）—— 四个弹窗
    //
    // 流程与硬约束（改顺序前先读）：
    //   ① 注入证书（必须 root：改 /data/system/packages.xml）
    //   ② 软重启     （PMS 只在启动时读 packages.xml，不重启则注入不生效）
    //   ③ 安装 APK   （必须在 ② 之后，否则 INSTALL_FAILED_SHARED_USER_INCOMPATIBLE）
    //   ④ 卸载       （顺序经确认：先卸 APK 再移密钥，理由见 DfRunner.uninstallDfFully 注释）
    //
    // ②会让本应用进程被系统杀掉，所以 ①→③ 之间必须靠 SharedPreferences 的
    // "待安装"标记接力（内存态必丢）。
    // ══════════════════════════════════════════════════════════════════════

    // ⓪ 机型不受支持（S24 系列）「暂不适配」纯提示 —— 无任何后续动作，确定即关闭。
    //   2026-10-02 F8 修复：09-26 版该弹窗仅挂在主页 DF 入口，随入口迁往设置页后被删除；
    //   现恢复为**设置页 DF 全入口共用**（「打开 DFReroot」卡片点击 与「配置 DFReroot」分支）。
    //   主页两个按钮在不受支持机型上不走本弹窗 —— 它们回退为「开始 Root 引导」（见 HomePage
    //   的 dfEntryUsable 门控），语义是"该机型不提供 DF 入口"，与设置页提示互补。
    //   文案复用既有 dfUnsupportedTitle/Body/Confirm 三键（此前已成为未被引用的死键）。
    if (showDfUnsupportedDialog) {
        GlassAlertDialog(
            onDismissRequest = { showDfUnsupportedDialog = false },
            title = { Text(strings.dfUnsupportedTitle) },
            text = {
                Text(strings.dfUnsupportedBody, style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    GlassPillButton(
                        text = strings.dfUnsupportedConfirm,
                        onClick = { showDfUnsupportedDialog = false },
                        isPrimary = false,
                        modifier = Modifier.widthIn(min = 148.dp),
                    )
                }
            },
        )
    }

    // ① 启用确认 —— 两个触发源：
    //    a) root 成功后自动弹出（主动引导）
    //    b) 首页第三个入口未配置时点击（灰色但可点，修复②：点击弹出本配置弹窗）
    if (showDfEnableDialog) {
        GlassAlertDialog(
            onDismissRequest = { if (!dfBusy) showDfEnableDialog = false },
            title = { Text(strings.dfEnableTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.dfEnableBody, style = MaterialTheme.typography.bodyMedium)
                    if (dfBusy) {
                        Text("正在注入…", style = MaterialTheme.typography.bodySmall)
                    }
                    if (dfOutput.isNotEmpty()) {
                        Text(
                            dfOutput,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    GlassPillButton(
                        text = strings.dfEnableCancel,
                        onClick = { showDfEnableDialog = false },
                        isPrimary = false,
                        enabled = !dfBusy,
                        modifier = Modifier.weight(1f),
                    )
                    GlassPillButton(
                        text = strings.dfEnableConfirm,
                        onClick = {
                                dfBusy = true
                                dfOutput = ""
                                scope.launch(Dispatchers.IO) {
                                    // 修复③：**先预检 root，再谈注入**。
                                    // 不预检就硬跑，没授权时只会得到一句生硬的
                                    // "su 执行失败: IOException: Cannot run program su"（真机截图实证）。
                                    //   ① hasRootAccess()：本应用此刻能否提权（首次探测会触发管理器的授权询问）
                                    //   ② resolveWorkingSu()：可用 su 的绝对路径；null = 没有 su 或未被授权
                                    // 任一不成立 → 不注入，直接换「需要 root 权限」引导弹窗
                                    // （showDfSuDialog："继续"按钮跳当前生效那支的管理器）。
                                    val rootOk = try {
                                        app.rootExecutor.hasRootAccess()
                                    } catch (_: Exception) {
                                        false
                                    }
                                    val su = if (rootOk) app.rootExecutor.resolveWorkingSu() else null
                                    if (su == null) {
                                        // 记下当前生效的管理器变体，供引导弹窗跳转；
                                        // 未持权时可能判不出（null）→ 弹窗内回退"已安装优先"。
                                        dfMgrVariant = try {
                                            app.rootExecutor.resolveWorkingVariant()
                                        } catch (_: Exception) {
                                            null
                                        }
                                        dfBusy = false
                                        dfOutput = ""
                                        showDfEnableDialog = false
                                        showDfSuDialog = true
                                        return@launch
                                    }
                                    // 修复④：先查密钥是否已在 packages.xml 里（DfInject --check，零写入）。
                                    // 5.1.0 时代注入过一次的设备，再硬跑默认注入会撞
                                    // "already injected into android.uid.system"
                                    // （PackagesXml.buildPatchedXml L166-175 的防重复保护，真机截图实证）。
                                    // 已注入 → 跳过注入，直接进软重启确认（装 DFReroot 前必须软重启）。
                                    val chk = DfRunner.checkInjected(context, su)
                                    if (chk.ok && chk.output.contains("all_injected=true")) {
                                        SettingsStore.setDfPendingInstall(context, true)
                                        dfBusy = false
                                        dfOutput = ""
                                        showDfEnableDialog = false
                                        showDfSoftRebootDialog = true
                                        return@launch
                                    }
                                    // 运行记录 begin（DFE 源）。
                                    // 位置刻意放在**两次预检之后、真正注入之前**：上面那些
                                    // 「没 su」「已注入过」的分支都是守卫出口，不是一次运行。
                                    val attempt = RunLogStore.begin(
                                        context,
                                        RunSource.DFE,
                                        RunLogStore.defaultModel(),
                                        RunLogStore.defaultFirmware(),
                                    )
                                    val r = DfRunner.injectCert(context, su)
                                    dfOutput = r.output
                                    dfBusy = false
                                    // 「已注入」在预检/注入窗口期竞态下也会从这里出来（见下方 when 分支），
                                    // 那种结果等价于成功，记录里不能记成失败。
                                    val injectOk = r.ok || r.output.contains("already injected")
                                    RunLogStore.finish(
                                        context,
                                        attempt,
                                        if (injectOk) RunOutcome.SUCCESS else RunOutcome.FAILED,
                                        r.summary,
                                        r.output.split("\n"),
                                    )
                                    when {
                                        r.ok -> {
                                            // 落盘"待安装"标记：下一步软重启会杀掉本进程，内存态留不住
                                            SettingsStore.setDfPendingInstall(context, true)
                                            showDfEnableDialog = false
                                            showDfSoftRebootDialog = true
                                        }
                                        // 预检过了但注入时 su 失效（竞态兜底）→ 仍走"去管理器授权"引导
                                        r.suMissing -> {
                                            showDfEnableDialog = false
                                            showDfSuDialog = true
                                        }
                                        // 兜底：预检时还没注入、执行时撞上"already injected"
                                        // （预检/注入窗口期密钥被写入的竞态）→ 按"已注入"处理，不当失败
                                        r.output.contains("already injected") -> {
                                            SettingsStore.setDfPendingInstall(context, true)
                                            showDfEnableDialog = false
                                            showDfSoftRebootDialog = true
                                        }
                                        else -> android.widget.Toast.makeText(
                                            context,
                                            "${strings.dfErrorPrefix}: ${r.summary}",
                                            android.widget.Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                }
                        },
                        isPrimary = false,
                        enabled = !dfBusy,
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }

    // ② 软重启确认（D6：保留手动确认，软件重启会黑屏数秒）
    if (showDfSoftRebootDialog) {
        GlassAlertDialog(
            onDismissRequest = { if (!dfBusy) showDfSoftRebootDialog = false },
            title = { Text(strings.dfSoftRebootTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.dfInjectOk, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        strings.dfSoftRebootBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    )
                    if (dfOutput.isNotEmpty()) {
                        Text(
                            dfOutput,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    GlassPillButton(
                        text = strings.commonCancel,
                        onClick = { showDfSoftRebootDialog = false },
                        isPrimary = false,
                        enabled = !dfBusy,
                        modifier = Modifier.weight(1f),
                    )
                    GlassPillButton(
                        text = strings.dfSoftRebootConfirm,
                        onClick = {
                                dfBusy = true
                                scope.launch {
                                    // 本进程随后会被系统一并杀掉，无需等待/判断返回值。
                                    // 待安装标记已在 ① 落盘，重进后由 LaunchedEffect 接力。
                                    val su = app.rootExecutor.resolveWorkingSu()
                                    // 2026-10-08：改用带候选兜底的版本，避免单点探测落空就误判「没有 su」。
                                    val r = DfRunner.softRebootRobust(su)
                                    dfBusy = false
                                    if (!r.ok) {
                                        // 软重启也要 root；没 su 同样给引导
                                        if (r.suMissing) {
                                            showDfSoftRebootDialog = false
                                            showDfSuDialog = true
                                        } else {
                                            dfOutput = r.output
                                        }
                                    }
                                }
                        },
                        isPrimary = false,
                        enabled = !dfBusy,
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }

    // ③ 安装 DFReroot（软重启后重进 App 时弹出，由"待安装"标记接力）
    if (showDfInstallDialog) {
        GlassAlertDialog(
            onDismissRequest = { if (!dfBusy) showDfInstallDialog = false },
            title = { Text(strings.dfInstallTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.dfInstallBody, style = MaterialTheme.typography.bodyMedium)
                    if (dfBusy) {
                        Text("正在安装…", style = MaterialTheme.typography.bodySmall)
                    }
                    if (dfOutput.isNotEmpty()) {
                        Text(
                            dfOutput,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    GlassPillButton(
                        text = strings.commonCancel,
                        onClick = { showDfInstallDialog = false },
                        isPrimary = false,
                        enabled = !dfBusy,
                        modifier = Modifier.weight(1f),
                    )
                    GlassPillButton(
                        text = strings.commonConfirm,
                        onClick = { runDfInstallNow() },
                        isPrimary = false,
                        enabled = !dfBusy,
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }

    // ④ 卸载 DFReroot（设置页入口）
    //    流程（DfRunner.uninstallDfFully）：pm uninstall → 等 PMS 落盘 → DfInject --uninstall
    //    → --check 复核（被 PMS 写回则自动重试，最多 3 轮）。
    //    若第一步失败会立即返回，绝不在"APK 还在"的情况下继续移密钥。
    //    v5.1.2 真机踩坑：卸载后密钥"复活"——PMS 异步 writeSettings 竞态，见 DfRunner 注释。
    if (showDfUninstallDialog) {
        // 关闭弹窗统一入口：顺带复位「完成态」标记（否则下次打开会直接显示完成态）。
        val closeDfUninstallDialog = {
            if (!dfBusy) {
                showDfUninstallDialog = false
                dfUninstallCompleted = false
            }
        }
        if (dfUninstallCompleted) {
            // ── 完成态（2026-10-04 修复；2026-10-05 修好「进不来」的根因）──
            // 进入本分支的判据是 **DfUninstallReport.done（事实）**，见其 KDoc：
            // 原先用「每一步都 ok」会让「重试轮里已被覆盖的失败」与「best-effort 的残留清理」
            // 把完成态永久挡住 —— 用户看到「已卸载」的输出却仍是「卸载」按钮，就是这个 bug。
            // 已卸载完成 ⇒ 不再显示「卸载」（语义错误、且再点无事可做），
            // 改为「取消 / 软重启」两个**等宽对称**按钮：
            //   · 左侧「取消」  = 关弹窗（不重启）
            //   · 右侧「软重启」= 让 packages.xml 的证书移除完全生效（PMS 只在开机读该文件）
            // 对称实现：confirmButton 内自建 `Row(fillMaxWidth)`，两个 TextButton 各 `weight(1f)`
            // ⇒ 等宽、左右分列。（**不能**靠 dismissButton + confirmButton 分列：见下方注释，
            // 那两者会被塞进内部的 AlertDialogFlowRow 并靠右紧贴。）
            GlassAlertDialog(
                onDismissRequest = closeDfUninstallDialog,
                title = { Text(strings.dfUninstallDoneTitle) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.dfUninstallDone, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            strings.dfUninstallDoneHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        )
                        if (dfOutput.isNotEmpty()) {
                            Text(
                                dfOutput,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                    }
                },
                // ⚠️ 为什么只用 confirmButton 而不写 dismissButton：
                // Material3 的 AlertDialog **没有** `buttons=` 参数（那是 Material2 的旧签名），
                // dismissButton + confirmButton 都会被塞进内部的 AlertDialogFlowRow 并**靠右紧贴**，
                // 正是用户截图里「两个按钮挤在一起」的成因。
                // 故此处**只提供 confirmButton**，在其内部自行 Row 布局实现左右对称（weight 等宽）。
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        GlassPillButton(
                            text = strings.commonCancel,
                            onClick = closeDfUninstallDialog,
                            isPrimary = false,
                            modifier = Modifier.weight(1f),
                        )
                        GlassPillButton(
                            text = strings.dfSoftRebootConfirm,
                            onClick = {
                                    dfBusy = true
                                    scope.launch {
                                        val su = app.rootExecutor.resolveWorkingSu()
                                        // 2026-10-08：改为带候选兜底的软重启。
                                        // 根因是卸载里的「残留清理」曾把 /data/adb/ksud（su 入口所经）删掉，
                                        // 使这里探测不到 su 而被误判 suMissing、甩到「去管理器授权」弹窗；
                                        // 清理已修（DfRunner.uninstallAutostartResidue），此处再兜一层。
                                        val r = DfRunner.softRebootRobust(su)
                                        dfBusy = false
                                        if (!r.ok) {
                                            if (r.suMissing) {
                                                // 软重启需要 root：确实没有可用 su 才给引导
                                                showDfUninstallDialog = false
                                                dfUninstallCompleted = false
                                                showDfSuDialog = true
                                            } else {
                                                dfOutput = r.output
                                            }
                                        }
                                        // 成功时本进程会被一并杀掉，无需再关弹窗
                                    }
                            },
                            isPrimary = false,
                            enabled = !dfBusy,
                            modifier = Modifier.weight(1f),
                        )
                    }
                },
            )
        } else {
            // ── 卸载前（确认态）：保持原「取消 / 卸载」布局 ──
            GlassAlertDialog(
                onDismissRequest = closeDfUninstallDialog,
                title = { Text(strings.dfUninstallTitle) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.dfUninstallBody, style = MaterialTheme.typography.bodyMedium)
                        if (dfBusy) {
                            Text("正在卸载…", style = MaterialTheme.typography.bodySmall)
                        }
                        if (dfOutput.isNotEmpty()) {
                            Text(
                                dfOutput,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        GlassPillButton(
                            text = strings.commonCancel,
                            onClick = closeDfUninstallDialog,
                            isPrimary = false,
                            enabled = !dfBusy,
                            modifier = Modifier.weight(1f),
                        )
                        GlassPillButton(
                            text = strings.dfUninstallConfirm,
                            onClick = {
                                    dfBusy = true
                                    dfOutput = ""
                                    scope.launch {
                                        // 预检 A（修复⑦）：没有可用 su → 不硬跑，直接引导去管理器授权
                                        //（与「注入并继续」的预检同一出口 showDfSuDialog）。
                                        val su = app.rootExecutor.resolveWorkingSu()
                                        if (su == null) {
                                            dfBusy = false
                                            showDfUninstallDialog = false
                                            dfMgrVariant = try {
                                                app.rootExecutor.resolveWorkingVariant()
                                            } catch (_: Exception) { null }
                                            showDfSuDialog = true
                                            return@launch
                                        }
                                        // 预检 B（修复⑦）：DF 未装且密钥也不在 → 无事可做，
                                        // 提示后关弹窗并刷新状态行（顺带把 dfKeyInjected 修正过来）。
                                        val chk = DfRunner.checkInjected(context, su)
                                        val keyIn = chk.ok && chk.output.contains("all_injected=true")
                                        if (!DfBridge.isDfInstalled(context) && !keyIn) {
                                            dfBusy = false
                                            showDfUninstallDialog = false
                                            android.widget.Toast.makeText(
                                                context, strings.dfUninstallNothing,
                                                android.widget.Toast.LENGTH_LONG,
                                            ).show()
                                            refreshDfState()
                                            return@launch
                                        }
                                        // 运行记录 begin（DFE 源）：预检（无 su / 无需卸载）都已在上面返回，
                                        // 到这里才是真的开始卸载。
                                        val attempt = RunLogStore.begin(
                                            context,
                                            RunSource.DFE,
                                            RunLogStore.defaultModel(),
                                            RunLogStore.defaultFirmware(),
                                        )
                                        val report = DfRunner.uninstallDfFully(context, su)
                                        android.util.Log.d(
                                            "DFUNINSTALL",
                                            "confirm: su=$su keyIn=$keyIn isDf=${DfBridge.isDfInstalled(context)} steps=${report.steps.size} done=${report.done}",
                                        )
                                        dfOutput = report.steps.joinToString("\n") {
                                            (if (it.ok) "[OK] " else "[X] ") + it.name + "：" + it.cmd.summary
                                        }
                                        // ★ 完成判据 = **事实**（DFReroot 已不在 && 密钥已移除），
                                        //   不再用「每一步都 ok」—— 后者会被①重试轮里已被后续成功覆盖的
                                        //   中间失败、②best-effort 的残留清理步骤拉成 false，导致弹窗永远
                                        //   停在确认态（按钮仍是「卸载」）。详见 DfUninstallReport 注释。
                                        val done = report.done
                                        RunLogStore.finish(
                                            context,
                                            attempt,
                                            if (done) RunOutcome.SUCCESS else RunOutcome.FAILED,
                                            report.failureDetail,
                                            dfOutput.split("\n"),
                                        )
                                        if (done) {
                                            SettingsStore.setDfPendingInstall(context, false)
                                        }
                                        refreshDfState()
                                        dfBusy = false
                                        if (done) {
                                            android.widget.Toast.makeText(
                                                context, strings.dfUninstallDone, android.widget.Toast.LENGTH_LONG,
                                            ).show()
                                            // 2026-10-04：不再直接关弹窗，转为「完成态」，
                                            // 让用户顺手软重启（否则密钥移除要等下次开机才完全生效）。
                                            dfUninstallCompleted = true
                                        } else if (report.steps.any { it.cmd.suMissing }) {
                                            showDfUninstallDialog = false
                                            showDfSuDialog = true
                                        }
                                    }
                            },
                            isPrimary = false,
                            enabled = !dfBusy,
                            modifier = Modifier.weight(1f),
                        )
                    }
                },
            )
        }
    }

    // ④′ df（DFRoot）跑链进度**弹窗已取消**（2026-10-05 需求①）。
    //     进度不再盖住整页，改为显示在主页红蓝按钮下方那张卡里（跑链期间该卡临时让位，
    //     显示实时日志；跑完自动恢复设备信息）—— 用户定调「操作时在卡内动态显示，
    //     不操作时保持现样」。链跑分钟级仍需可见反馈这一点没变，只是换了容器：
    //     用弹窗时用户既看不到按钮状态，也没法边看日志边操作。
    //     结局由 Toast 交代（日志随卡恢复而消失），见 runDfRootChain。

    // ④‴ df（DFRoot）「本次开机已注入」解释弹窗（2026-10-04 P3 / S5）——
    //     round18 随「按钮压灰」一起废除：已注入态按钮不再压灰，改为显示当前工作
    //     管理器 / 点另一个直接切换（下次开机生效），无需解释弹窗。

    // ⑥ su 不可用 / 未授权 → 引导去管理器授予 root。
    //    两个触发源：
    //    a) 点「注入并继续」时的**预检**发现没有可用 root（修复③：先检后做，不再硬跑报错）
    //    b) 预检通过但注入瞬间 su 失效的竞态兜底
    //    KernelSU 的 su 在 /data/adb/ksu/bin/su（不在 app 的 PATH 里），或本应用尚未被授权。
    if (showVariantSwitchDialog) {
        // 2026-10-02 F1 修复：原先标题/正文/按钮全部硬编码中文，英文环境下仍显示中文；
        // 现全部走 AppStrings（zh/en），与项目其它弹窗一致。
        // 变体名同样取多语言资源（ksuVariantNext / ksuVariantKsu），不再写字面量。
        // round19：弹窗同时服务 dfe / df 两路入口（pendingSwitchIsDfRoot 区分）。
        // ⚠️ 2026-10-05：**正文必须按来源分开** —— dfe 的动作是「跳转 DFReroot 并由它写 ksud」，
        //    df 的动作是「本进程内跑链」，两者末句不同（原先共用 dfe 那句，df 入口会
        //    谎称"将跳转 DFReroot"，用户已报障）。标题/按钮/目标变体名仍共用。
        val targetLabel = if (pendingSwitchVariant == PayloadManager.KsuVariant.KSU_NEXT)
            strings.ksuVariantNext else strings.ksuVariantKsu
        GlassAlertDialog(
            onDismissRequest = { showVariantSwitchDialog = false },
            title = { Text(strings.dfVariantSwitchTitle) },
            text = {
                Text(
                    if (pendingSwitchIsDfRoot) strings.dfRootVariantSwitchBody(targetLabel)
                    else strings.dfVariantSwitchBody(targetLabel),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    GlassPillButton(
                        text = strings.dfVariantSwitchCancel,
                        onClick = { showVariantSwitchDialog = false },
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )
                    GlassPillButton(
                        text = strings.dfVariantSwitchConfirm,
                        onClick = {
                                showVariantSwitchDialog = false
                                if (pendingSwitchIsDfRoot) {
                                    // df（DFRoot）：记变体（本应用 DE prefs `ksu_next`，自启与跑链共用，
                                    // 下次开机自动 Root 生效）+ 继续本次跑链。
                                    setDfRootVariant(pendingSwitchVariant)
                                    runDfRootChain(pendingSwitchVariant)
                                } else {
                                    // dfe（DFReroot）：写 SRK 持久化变体 + 内联跑链。
                                    // 2026-10-07 需求①：本路受「已 root 则不写运行记录」约束 ——
                                    // 判据取自打开弹窗时的快照 [pendingSwitchSystemRooted]。
                                    // ★ 2026-10-08（用户定版）：本弹窗及其确认动作**保持 2026-10-07
                                    //   原样不动**（用户明确要求）；主页按钮的「已 root 小提示」
                                    //   调整不涉及此处 —— 切换窗只在未走过的变体路出现，确认后
                                    //   仍按原样内联执行。
                                    openDfWithVariant(
                                        pendingSwitchVariant,
                                        recordRunLog = !pendingSwitchSystemRooted,
                                    )
                                }
                        },
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }

    // ⑧ df（DFRoot）「系统已 root，无需重复操作」提示（2026-10-05 需求④）。
    //    df 模式下点主页红/蓝按钮、且未走到「切换下次开机管理器」那一路时出现。
    //    ★ 2026-10-08：dfe 主页按钮在已 root 时也复用本弹窗（用户定版：不跳 DFE UI、
    //    不内联，就提示这一句；文案两者通用，无需新增字符串）。
    //    为什么不用 Toast：这是**阻断一次用户主动动作**的反馈（点击被"消化"了），
    //    必须有明确的标题 + 正文 + 关闭按钮，避免用户以为按钮失灵又连点。
    if (showDfRootAlreadyRootedDialog) {
        GlassAlertDialog(
            onDismissRequest = { showDfRootAlreadyRootedDialog = false },
            title = { Text(strings.dfRootAlreadyRootedTitle) },
            text = {
                Text(
                    strings.dfRootAlreadyRootedBody,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    GlassPillButton(
                        text = strings.dfRootRunClose,
                        onClick = { showDfRootAlreadyRootedDialog = false },
                        isPrimary = false,
                        modifier = Modifier.widthIn(min = 148.dp),
                    )
                }
            },
        )
    }

    // ⑨ df（DFRoot）「当前系统版本不支持」——设置页总开关被失败记录拦下（2026-10-05 需求②）。
    //    为什么要有这个弹窗：开关被拦时用户只看到「拨了又自己弹回去」，没有解释会以为是
    //    开关坏了；这里明确告知原因（本固件上 df 已连续多次提权失败）与解除条件（系统升级）。
    //    ★ 2026-10-08：本弹窗只会在**已达阈值**（连续 3 次）后出现 ——
    //      拦截判据是 DfRootFailureRecordStore.read()，它自带阈值。第 1、2 次失败
    //      总开关照常可拨，用户不会看到任何拦截。
    //    正文里那句「系统升级后会自动重新启用」是**事实**，不是安慰：见 refreshDfRootState
    //    的陈旧记录清理（固件一变即清记录 + 自动把总开关写回 true）。
    if (showDfRootUnsupportedDialog) {
        GlassAlertDialog(
            onDismissRequest = { showDfRootUnsupportedDialog = false },
            title = { Text(strings.dfRootUnsupportedTitle) },
            text = {
                // 只有**已达阈值**才会走到本弹窗（setDfRootEnabled 的判据是 read()，
                // 而 read() 自带「固件一致 && 连续失败 ≥ FAIL_THRESHOLD」）。
                // 阈值作为参数传入，保证文案里的数字与常量同源、不会漂移。
                Text(
                    strings.dfRootUnsupportedBody(DfRootFailureRecord.FAIL_THRESHOLD),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    GlassPillButton(
                        text = strings.dfRootRunClose,
                        onClick = { showDfRootUnsupportedDialog = false },
                        isPrimary = false,
                        modifier = Modifier.widthIn(min = 148.dp),
                    )
                }
            },
        )
    }

    // ⑩ df（DFRoot）跑链失败 → 引导进 SRK 的 Root 流程（2026-10-05 需求②）。
    //    到达这里时：失败次数已落盘（DfRootManager.runChain → handleChainFailure 内完成，
    //    本弹窗不重复做）。**是否已关总开关取决于是否达阈值**：
    //      · 第 1、2 次失败 ⇒ 总开关**没关**，用户可直接重试（文案走 retry 版）；
    //      · 第 3 次失败   ⇒ 已记录版本 + 已关总开关（文案走 fallback 版）。
    //    确认 ⇒ 进 SRK 自己的向导（payload root 模式）；取消 ⇒ 留在主页。
    //    未达阈值时取消后主页按钮仍走 df 路（总开关还开着），与 2026-10-03 设计不冲突。
    if (showDfRootFailedFallbackDialog) {
        GlassAlertDialog(
            onDismissRequest = { showDfRootFailedFallbackDialog = false },
            title = { Text(strings.dfRootFailedFallbackTitle) },
            text = {
                // ★ 2026-10-08 两态文案（务必分开，见 AppStrings 里两个字段的 KDoc）：
                //   · 已达阈值（连续 ≥3 次）⇒ 如实说「已记录该版本并已关闭总开关」；
                //   · 未达阈值（第 1、2 次）⇒ 只说「第 N 次、满 3 次才会关」。
                //     绝不能沿用前者 —— 那种情况总开关**根本没关**，说了就是骗用户。
                // 判据用 strikes（而非 failureRecord）：后者由 read() 产出、只在达阈值时非 null，
                // 而这里还要区分「未达阈值」这一态。strikes 为 0 只可能是计数没能落盘
                // （见 handleChainFailure 的 fail-open），此时按 1 次显示 —— 至少真失败过这一次。
                Text(
                    if (dfRootFailureStrikes >= DfRootFailureRecord.FAIL_THRESHOLD) {
                        strings.dfRootFailedFallbackBody(DfRootFailureRecord.FAIL_THRESHOLD)
                    } else {
                        strings.dfRootFailedRetryBody(
                            dfRootFailureStrikes.coerceAtLeast(1),
                            DfRootFailureRecord.FAIL_THRESHOLD,
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    GlassPillButton(
                        text = strings.commonCancel,
                        onClick = { showDfRootFailedFallbackDialog = false },
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )
                    GlassPillButton(
                        text = strings.dfRootFailedFallbackConfirm,
                        onClick = {
                                showDfRootFailedFallbackDialog = false
                                val variant = dfRootFallbackVariant
                                // Shizuku 就绪与否决定向导首屏：就绪 ⇒ 直接选机型；未就绪 ⇒ 先引导授权。
                                // 这里自己探一次而不是吃页面缓存 —— 与 HomePage 的判据同源
                                // （shizukuRunning && shizukuAuthorized），且本弹窗可能在任意时刻被确认。
                                scope.launch {
                                    val ready = withContext(Dispatchers.IO) {
                                        try {
                                            app.shizukuManager.isShizukuRunning() &&
                                                app.shizukuManager.hasPermission()
                                        } catch (_: Exception) {
                                            false
                                        }
                                    }
                                    startWizard(shizukuReady = ready, variant = variant)
                                }
                        },
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }

    if (showDfSuDialog) {
        GlassAlertDialog(
            onDismissRequest = { if (!dfBusy) showDfSuDialog = false },
            title = { Text(strings.dfSuNeededTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.dfSuNeededBody, style = MaterialTheme.typography.bodyMedium)
                    if (dfOutput.isNotEmpty()) {
                        Text(
                            dfOutput,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    GlassPillButton(
                        text = strings.dfSuNeededRetry,
                        onClick = { showDfSuDialog = false },
                        isPrimary = false,
                        enabled = !dfBusy,
                        modifier = Modifier.weight(1f),
                    )
                    GlassPillButton(
                        text = strings.dfSuNeededOpenManager,
                        onClick = {
                                // 跳到**当前生效那一支**的管理器（预检时已探测，见修复③）；
                                // 判不出时回退"已安装优先"；一个都没装则 Toast 说明，不再静默无效。
                                val pkg = getInstalledKernelSUPackage(context, preferredVariant = dfMgrVariant)
                                if (pkg != null) {
                                    context.packageManager.getLaunchIntentForPackage(pkg)
                                        ?.let { context.startActivity(it) }
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "未找到已安装的 KernelSU 管理器",
                                        android.widget.Toast.LENGTH_LONG,
                                    ).show()
                                }
                        },
                        isPrimary = false,
                        enabled = !dfBusy,
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }

    if (showKsudMissingDialog) {
        GlassAlertDialog(
            onDismissRequest = {
                showKsudMissingDialog = false
                pendingKsudEntry = null
                clearRootCache()
            },
            title = { Text("缺少 ksud 文件") },
            text = {
                Text(
                    "执行时检测到 ksud 文件缺失,请手动选择一个 ksud 文件后重试。\n" +
                    "提示:ksud 文件通常文件名包含 \"ksud\",内核版本需要与设备匹配（5.15/6.1/6.6 等）。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    GlassPillButton(
                        text = "取消",
                        onClick = {
                                showKsudMissingDialog = false
                                pendingKsudEntry = null
                                clearRootCache()
                        },
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )
                    GlassPillButton(
                        text = "选择 ksud",
                        onClick = {
                                ksudFallbackLauncher.launch(arrayOf("*/*"))
                        },
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }

    // ── ksud 不适配对话框 ──
    // 触发条件:用户选的外部 ksud 文件名版本号与设备内核主版本不匹配(如选了 5.15 的 ksud 给 6.1 设备)。
    // 重新选择时走 ksudFallbackLauncher,保留 mismatchKsudEntry 以便适配后继续重试。
    if (showKsudMismatchDialog) {
        GlassAlertDialog(
            onDismissRequest = {
                showKsudMismatchDialog = false
                mismatchKsudEntry = null
                clearRootCache()
            },
            title = { Text("ksud 不适配") },
            text = {
                Text(
                    "所选 ksud 文件的内核版本与当前设备不匹配,请重新选择适配的 ksud 文件。\n" +
                    "提示:文件名中的版本号（如 5.15/6.1/6.6）需要与设备内核主版本一致。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    GlassPillButton(
                        text = "取消",
                        onClick = {
                                showKsudMismatchDialog = false
                                mismatchKsudEntry = null
                                pendingKsudEntry = null
                                clearRootCache()
                        },
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )
                    GlassPillButton(
                        text = "重新选择",
                        onClick = {
                                showKsudMismatchDialog = false
                                pendingKsudEntry = mismatchKsudEntry
                                ksudFallbackLauncher.launch(arrayOf("*/*"))
                        },
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }
}

// 改成 enum 让 rememberSaveable 可以直接序列化（Activity 重建后能恢复当前页面状态）
// 切语言会触发 AppCompatDelegate.setApplicationLocales 重建 Activity，必须用 rememberSaveable
// 否则切完语言会跳回 Home tab。
private enum class Screen { Disclaimer, Main, Wizard }

private enum class MainTab { Home, RunLog, Settings }

sealed class Page {
    object RiskWarning : Page()
    object ShizukuCheck : Page()
    object ModelSelect : Page()
    object Executing : Page()
    object Result : Page()
}

data class LogEntry(
    val level: RootExecutor.LogLevel,
    val message: String,
    val timestamp: Long,
)

// ==================== Shared UI Components ====================

private enum class SettingsCardPosition {
    Single,
    GroupedSingle,
    Top,
    Middle,
    Bottom,
}

@Composable
private fun expressiveClickableCardShape(
    interactionSource: MutableInteractionSource,
    position: SettingsCardPosition = SettingsCardPosition.Single,
): RoundedCornerShape {
    val pressed by interactionSource.collectIsPressedAsState()
    val topRadius by animateDpAsState(
        targetValue = when {
            pressed -> 28.dp
            position == SettingsCardPosition.Single -> 16.dp
            position in setOf(SettingsCardPosition.GroupedSingle, SettingsCardPosition.Top) -> 24.dp
            else -> 6.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "clickable-card-top-corner",
    )
    val bottomRadius by animateDpAsState(
        targetValue = when {
            pressed -> 28.dp
            position == SettingsCardPosition.Single -> 16.dp
            position in setOf(SettingsCardPosition.GroupedSingle, SettingsCardPosition.Bottom) -> 24.dp
            else -> 6.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "clickable-card-bottom-corner",
    )
    return RoundedCornerShape(
        topStart = topRadius,
        topEnd = topRadius,
        bottomStart = bottomRadius,
        bottomEnd = bottomRadius,
    )
}

private fun clickHaptic(view: View) {
    view.performHapticFeedback(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.LONG_PRESS
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 18.dp, top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun IconInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Column {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Column {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

/**
 * df 跑链的**卡内实时日志**（2026-10-05 需求①）。
 *
 * <p>取代原先那个盖住整页的「DFRoot 执行」弹窗。存在理由不变 —— 链可能跑分钟级，
 * 没有可见反馈用户会以为卡死并杀进程，而杀进程会打断链、`/dev/df` 的 O_EXCL 可能
 * 已被消费 ⇒ 本次开机再也拿不到 root —— 但**容器**换了：放在主页红蓝按钮正下方的
 * 设备信息卡里，用户点完按钮视线不用移动；也不再有「弹窗挡住按钮 → 关掉后不知道
 * 刚才发生了什么」的问题。
 *
 * <h2>为什么是「固定高度」而不是「上限」</h2>
 * 它生活在 LazyColumn 的一个 item 里。`LazyColumn` 要求 item 有确定高度，
 * 无上限的 `verticalScroll` 会直接崩（infinite constraints）。
 *
 * <p>2026-10-07 修复：此前用 `heightIn(max = 300.dp)`，只给上限带来两个问题 ——
 * ① 300dp 仍偏高（整卡 ≈ 430dp，占去半屏）；② 只封顶不固定，日志少时卡片会随内容
 * 长高，用户观感即「卡片被拉高、日志没在卡内滚动」。现改为 [DF_LOG_VIEWPORT_HEIGHT]
 * 固定值：以 min=max 约束强制视口高度，卡片高度恒定、日志恒在卡内滚动。
 *
 * <h2>为什么每次追加都滚到底</h2>
 * 日志是持续追加的，停在中间 = 看不到最新一步；而用户此刻关心的恰恰是最新一步。
 * 用 `scrollTo`（无动画）而非 `animateScrollTo`：链的日志行是高频小步追加的，
 * 动画会互相打断，看起来像抖动。
 */
@Composable
private fun DfRootLiveLogCard(title: String, running: String, output: String) {
    val scroll = rememberScrollState()
    // 以 output 为 key：这一行只要变了就重新滚到底（同一 key 不会重复触发）。
    LaunchedEffect(output) {
        scroll.scrollTo(scroll.maxValue)
    }
    Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LoadingIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary)
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            running,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // 2026-10-07 修复：原为 heightIn(max = 300.dp)。
                //  · 300dp 上限过大 → 整卡 ≈ 430dp，占去半屏（实测）。
                //  · 且"只封顶、不固定"：内容少时卡片仍随内容长高，观感即「卡片被拉高」。
                // 改为固定高度（height 以 min=max 约束强制，不依赖上限语义），
                // 使卡片高度恒定、日志恒在卡内滚动，与主页设备信息卡（≈230dp）等高对齐。
                .height(DF_LOG_VIEWPORT_HEIGHT)
                .clipToBounds()
                .verticalScroll(scroll),
        ) {
            Text(
                // 首行可能还没来（setup 阶段之前）：留一个占位，避免卡片高度先塌再涨。
                output.ifBlank { "…" },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
        }
    }
}

/**
 * 主页「DFRoot 实时日志」视口的**固定高度**。
 *
 * <p>取值依据（2026-10-07 真机标定）：与主页红蓝按钮正下方那张卡片（设备信息卡，
 * 满信息态）的**整卡高度对齐** —— 实测设备信息卡 ≈ 230dp（截图像素 y[373,625] = 253px
 * ÷ 0.898 dp/px）；减去日志卡头部（图标行 + 提示行 + 分隔线 + 三处 10dp 间距 ≈ 85dp）
 * 与上下 padding（2 × 18dp），得日志视口 ≈ 130dp。
 *
 * <p>调整日志可见行数只需改此一处（整卡高度 ≈ 此值 + 103dp）。
 */
private val DF_LOG_VIEWPORT_HEIGHT = 130.dp

@Composable
private fun IconCircle(icon: ImageVector, containerColor: Color, contentColor: Color, size: Int = 36) {
    Surface(
        modifier = Modifier.size(size.dp),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size((size * 0.55).dp))
        }
    }
}

private fun pageHeaderIconCircle(icon: ImageVector) = @Composable {
    Surface(
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

// ==================== 1. Risk Warning Page ====================
@Composable
fun RiskWarningPage(
    onConfirm: () -> Unit,
    onCancel: () -> Unit = {},
    showBackButton: Boolean = false,
) {
    val strings = LocalStrings.current
    val view = LocalView.current
    val scrollState = rememberScrollState()
    val reachedBottom by remember {
        derivedStateOf { scrollState.maxValue == 0 || scrollState.value >= scrollState.maxValue - 10 }
    }

    val readSeconds = 12
    var countdown by remember { mutableStateOf(readSeconds) }
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    var checkboxRead by remember { mutableStateOf(false) }
    var checkboxAgree by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf(randomProblem()) }
    var answer by remember { mutableStateOf("") }
    var mathError by remember { mutableStateOf<String?>(null) }

    val allReady = countdown <= 0 && reachedBottom && checkboxRead && checkboxAgree && answer.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.disclaimerTitle, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = strings.commonBack)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState),
        ) {
            Spacer(Modifier.height(8.dp))

            // Warning banner — 圆角与主页/授权页统一（GlassRadii.Card = 28dp）
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                cornerRadius = GlassRadii.Card,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                        .padding(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            Icons.Rounded.Warning, null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                strings.disclaimerBannerTitle,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 16.sp,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                strings.disclaimerWarning,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // No-resale notice (prominent) — 圆角与主页/授权页统一
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                cornerRadius = GlassRadii.Card,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                        .padding(16.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Gavel, null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                strings.disclaimerNoResaleTitle,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 16.sp,
                            )
                        }
                        Text(
                            strings.disclaimerNoResaleBody,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 20.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Terms body — 圆角与主页/授权页统一（GlassRadii.Card = 28dp）
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                cornerRadius = GlassRadii.Card,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    strings.disclaimerSections.forEachIndexed { index, clause ->
                        DisclaimerClause(index = index + 1, title = clause.first, body = clause.second)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Countdown chip
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Countdown chip — 圆角与主页/授权页统一（GlassRadii.Card = 28dp）
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = GlassRadii.Card,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (countdown > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else AppGreen.copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Timer, null,
                                tint = if (countdown > 0) MaterialTheme.colorScheme.primary else AppGreen,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (countdown > 0) strings.disclaimerWaitCountdown(countdown)
                                else strings.disclaimerReadTimeOk,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (countdown > 0) MaterialTheme.colorScheme.primary else AppGreen,
                            )
                        }
                    }
                }

                if (!reachedBottom) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            strings.disclaimerScrollToBottom,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Interaction area — ALWAYS rendered (no layout feedback loop).
            // Button is disabled until all conditions met.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            ) {
                DisclaimerCheckRow(
                    checked = checkboxRead,
                    onCheckedChange = { checkboxRead = it },
                    text = strings.disclaimerAgreeTerms,
                )
                DisclaimerCheckRow(
                    checked = checkboxAgree,
                    onCheckedChange = { checkboxAgree = it },
                    text = strings.disclaimerAcceptRisk,
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    strings.disclaimerMathLabel(problem.question),
                    fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = {
                        answer = it.filter { c -> c.isDigit() || c == '-' }
                        mathError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = mathError != null,
                    label = { Text(strings.disclaimerMathHint) },
                    supportingText = mathError?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    shape = RoundedCornerShape(10.dp),
                )

                Spacer(Modifier.height(14.dp))

                // Dynamic hint based on what's still missing
                val hints = mutableListOf<String>()
                if (countdown > 0) hints.add(strings.disclaimerWaitCountdown(countdown))
                if (!reachedBottom) hints.add(strings.disclaimerScrollToBottom)
                if (!checkboxRead || !checkboxAgree) hints.add(strings.disclaimerUnlockRequirements)

                if (!allReady) {
                    Text(
                        hints.joinToString(" · "),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(12.dp))
                // 进入(右) / 退出(左) 并排显示——统一纯色按钮，不做玻璃模糊
                val interactionSourceEnter = remember { MutableInteractionSource() }
                val interactionSourceExit = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 退出（左边）
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .clickable(
                                interactionSource = interactionSourceExit,
                                indication = ripple(),
                            ) { clickHaptic(view); onCancel() },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                strings.disclaimerExit,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    // 进入（右边）— 绿色确认风格
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .clickable(
                                interactionSource = interactionSourceEnter,
                                indication = ripple(),
                                enabled = allReady,
                            ) {
                                val parsed = answer.toIntOrNull()
                                if (parsed == null) {
                                    mathError = strings.disclaimerMathInvalid
                                    return@clickable
                                }
                                if (parsed != problem.correctAnswer) {
                                    mathError = strings.disclaimerMathWrong
                                    answer = ""
                                    problem = randomProblem()
                                    return@clickable
                                }
                                clickHaptic(view)
                                onConfirm()
                            },
                        shape = RoundedCornerShape(50),
                        color = if (allReady) AppGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, if (allReady) AppGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                strings.disclaimerEnter,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (allReady) AppGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class MathProblem(val a: Int, val b: Int, val isAdd: Boolean) {
    val question: String get() = "$a ${if (isAdd) "+" else "−"} $b = ?"
    val correctAnswer: Int get() = if (isAdd) a + b else a - b
}

@Composable
private fun DisclaimerClause(index: Int, title: String, body: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(index.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DisclaimerCheckRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { clickHaptic(view); onCheckedChange(!checked) }
            .padding(vertical = 6.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun randomProblem(): MathProblem {
    val isAdd = kotlin.random.Random.nextBoolean()
    val a = kotlin.random.Random.nextInt(10, 80)
    val b = if (isAdd) kotlin.random.Random.nextInt(3, 50)
    else kotlin.random.Random.nextInt(3, if (a < 50) a else 50)
    return MathProblem(a, b, isAdd)
}

// ==================== 2. Shizuku Check Page ====================
/**
 * 一键跳转到系统「无线调试」设置页。
 *
 * 注意：本函数仅做 UI 跳转，不涉及任何 root / 核心执行逻辑。
 * 无线调试配对是本地回环（loopback），仅需打开 Wi-Fi 硬件开关，
 * 无需连接任何 Wi-Fi 网络或联网。
 */
private fun openWirelessDebuggingSettings(context: Context) {
    // 使用原始 action 字符串，避免依赖 SDK 平台桩中未暴露的符号
    // 值等同于 Settings.ACTION_WIRELESS_DEBUGGING_SETTINGS，自 API 29 起有效
    val intent = Intent("android.settings.WIRELESS_DEBUGGING_SETTINGS").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // 部分 ROM 该 action 不存在，回退到「开发者选项」页
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
        } catch (_: Exception) {
            // 再回退到系统设置首页
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS)
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
        }
    }
}

/**
 * 检测是否已连上 WLAN 网络。
 * 三星固件在开启「无线调试」前要求先连上一个 Wi-Fi 网络，否则系统会提示
 * 「请连接到 WLAN 网络」。本检查仅用于 UI 提示，不影响任何核心逻辑。
 */
private fun isWifiConnected(context: Context): Boolean {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
        val connectedViaWifi = cm?.activeNetworkInfo?.let { info ->
            info.isConnected && info.type == android.net.ConnectivityManager.TYPE_WIFI
        } ?: false
        val hasWifiNetwork = wifi?.connectionInfo?.let { info ->
            info.networkId != -1 && info.supplicantState == android.net.wifi.SupplicantState.COMPLETED
        } ?: false
        connectedViaWifi || hasWifiNetwork
    } catch (_: Exception) {
        false
    }
}

@Composable
fun ShizukuCheckPage(
    shizukuManager: com.samsung.rootkit.shizuku.ShizukuManager,
    onReady: () -> Unit,
    onBack: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val view = LocalView.current
    var installed by remember { mutableStateOf(shizukuManager.isShizukuInstalled()) }
    var running by remember { mutableStateOf(shizukuManager.isShizukuRunning()) }
    var hasPermission by remember { mutableStateOf(shizukuManager.hasPermission()) }
    var wifiConnected by remember { mutableStateOf(isWifiConnected(context)) }
    var showShizukuAuthDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        shizukuManager.setPermissionResultListener { _, grantResult ->
            hasPermission = grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        onDispose { shizukuManager.clearPermissionResultListener() }
    }

    fun recheck() {
        installed = shizukuManager.isShizukuInstalled()
        running = shizukuManager.isShizukuRunning()
        hasPermission = shizukuManager.hasPermission()
        wifiConnected = isWifiConnected(context)
    }

    // 从 Shizuku 应用 / 系统设置返回后自动刷新状态，无需手动点重新检查
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                recheck()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val allOk = installed && running && hasPermission

    Scaffold(
        containerColor = Color.Transparent,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 54.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = strings.commonBack,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    pageHeaderIconCircle(Icons.Rounded.Security)()
                    Text(strings.shizukuTitle, style = MaterialTheme.typography.headlineLarge)
                }
            }

            item {
                // Shizuku 副标题说明区——纯文字信息展示，无容器样式
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        Icons.Rounded.Security,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text(strings.shizukuSubtitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            strings.shizukuNoPc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                if (!wifiConnected) {
                    GlassCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = strings.shizukuWifiNotConnected,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            item {
                // 一键跳转系统「无线调试」设置 —— 重启后无需连电脑，连上 WLAN 即可本地配对启动 Shizuku
                ShizukuActionCard(
                    icon = Icons.Rounded.Wifi,
                    title = strings.shizukuOpenWirelessDebug,
                    description = strings.shizukuOpenWirelessDebugDesc,
                    onClick = {
                        wifiConnected = isWifiConnected(context)
                        openWirelessDebuggingSettings(context)
                    },
                )
            }

            item {
                ShizukuStatusRow(
                    icon = Icons.Rounded.CloudDownload,
                    title = strings.shizukuInstalled,
                    description = if (installed) strings.shizukuDetected else strings.shizukuNotInstalled,
                    isOk = installed,
                )
            }

            if (installed) {
                item {
                    ShizukuStatusRow(
                        icon = Icons.Rounded.Refresh,
                        title = strings.shizukuRunning,
                        description = if (running) strings.shizukuRunningOk else strings.shizukuNotRunning,
                        isOk = running,
                    )
                }
            }

            if (running) {
                item {
                    ShizukuStatusRow(
                        icon = Icons.Rounded.VerifiedUser,
                        title = strings.shizukuAuthorized,
                        description = if (hasPermission) strings.shizukuGranted else strings.shizukuNeedGrant,
                        isOk = hasPermission,
                    )
                }
            }

            if (allOk) {
                item {
                    val interactionSource = remember { MutableInteractionSource() }
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GlassRadii.Card))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(),
                            ) { clickHaptic(view); onReady() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Icon(
                                Icons.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.shizukuReady, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    strings.shizukuReadyDesc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            } else {
                if (!installed) {
                    item {
                        ShizukuActionCard(
                            icon = Icons.Rounded.CloudDownload,
                            title = strings.shizukuDownload,
                            description = strings.shizukuDownloadDesc,
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/")))
                            },
                        )
                    }
                }
                if (installed && !running) {
                    item {
                        ShizukuActionCard(
                            icon = Icons.Rounded.OpenInNew,
                            title = strings.shizukuOpen,
                            description = strings.shizukuOpenDesc,
                            onClick = {
                                shizukuManager.getShizukuIntent()?.let { context.startActivity(it) }
                            },
                        )
                    }
                }
                if (running && !hasPermission) {
                    item {
                        ShizukuActionCard(
                            icon = Icons.Rounded.VerifiedUser,
                            title = strings.shizukuRequest,
                            description = strings.shizukuRequestDesc,
                            onClick = {
                                showShizukuAuthDialog = true
                            },
                        )
                    }
                }
                if (running && !hasPermission) {
                    item {
                        Text(
                            text = strings.shizukuManualHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        )
                    }
                }
                item {
                    val recheckInteraction = remember { MutableInteractionSource() }
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = recheckInteraction,
                                indication = ripple(),
                            ) { clickHaptic(view); recheck() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                strings.shizukuRecheck,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            item {
                GlassCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(strings.shizukuRebootGuideTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = strings.shizukuRebootGuideBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                        )
                    }
                }
            }

            item {
                GlassCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(strings.shizukuHowToTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = strings.shizukuHowToBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }

    // Shizuku 授权玻璃弹窗（与 AboutDialog / CustomAlertDialog 同款 drawBackdrop 磨砂效果）
    if (showShizukuAuthDialog) {
        ShizukuAuthDialog(
            onConfirm = {
                showShizukuAuthDialog = false
                shizukuManager.requestPermission { granted -> hasPermission = granted }
            },
            onDismiss = { showShizukuAuthDialog = false },
        )
    }
}

@Composable
private fun ShizukuStatusRow(
    icon: ImageVector,
    title: String,
    description: String,
    isOk: Boolean,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconCircle(
                icon = icon,
                containerColor = if (isOk) MaterialTheme.colorScheme.primaryContainer
                                 else MaterialTheme.colorScheme.error,
                contentColor = if (isOk) MaterialTheme.colorScheme.onPrimaryContainer
                               else MaterialTheme.colorScheme.onError,
                size = 40,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOk) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Icon(
                if (isOk) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ShizukuActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { clickHaptic(view); onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ==================== 3. Model Select Page ====================
@Composable
fun ModelSelectPage(
    deviceInfo: DeviceInfo?,
    payloads: List<PayloadEntry>,
    shizukuManager: com.samsung.rootkit.shizuku.ShizukuManager,
    payloadManager: com.samsung.rootkit.data.PayloadManager,
    onPayloadSelected: (PayloadEntry) -> Unit,
    onCustomSelected: (CustomPayloadInfo) -> Unit,
    onBack: () -> Unit = {},
    // 本次流程的 KSU 变体（首页按钮决定）。自选外部文件时用它匹配内置 ksud。
    variant: PayloadManager.KsuVariant = PayloadManager.KsuVariant.KSU,
) {
    val strings = LocalStrings.current
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showManualList by remember { mutableStateOf(false) }
    // 按机型分组（不区分国行/国际/美版等地区，只区分机型）
    // displayName 格式如 "Galaxy Z Flip5 (SM-F7310)"，取括号前部分作为机型名
    val groupedPayloads = remember(payloads) {
        payloads.groupBy { entry ->
            entry.displayName.substringBefore(" (").ifEmpty { entry.displayName }
        }.toSortedMap()
    }
    // 已展开的机型组集合
    var expandedModels by remember { mutableStateOf(setOf<String>()) }
    // 点击「选择文件」按钮后先弹出的提示框：说明需选 payload + root，ksud 优先内部适配
    var showCustomHintDialog by remember { mutableStateOf(false) }
    // 缓存：用户选了 payload+root 后先暂存,等 ksud 也齐了一次性传给 Wizard
    // 用于「分步选择」:第一次选 payload+root → 缺 ksud → 弹窗让用户只补 ksud,不重选 payload+root
    var pendingCustomPayload by remember { mutableStateOf<PendingCustomPayload?>(null) }
    // 「缺少 ksud 文件」弹窗控制（仿用户截图 #1 样式）—— payload+root 都有但 ksud 缺失时弹出
    var showCustomMissingKsudDialog by remember { mutableStateOf(false) }
    // 「缺少 payload 文件」或「缺少 root 文件」弹窗控制（弹窗式提示,非红字）。
    // 值:null=不弹;"payload"=缺 payload 文件;"root"=缺 root 文件。
    var missingFileType by remember { mutableStateOf<String?>(null) }
    // ksud 版本不匹配弹窗（仿 RootKitScreen 顶层「ksud 不匹配」样式,弹窗式而非红字）。
    // ksudVersionMismatchInfo 保存 (ksudVer, kernelMain) 用于弹窗正文显示。
    var showKsudVersionMismatchDialog by remember { mutableStateOf(false) }
    var ksudVersionMismatchInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
    // 「选择的文件不对」弹窗控制：SAF 选完文件后在 launcher 回调里先按文件名检测,
    // 不匹配则弹此提示而非走 handleCustomFiles 复制到 cacheDir,避免选错文件进缓存造成垃圾堆积。
    // 用两个独立 state 区分来源,避免用字符串 when 判断导致「重新选择」打开错误的 launcher:
    //   showWrongExternalDialog = 多选 safLauncher(payload+root) 检测失败触发 → 重新打开 safLauncher
    //   showWrongKsudDialog     = 单选 customKsudLauncher(ksud) 检测失败触发 → 重新打开 customKsudLauncher
    var showWrongExternalDialog by remember { mutableStateOf(false) }
    var showWrongKsudDialog by remember { mutableStateOf(false) }

    /**
     * 查询 SAF Uri 的显示文件名（DISPLAY_NAME）。
     */
    fun queryDisplayName(uri: android.net.Uri): String? = try {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) c.getString(idx) else null
            } else null
        }
    } catch (_: Exception) { null }

    /**
     * 根据设备 kernel 主版本匹配内置 ksud 文件名（按变体）。
     * 返回 null 仅表示：内核代际不在内置范围内（无法判定系列）。
     *
     * 注：两个变体自 2026-10-02 起都改用各自内嵌 8 个 KMI 的统一 ksud
     * （KSU → [PayloadManager.KSUD_UNIFIED]；Next → [PayloadManager.KSUD_NEXT_UNIFIED]），
     * 不再按系列分文件，故此处只需先判定系列即可。
     */
    fun matchBuiltinKsud(kernelVer: String, variant: PayloadManager.KsuVariant): String? {
        PayloadManager.seriesOfKernel(kernelVer) ?: return null
        return PayloadManager.ksudFileName(variant)
    }

/**
     * 处理 SAF 选中的多个文件（支持分步选择）：
     *   1. 按文件名后缀分类为 payload / root / ksud
     *   2. 全部复制到 cacheDir/custom_payloads/
     *   3. 与 pendingCustomPayload 缓存合并（本次可能只选了 ksud,payload/root 用缓存的）
     *   4. 分步骤检查：缺 payload → 提示；缺 root → 提示；都有 → 检测 ksud
     *   5. ksud 检测：builtinUsable → 直接用内置；ksudFile != null → 适配性检测 + 用用户 ksud；
     *      都没 → 缓存 + 弹「缺少 ksud 文件」让用户单选 ksud
     */
    /**
     * 从外部自选 payload 文件名解析 board / firmware（中间段固件码）。
     *
     * 设计目标（用户明确要求：只匹配中间段）：
     *   文件名可能是多种前缀格式，例如：
     *     - "S9180_S9180ZCS8FZG1"（下划线分隔，前缀=型号本身）
     *     - "dm3q-S9180ZCS8FZG1"（连字符分隔，前缀=board 码）
     *     - "pa1q-S9310ZCUBCZF1"（标准 device-firmware 格式）
     *   不论前缀如何，真正的固件 PDA 码是中间的 "S9180ZCS8FZG1" 这种段——
     *   特征：含数字、且是文件名中**最长**的字母数字混合 token。
     *
     * 提取策略：
     *   1. 按非字母数字字符（_- 等）切分为 token；
     *   2. firmware = 含数字且最长的 token（固件码必然含版本数字且长于 board/型号短码）；
     *   3. board = 除 firmware 外、含小写字母或最短的候选 token（如 dm3q/pa1q）；
     *      若实在无法推断，回退 "custom"，由用户手动选择 ksud 时兜底。
     */
    fun parseCustomPayloadKey(key: String): Pair<String, String> {
        val tokens = key.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return "custom" to "custom"

        // firmware：含数字且最长的 token（固件 PDA 码如 S9180ZCS8FZG1 必含数字且最长）
        val firmware = tokens
            .filter { it.any { c -> c.isDigit() } }
            .maxByOrNull { it.length }
            ?: tokens.maxByOrNull { it.length }
            ?: "custom"

        // board：排除 firmware 后，优先取含小写字母的 token（dm3q/pa1q/b6q 等 board 码含小写），
        //        否则取最短 token；都没有则回退 custom
        val boardCandidates = tokens.filter { it != firmware }
        val board = boardCandidates
            .firstOrNull { it.any { c -> c.isLowerCase() } }
            ?: boardCandidates.minByOrNull { it.length }
            ?: "custom"

        return board to firmware
    }

    // 外部文件（payload/root）文件名匹配规则：
    //   payload：以 "payload" 结尾 / 以 ".so" 结尾（不区分大小写）/ 文件名含 "43499"（不区分后缀）
    //   root：文件名含 "root" 字样（不区分大小写）。优先级：payload 强后缀 > root > .so/43499 > ksud
    fun isPayloadFileName(name: String): Boolean {
        return name.endsWith("payload")
            || name.endsWith(".so", ignoreCase = true)
            || name.contains("43499", ignoreCase = true)
    }

    fun isRootFileName(name: String): Boolean {
        return name.contains("root", ignoreCase = true)
    }

    fun handleCustomFiles(uris: List<android.net.Uri>) {
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val customDir = java.io.File(context.cacheDir, "custom_payloads").apply { mkdirs() }
            var payloadFile: java.io.File? = null
            var rootFile: java.io.File? = null
            var ksudFile: java.io.File? = null

            for (uri in uris) {
                val name = queryDisplayName(uri) ?: continue
                val dest = java.io.File(customDir, name)
                val ok = try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                    true
                } catch (_: Exception) { false }
                if (!ok) continue

                when {
                    name.endsWith("payload") -> payloadFile = dest
                    name.contains("root", ignoreCase = true) -> rootFile = dest
                    name.endsWith(".so", ignoreCase = true) || name.contains("43499", ignoreCase = true) -> payloadFile = dest
                    name.contains("ksud", ignoreCase = true) -> ksudFile = dest
                }
            }

            // 与缓存合并：分步选择场景下,本次选择可能只含 ksud,payload/root 用上次缓存的
            val effectivePayload = payloadFile ?: pendingCustomPayload?.payloadFile
            val effectiveRoot = rootFile ?: pendingCustomPayload?.rootFile

            // 分步骤提示（弹窗式,非红字）:缺什么缓存另一个文件 + 弹窗让用户补选
            // 用户点弹窗的"选择文件"重新打开 SAF 多选,选中的文件会和已缓存的合并
            if (effectivePayload == null) {
                pendingCustomPayload = PendingCustomPayload(
                    payloadFile = null,
                    rootFile = effectiveRoot,  // 缓存已有的 root
                    board = null,
                    firmware = null,
                )
                missingFileType = "payload"
                return@launch
            }
            if (effectiveRoot == null) {
                // 有 payload 就能解析 board/firmware,缓存;后续 ksud 检测要用
                val key = effectivePayload.name.removeSuffix("__payload")
                val (cachedBoard, cachedFirmware) = parseCustomPayloadKey(key)
                pendingCustomPayload = PendingCustomPayload(
                    payloadFile = effectivePayload,  // 缓存已有的 payload
                    rootFile = null,
                    board = cachedBoard,
                    firmware = cachedFirmware,
                )
                missingFileType = "root"
                return@launch
            }

            val board = pendingCustomPayload?.board ?: run {
                val key = effectivePayload.name.removeSuffix("__payload")
                parseCustomPayloadKey(key).first
            }
            val firmware = pendingCustomPayload?.firmware ?: run {
                val key = effectivePayload.name.removeSuffix("__payload")
                parseCustomPayloadKey(key).second
            }
            val kernelVer = deviceInfo?.kernel.orEmpty()
            val matchedBuiltin = matchBuiltinKsud(kernelVer, variant)
            // 关键：matchBuiltinKsud 只按内核版本字符串匹配,不检查文件是否实际存在。
            // 用户删除内置 ksud 后 matchedBuiltin 仍非空 → 必须再加文件存在性检查,
            // 否则走内置路径 → 执行时 getKsudAsset 抛 "ksud not found" → 死循环。
            val builtinUsable = matchedBuiltin != null && payloadManager.hasBuiltinKsud(matchedBuiltin)

            when {
                builtinUsable -> {
                    onCustomSelected(CustomPayloadInfo(
                        payloadFile = effectivePayload,
                        rootFile = effectiveRoot,
                        ksudName = matchedBuiltin,
                        board = board,
                        firmware = firmware
                    ))
                    pendingCustomPayload = null  // 成功路径清空缓存
                }
                ksudFile != null -> {
                    // 适配性检测：从 ksud 文件名提取版本号与设备 kernel 主版本比对。
                    // 提取不到版本号时放行（不误伤任意命名的文件）。
                    val ksudVer = Regex("""(\d+\.\d+)""").find(ksudFile.name)?.groupValues?.get(1)
                    val kernelMain = Regex("""^(\d+\.\d+)""").find(kernelVer)?.value
                    if (ksudVer != null && kernelMain != null && ksudVer != kernelMain) {
                        // 不匹配 → 弹窗而非红字；缓存 payload+root（含 board/firmware）供用户只重选 ksud
                        pendingCustomPayload = PendingCustomPayload(
                            payloadFile = effectivePayload,
                            rootFile = effectiveRoot,
                            board = board,
                            firmware = firmware,
                        )
                        ksudVersionMismatchInfo = ksudVer to kernelMain
                        showKsudVersionMismatchDialog = true
                        return@launch
                    }
                    onCustomSelected(CustomPayloadInfo(
                        payloadFile = effectivePayload,
                        rootFile = effectiveRoot,
                        ksudName = "ksud-custom-user",
                        board = board,
                        firmware = firmware,
                        ksudFile = ksudFile
                    ))
                    pendingCustomPayload = null
                }
                else -> {
                    // payload+root 都有了,但 ksud 适配不上（内置缺失且用户没自带）
                    // 缓存 payload+root 等待用户补选 ksud；弹「缺少 ksud 文件」对话框,
                    // 让用户**只**补选 ksud 文件,不必重新选 payload+root
                    pendingCustomPayload = PendingCustomPayload(
                        payloadFile = effectivePayload,
                        rootFile = effectiveRoot,
                        board = board,
                        firmware = firmware,
                    )
                    showCustomMissingKsudDialog = true
                }
            }
        }
    }

    // SAF 多选文件选择器（第一次选 payload+root 或一次选三个文件）
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            // 先按文件名检测：选中的所有文件必须至少包含一个 payload（*payload / *.so / 含 43499）和一个 root（*root / 含 root 字样）。
            // 不匹配则弹「选择的文件不对」提示,不调 handleCustomFiles(避免错误文件被复制到 cacheDir 造成垃圾堆积)。
            val names = uris.mapNotNull { queryDisplayName(it) }
            val hasPayload = names.any { isPayloadFileName(it) }
            val hasRoot = names.any { isRootFileName(it) }
            if (!hasPayload && !hasRoot) {
                showWrongExternalDialog = true
                return@rememberLauncherForActivityResult
            }
            handleCustomFiles(uris)
        }
    }

    // ksud 单选 launcher（弹「缺少 ksud 文件」时触发,让用户**只**补选 ksud,
    // 用之前缓存的 payload+root 不必重选）
    val customKsudLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            // 先按文件名检测：ksud 文件名通常包含 "ksud"（不区分大小写）。
            // 不匹配则弹「选择的文件不对」提示,不调 handleCustomFiles(避免错误文件被复制到 cacheDir)。
            val name = queryDisplayName(uri) ?: ""
            if (!name.contains("ksud", ignoreCase = true)) {
                showWrongKsudDialog = true
                return@rememberLauncherForActivityResult
            }
            handleCustomFiles(listOf(uri))
        }
    }

    // 清除自定义文件缓存：清空 pendingCustomPayload 内存引用 + 删除 cacheDir/custom_payloads/ 实际文件。
    // 所有弹窗的「取消操作」按钮都调用它,避免用户反复选择/取消时缓存文件堆积占用存储。
    fun clearCustomCache() {
        pendingCustomPayload = null
        scope.launch(Dispatchers.IO) {
            val customDir = java.io.File(context.cacheDir, "custom_payloads")
            if (customDir.exists()) customDir.listFiles()?.forEach { it.delete() }
        }
    }

    var autoMatch by remember { mutableStateOf<PayloadEntry?>(null) }
    var matchError by remember { mutableStateOf<String?>(null) }

    Scaffold(containerColor = Color.Transparent) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = strings.commonBack,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    pageHeaderIconCircle(Icons.Rounded.Memory)()
                    Text(strings.modelSelectTitle, style = MaterialTheme.typography.headlineLarge)
                }
            }

            item { SectionLabel(strings.modelDeviceInfo) }
            item {
                GlassCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (deviceInfo != null) {
                            InfoLine(Icons.Rounded.Memory, strings.modelDeviceModel, deviceInfo.model.orEmpty())
                            InfoLine(Icons.Rounded.Code, strings.modelDeviceBoard, deviceInfo.board.orEmpty())
                            InfoLine(Icons.Rounded.Description, strings.modelDeviceKernel, deviceInfo.kernel.orEmpty())
                            InfoLine(Icons.Rounded.Info, strings.modelDeviceFingerprint, deviceInfo.fingerprint.orEmpty())
                            InfoLine(Icons.Rounded.Security, strings.homeSdkLabel, deviceInfo.sdk.orEmpty())
                        }
                    }
                }
            }

            item { SectionLabel(strings.modelAutoMatch) }
            item {
                val interactionSource = remember { MutableInteractionSource() }
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GlassRadii.Card))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                        ) {
                            clickHaptic(view)
                            // 已展开（成功或失败）→ 收起；未展开 → 执行自动匹配
                            if (autoMatch != null || matchError != null) {
                                autoMatch = null
                                matchError = null
                            } else if (deviceInfo != null) {
                                val match = payloadManager.autoMatch(deviceInfo.fingerprint.orEmpty())
                                if (match != null) {
                                    autoMatch = match
                                } else {
                                    matchError = strings.modelNoMatchError
                                }
                            }
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.modelAutoDetect, style = MaterialTheme.typography.titleMedium)
                            Text(
                                strings.modelAutoDetectDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f),
                            )
                        }
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(if (autoMatch != null || matchError != null) 90f else 0f),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            autoMatch?.let { entry ->
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(strings.modelMatched, style = MaterialTheme.typography.labelMedium)
                            Text(entry.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(strings.modelFirmware(entry.firmware), style = MaterialTheme.typography.bodySmall)
                            Text(strings.modelBoard(entry.board), style = MaterialTheme.typography.bodySmall)
                            Text(strings.modelStatus(entry.status), style = MaterialTheme.typography.bodySmall)
                            Text(strings.modelRegion(entry.region), style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(12.dp))
                            val interactionSource = remember { MutableInteractionSource() }
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = ripple(),
                                    ) { clickHaptic(view); onPayloadSelected(entry) },
                                cornerRadius = 20.dp,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.error)
                                        Text(strings.modelConfirmRoot, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            matchError?.let { error ->
                item {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            item { SectionLabel(strings.modelManualSelect) }
            item {
                val interactionSource = remember { MutableInteractionSource() }
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GlassRadii.Card))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                        ) { clickHaptic(view); showManualList = !showManualList },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Rounded.List, contentDescription = null)
                        Text(if (showManualList) strings.modelCollapse else strings.modelExpand(payloads.size), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            if (showManualList) {
                groupedPayloads.forEach { (model, entries) ->
                    item(key = "group_$model") {
                        ModelGroupHeader(
                            model = model,
                            count = entries.size,
                            expanded = expandedModels.contains(model),
                            onClick = {
                                expandedModels = if (expandedModels.contains(model))
                                    expandedModels - model else expandedModels + model
                            },
                        )
                    }
                    if (expandedModels.contains(model)) {
                        items(entries, key = { it.key }) { entry ->
                            PayloadCard(entry = entry, onSelect = { onPayloadSelected(entry) })
                        }
                    }
                }
            }

            // 自选外部文件 root（不走内置 pack，新增机型直接放手机里选）
            // 位置：紧跟展开的机型列表之后；收起时 showManualList=false,整块自动回退到手动选择按钮下方
            item { SectionLabel("外部文件") }
            item {
                val interactionSource = remember { MutableInteractionSource() }
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GlassRadii.Card))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                        ) {
                            clickHaptic(view)
                            showCustomHintDialog = true
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Rounded.FolderOpen,
                            contentDescription = null,
                        )
                        Text("选择文件（payload + root）", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }

    // 点击「选择文件」按钮后先弹出的提示框：说明文件要求，确认后才打开 SAF
    if (showCustomHintDialog) {
        CustomAlertDialog(
            title = "外部文件",
            confirmText = "选择文件",
            onConfirm = {
                showCustomHintDialog = false
                safLauncher.launch(arrayOf("*/*"))
            },
            onDismiss = {
                showCustomHintDialog = false
                clearCustomCache()
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("需选择 payload 文件和 root 文件", style = MaterialTheme.typography.bodyMedium)
                Text("ksud 文件优先内部适配", style = MaterialTheme.typography.bodyMedium)
                Text("payload 识别：*payload / *.so / 文件名含 43499；root 识别：*root / 文件名含 root 字样", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    // 「缺少 payload 文件」/「缺少 root 文件」对话框（弹窗式提示,非红字）
    // 触发场景：用户选了部分文件(只 payload 或只 root),缺另一个。
    // 已选文件存在 pendingCustomPayload 缓存里,用户点「选择文件」重新打开 SAF,
    // 第二次选的文件会和缓存合并。
    // 点「取消」清空全部缓存,避免反复弹窗死循环。
    if (missingFileType != null) {
        val isPayload = missingFileType == "payload"
        CustomAlertDialog(
            title = if (isPayload) "缺少 payload 文件" else "缺少 root 文件",
            confirmText = "选择文件",
            onConfirm = {
                missingFileType = null
                safLauncher.launch(arrayOf("*/*"))
            },
            onDismiss = {
                missingFileType = null
                clearCustomCache()
            },
        ) {
            Text(
                if (isPayload) "尚未检测到 payload 文件，请补选（可识别：*payload、*.so 或文件名含 43499）。\n已选的其他文件已缓存，无需重新选择。"
                else "尚未检测到 root 文件，请补选（可识别：*root 或文件名含 root 字样）。\n已选的其他文件已缓存，无需重新选择。",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    // 「缺少 ksud 文件」对话框（仿用户截图 #1 样式）
    // 触发场景：用户已选 payload+root,内置 ksud 缺失且用户没自带 ksud。
    // 点「选择 ksud」调用 customKsudLauncher(单选),让用户**只**补选 ksud,
    // payload+root 用 pendingCustomPayload 缓存(不必重选)。
    // 点「取消」清空缓存,避免反复弹窗死循环。
    if (showCustomMissingKsudDialog) {
        CustomAlertDialog(
            title = "缺少 ksud 文件",
            confirmText = "选择文件",
            onConfirm = {
                showCustomMissingKsudDialog = false
                customKsudLauncher.launch(arrayOf("*/*"))
            },
            onDismiss = {
                showCustomMissingKsudDialog = false
                clearCustomCache()
            },
        ) {
            Text(
                "执行时检测到 ksud 文件缺失,请手动选择一个 ksud 文件后重试。\n" +
                "提示:ksud 文件通常文件名包含 \"ksud\",内核版本需要与设备匹配（5.15/6.1/6.6 等）。",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    // 「ksud 不匹配」弹窗（弹窗式而非红字）
    // 触发场景:handleCustomFiles 检测到用户选的 ksud 文件名版本号与设备内核主版本不匹配。
    // 点「选择文件」调用 customKsudLauncher(单选),让用户**只**重选正确的 ksud,
    // payload+root 用 pendingCustomPayload 缓存(不必重选);
    // 点「取消操作」清空 pendingCustomPayload 缓存,避免反复弹窗死循环。
    if (showKsudVersionMismatchDialog && ksudVersionMismatchInfo != null) {
        val (ksudVer, kernelMain) = ksudVersionMismatchInfo!!
        CustomAlertDialog(
            title = "ksud 不匹配",
            confirmText = "选择文件",
            onConfirm = {
                showKsudVersionMismatchDialog = false
                ksudVersionMismatchInfo = null
                // 不清空缓存：payload+root 已在 pendingCustomPayload，用户只需重选正确的 ksud
                customKsudLauncher.launch(arrayOf("*/*"))
            },
            onDismiss = {
                showKsudVersionMismatchDialog = false
                ksudVersionMismatchInfo = null
                clearCustomCache()
            },
        ) {
            Text(
                "所选 ksud 不适配:版本 $ksudVer 与设备内核 $kernelMain 不匹配。\n" +
                "请重新选择适配的 ksud 文件（文件名版本号需与设备内核一致）。",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    // 「选择的文件不对」弹窗（仿「缺少 ksud 文件」弹窗样式,左右对称布局）
    // 触发场景:SAF 选完文件后 launcher 回调先按文件名检测,不匹配时弹此提示。
    // 关键:此时 handleCustomFiles 还没被调用,文件**没有**进 cacheDir 缓存,避免选错文件造成垃圾堆积。
    // 用两个独立弹窗分别对应两个 launcher,确保「重新选择」打开的是**同一个** launcher,
    // 避免用字符串 when 判断导致打开错误 launcher(如选了 ksud 却重新打开多选 payload+root 窗口)。
    // 两个弹窗的文案要分开：
    //   - wrongFileHintText:多选 payload+root 选错时用,需同时提示 payload / root / ksud 三种文件名;
    //   - wrongKsudHintText:单选 ksud 选错时用,此时只补选 ksud,只说 ksud 提示即可,
    //     避免误导用户去找 payload/root(那俩在上一步已经选好了)。
    val wrongFileHintText = "需重新选择正确的文件：请选择 payload（*payload / *.so / 含 43499）与 root（*root / 含 root 字样）至少各一个。"
    val wrongKsudHintText = "需重新选择正确的文件，提示：文件名通常含ksud。"

    // 多选 payload+root 选错 → 重新打开 safLauncher(多选)
    if (showWrongExternalDialog) {
        CustomAlertDialog(
            title = "选择的文件错误",
            confirmText = "重新选择",
            onConfirm = {
                showWrongExternalDialog = false
                safLauncher.launch(arrayOf("*/*"))
            },
            onDismiss = {
                showWrongExternalDialog = false
                clearCustomCache()
            },
        ) {
            Text(wrongFileHintText, style = MaterialTheme.typography.bodyMedium)
        }
    }

    // 单选 ksud 选错 → 重新打开 customKsudLauncher(单选)
    if (showWrongKsudDialog) {
        CustomAlertDialog(
            title = "选择的文件错误",
            confirmText = "重新选择",
            onConfirm = {
                showWrongKsudDialog = false
                customKsudLauncher.launch(arrayOf("*/*"))
            },
            onDismiss = {
                showWrongKsudDialog = false
                clearCustomCache()
            },
        ) {
            Text(wrongKsudHintText, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * ModelSelectPage 内的自定义弹窗（统一样式）。
 * 用 Dialog + Surface + Row(Arrangement.SpaceBetween) 实现「取消操作」贴左、「确认」贴右的左右对称布局。
 * Material 3 AlertDialog 的按钮默认靠右（dismissButton 在 confirmButton 左侧但不贴左边缘）,
 * 无法做到两端对称,所以自己实现。所有 ModelSelectPage 弹窗都通过这个 helper 渲染,确保样式一致。
 */
@Composable
private fun CustomAlertDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        // 外层只用纯色容器（shape + border），不做玻璃模糊；
        // 真正的模糊由内层 drawBackdrop(LocalBackdrop) 统一提供。
        // 若外层也用 GlassCard（自带 layerBackdrop+blur），会与内层 drawBackdrop
        // 形成双重嵌套模糊，导致弹窗内容发虚/异常（真机截图验证）。
        val dialogShape = RoundedCornerShape(20.dp)
        // 简单主题检测（与 GlassComponents.isDarkTheme 同逻辑）
        val bg = MaterialTheme.colorScheme.background
        val isDark = (bg.red * 0.2126f + bg.green * 0.7152f + bg.blue * 0.0722f) < 0.5f
        val hairline = if (isDark) Color(0x33FFFFFF) else Color(0xCCFFFFFF)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = dialogShape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, hairline),
        ) {
            // Phase 3：真·实时玻璃模糊（Kyant0/Backdrop）。
            // drawBackdrop 模糊 BackdropProvider 捕获的「弹窗背后内容」，效果在 Android 12+(blur) /
            // Android 13+(lens 折射) 生效；低版本对应 effect 自动 no-op（库源码验证，不崩溃）。
            // onDrawSurface 画主题感知玻璃底（替代原硬编码底色，保留暗色模式适配）。
            val backdrop = LocalBackdrop.current
            val glassOn = LocalGlassEnabled.current // 玻璃总开关（执行页自动停用）：false 时跳过 blur/lens
            // 在 @Composable 上下文预取主题色，避免在 draw lambda 内访问 CompositionLocal
            // （draw lambda 非 Composable 上下文，直接读 MaterialTheme 会触发编译错误）。
            val surfaceTint = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { dialogShape },
                        effects = { if (glassOn) { blur(16.dp.toPx()); lens(24.dp.toPx(), 24.dp.toPx()) } },
                        onDrawSurface = {
                            drawRect(color = surfaceTint)
                        },
                    )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    content()
                    Spacer(modifier = Modifier.height(24.dp))
                    // 2026-10-08：与 GlassAlertDialog 的按钮区统一 —— 药丸玻璃按钮 + 左右等宽对称。
                    // 原先用 SpaceBetween 两端分列 + 裸 TextButton，与「玻璃弹窗」整体质感不一致；
                    // 现改为与 ShizukuAuthDialog 同款 GlassPillButton（既保证左右对称，又是药丸玻璃）。
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GlassPillButton(
                            text = "取消操作",
                            onClick = onDismiss,
                            isPrimary = false,
                            modifier = Modifier.weight(1f),
                        )
                        GlassPillButton(
                            text = confirmText,
                            onClick = onConfirm,
                            isPrimary = false,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Material3 AlertDialog 的「玻璃版」——把原先纯色底的标准 M3 AlertDialog 升级为与
 * [CustomAlertDialog] **完全一致**的实时磨砂玻璃（同一套 Kyant0/Backdrop 管线：
 * 圆角 20dp + blur 16dp + lens 24dp + surfaceContainerHigh@0.9 玻璃底 + 发丝描边）。
 *
 * 为什么不直接复用 [CustomAlertDialog]：后者的排版是写死的（标题 + 内容 +
 * 底部「取消操作/确认」两端对称，且取消文案固定为「取消操作」），无法承载 M3 AlertDialog
 * 各调用点的自定义按钮文案与复杂内容（如 df 卸载完成态的 weight 等宽双按钮）。
 * 故此处采取**最小侵入**策略：完整保留 M3 原生参数
 * （onDismissRequest / title / text / icon / confirmButton / dismissButton），
 * 仅把 M3 自己的 Surface 底色置透明 + tonalElevation 归零，再经 [drawBackdrop]
 * 在其 Surface **之下**叠一层玻璃（modifier 链先于 surface 绘制，故玻璃落在内容背后）。
 * ⇒ 各调用点仅需把 `AlertDialog(` 改成 `GlassAlertDialog(`，版式与文案零变化，
 *   观感与 6 个 [CustomAlertDialog] 统一。
 */
@Composable
private fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    // 与 [CustomAlertDialog] 逐字段对齐的玻璃参数（保证「效果一致」）。
    val dialogShape = RoundedCornerShape(20.dp)
    // 简单主题检测（与 GlassComponents.isDarkTheme / CustomAlertDialog 内联判定同逻辑）
    val bg = MaterialTheme.colorScheme.background
    val isDark = (bg.red * 0.2126f + bg.green * 0.7152f + bg.blue * 0.0722f) < 0.5f
    val hairline = if (isDark) Color(0x33FFFFFF) else Color(0xCCFFFFFF)
    val backdrop = LocalBackdrop.current
    val glassOn = LocalGlassEnabled.current // 玻璃总开关（执行页自动停用）：false 时跳过 blur/lens
    val surfaceTint = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { dialogShape },
                effects = { if (glassOn) { blur(16.dp.toPx()); lens(24.dp.toPx(), 24.dp.toPx()) } },
                onDrawSurface = { drawRect(color = surfaceTint) },
            )
            .border(1.dp, hairline, dialogShape),
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = dialogShape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
    )
}

@Composable
fun PayloadCard(entry: PayloadEntry, onSelect: () -> Unit) {
    val strings = LocalStrings.current
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val enabled = entry.hasBinary
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassRadii.Card))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
            ) { if (enabled) { clickHaptic(view); onSelect() } },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Rounded.Memory,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = if (enabled) LocalContentColor.current else MaterialTheme.colorScheme.onErrorContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.displayName, style = MaterialTheme.typography.titleMedium)
                Text(strings.modelFirmware(entry.firmware), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (entry.isAlias) "复用" else strings.modelStatus(entry.status),
                    style = MaterialTheme.typography.bodyMedium,
                    // 未测试机型用红字警示（其余：复用=主色，普通=次级文字色）
                    color = when {
                        entry.isAlias -> MaterialTheme.colorScheme.primary
                        // 未测试 / 移植待测（root-my-s24 移植载荷）均用红字警示
                        entry.status == "未测试" || entry.status == "移植待测" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(strings.modelRegion(entry.region), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!enabled) {
                    Text(strings.modelNoLocalPayload, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 机型分组标题（一级列表项），点击展开/收起该机型的所有固件版本（二级列表）。
 * 不区分国行/国际/美版等地区，仅按机型名分组。
 */
@Composable
private fun ModelGroupHeader(
    model: String,
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassRadii.Card))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
            ) { clickHaptic(view); onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Rounded.Memory,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = model,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            // 版本数量徽章
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            Text(
                text = if (expanded) "▴" else "▾",
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

// ==================== 4. Executing Page ====================

private data class ExecStep(
    val title: String,
    val detail: String,
    val icon: ImageVector,
)

private fun execStepState(stage: RootExecutor.Stage, index: Int): Int {
    if (stage == RootExecutor.Stage.DONE) return 2
    if (stage == RootExecutor.Stage.FAILED) return 0
    val activeIndex = when (stage) {
        RootExecutor.Stage.DEVICE_CHECK, RootExecutor.Stage.PAYLOAD_MATCH -> 0
        RootExecutor.Stage.PUSH_FILES -> 1
        RootExecutor.Stage.WARMUP, RootExecutor.Stage.EXPLOIT, RootExecutor.Stage.VERIFY_ROOT -> 2
        RootExecutor.Stage.KERNELSU_LOAD, RootExecutor.Stage.INSTALL_APP -> 3
        else -> 0
    }
    return when {
        index < activeIndex -> 2
        index == activeIndex -> 1
        else -> 0
    }
}

private fun execProgress(stage: RootExecutor.Stage): Float = when (stage) {
    RootExecutor.Stage.DEVICE_CHECK -> 0.05f
    RootExecutor.Stage.PAYLOAD_MATCH -> 0.15f
    RootExecutor.Stage.PUSH_FILES -> 0.3f
    RootExecutor.Stage.WARMUP -> 0.5f
    RootExecutor.Stage.EXPLOIT -> 0.65f
    RootExecutor.Stage.VERIFY_ROOT -> 0.78f
    RootExecutor.Stage.KERNELSU_LOAD -> 0.9f
    RootExecutor.Stage.INSTALL_APP -> 0.97f
    RootExecutor.Stage.DONE -> 1f
    RootExecutor.Stage.FAILED -> 0f
}

@Composable
fun ExecutingPage(
    stage: RootExecutor.Stage,
    stageMessage: String,
    logs: List<LogEntry>,
    onBack: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val execSteps = listOf(
        ExecStep(strings.execStepEnvCheck, strings.execStepEnvCheckDesc, Icons.Rounded.Security),
        ExecStep(strings.execStepPush, strings.execStepPushDesc, Icons.Rounded.CloudDownload),
        ExecStep(strings.execStepExploit, strings.execStepExploitDesc, Icons.Rounded.Memory),
        ExecStep(strings.execStepKsu, strings.execStepKsuDesc, Icons.Rounded.Check),
    )
    val logScrollState = rememberScrollState()
    LaunchedEffect(logs.size) {
        logScrollState.animateScrollTo(logScrollState.maxValue)
    }
    val busy = stage != RootExecutor.Stage.DONE && stage != RootExecutor.Stage.FAILED

    // 注意：本页内部 Log Card 已使用 LazyColumn（可垂直滚动组件）。
    // Compose 禁止把可滚动组件放进另一个 verticalScroll 的无限高度约束中，
    // 否则会抛 IllegalStateException 导致闪退。因此外层 Column 不可加 verticalScroll，
    // 改用固定高度约束让 Log Card 可见。
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Column(
            modifier = Modifier.padding(top = 28.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = strings.commonBack,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = strings.execRooting,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = if (busy) {
                    strings.execKeepScreenOn
                } else {
                    stageMessage.ifEmpty { strings.execRunning }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Status Card — 玻璃效果 + 保留原色
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
        ) {
            val heroColor = if (stage == RootExecutor.Stage.FAILED) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
            val heroOnColor = if (stage == RootExecutor.Stage.FAILED) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }
            Surface(
                shape = MaterialTheme.shapes.large,
                color = heroColor,
                contentColor = heroOnColor,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AnimatedContent(targetState = stage, label = "exec-status-icon", transitionSpec = {
                        (fadeIn() + scaleIn()) togetherWith (fadeOut() + scaleOut())
                    }) { currentStage ->
                        when {
                            busy -> LoadingIndicator(
                                modifier = Modifier.size(44.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            currentStage == RootExecutor.Stage.DONE -> Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                            )
                            else -> Icon(
                                Icons.Rounded.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stageMessage.ifEmpty { strings.execRunning },
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = when (stage) {
                                RootExecutor.Stage.DEVICE_CHECK -> strings.execStageDeviceCheck
                                RootExecutor.Stage.PAYLOAD_MATCH -> strings.execStagePayloadMatch
                                RootExecutor.Stage.PUSH_FILES -> strings.execStagePushFiles
                                RootExecutor.Stage.WARMUP -> strings.execStageWarmup
                                RootExecutor.Stage.EXPLOIT -> strings.execStageExploit
                                RootExecutor.Stage.VERIFY_ROOT -> strings.execStageVerifyRoot
                                RootExecutor.Stage.KERNELSU_LOAD -> strings.execStageKsuLoad
                                RootExecutor.Stage.INSTALL_APP -> strings.execStageInstallApp
                                RootExecutor.Stage.DONE -> strings.execStageDone
                                RootExecutor.Stage.FAILED -> strings.execStageFailed
                            },
                            color = LocalContentColor.current.copy(alpha = 0.78f),
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { execProgress(stage) },
                    modifier = Modifier.fillMaxWidth(),
                    color = LocalContentColor.current,
                    trackColor = LocalContentColor.current.copy(alpha = 0.2f),
                    drawStopIndicator = {},
                )
            }
        }
    }

        // Step List Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                execSteps.forEachIndexed { index, step ->
                    val stepState = execStepState(stage, index)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = if (stepState >= 1) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            contentColor = if (stepState >= 1) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (stepState == 2) Icons.Rounded.Check else step.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(21.dp),
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = step.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                            )
                        }
                        if (stepState == 1 && busy) {
                            LoadingIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }

        // Log Card — 直接渲染在玻璃外壳内，字色随主题自适应（浅色/深色模式都协调）
        // 严格对齐 v4.5.3 旧版结构：GlassCard(weight(1f)) → Column(fillMaxSize()) → LazyColumn(weight(1f))
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(strings.execLiveLog, style = MaterialTheme.typography.titleMedium)
                val lazyListState = rememberLazyListState()
                LaunchedEffect(logs.size) {
                    if (logs.isNotEmpty()) {
                        lazyListState.animateScrollToItem(logs.size - 1)
                    }
                }
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (logs.isEmpty()) {
                        item {
                            Text(
                                text = strings.execPreparing,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(logs) { log ->
                            Text(
                                text = log.message,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = when (log.level) {
                                    RootExecutor.LogLevel.ERR -> MaterialTheme.colorScheme.error
                                    RootExecutor.LogLevel.WARN -> MaterialTheme.colorScheme.tertiary
                                    RootExecutor.LogLevel.OK -> AppGreen
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ==================== 5. Result Page ====================
/**
 * 清理缓存结果提示：short 普通提示，long 为需要用户进一步操作的说明（如 KSU 授权）。
 */
@Composable
fun ResultPage(
    success: Boolean,
    logs: List<LogEntry>,
    /** 【测试版】提权耗时（毫秒）：400 次内核预热结束 → root 提权成功；未统计到时为 null */
    rootElapsedMs: Long? = null,
    onRetry: () -> Unit,
    onClose: () -> Unit = {},
    onBackHome: () -> Unit = {},
    onOpenKsu: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val view = LocalView.current
    val scrollState = rememberScrollState()
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Column(
                modifier = Modifier.padding(top = 28.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    IconButton(
                        onClick = onBackHome,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = strings.commonBack,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        text = if (success) strings.resultSuccess else strings.resultFailed,
                        style = MaterialTheme.typography.headlineLarge,
                        color = if (success) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text = if (success) strings.resultSuccessDesc
                          else strings.resultFailedDesc,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Hero status card — 玻璃效果 + 保留原色
            // 整个卡片可点击打开 KernelSU（仅成功时），无按压/波纹效果：用 indication = null 的 clickable
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .then(
                        if (success) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { clickHaptic(view); onOpenKsu() },
                        ) else Modifier
                    ),
            ) {
                val heroColor = if (success) MaterialTheme.colorScheme.primaryContainer
                                 else MaterialTheme.colorScheme.errorContainer
                val heroOnColor = if (success) MaterialTheme.colorScheme.onPrimaryContainer
                                   else MaterialTheme.colorScheme.onErrorContainer
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = heroColor,
                    contentColor = heroOnColor,
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            if (success) Icons.Rounded.Check else Icons.Rounded.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = heroOnColor,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (success) strings.resultComplete else strings.resultIncomplete,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = if (success) strings.resultTempRoot
                                      else strings.resultRebootRetry,
                                color = LocalContentColor.current.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }

            // 【测试版】提权耗时卡片：预热结束 → 提权成功
            if (success && rootElapsedMs != null) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "提权耗时",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "%.2f 秒".format(rootElapsedMs!! / 1000.0),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "统计区间：400 次内核预热结束 → root 提权成功（含 exploit 重试与设备重启等待）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (success) {
                GlassCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(strings.resultUsageNotes, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = strings.resultUsageBody,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Log Area — 直接渲染在玻璃外壳内，字色随主题自适应
            // 严格对齐 v4.5.3 旧版结构：GlassCard(weight(1f)) → Column(fillMaxSize()) → Text(weight(1f).verticalScroll)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(strings.resultExecLog, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = logs.joinToString("\n") { it.message }.ifBlank { strings.execPreparing },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(scrollState),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (success) {
                    // 白色磨砂玻璃药丸按钮：再次执行
                    // （「清理缓存」已改为自动执行 —— root 前与 root 成功后各清一次，不再提供手动按钮）
                    FrostedGlassPillButton(
                        text = strings.resultRunAgain,
                        icon = Icons.Rounded.Refresh,
                        onClick = { clickHaptic(view); onRetry() },
                        modifier = Modifier.weight(1f),
                    )
            } else {
                // 重启提示卡（对齐官方脚本：二次运行失败需先重启设备）
                GlassCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Info, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            strings.resultRebootHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { clickHaptic(view); onClose() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) {
                    Text(strings.resultCancel)
                }
            }
            }
        }
    }
}

// ==================== Bottom Navigation ====================
@Composable
private fun MainBottomBar(
    modifier: Modifier = Modifier,
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
) {
    val strings = LocalStrings.current
    val dark = isSystemInDarkTheme()

    // Tab 定义：图标 + 文字标签
    data class TabDef(val tab: MainTab, val icon: ImageVector, val label: String)
    val tabs = listOf(
        TabDef(MainTab.Home, Icons.Rounded.Home, strings.navHome),
        TabDef(MainTab.RunLog, Icons.Rounded.History, strings.navHistory),
        TabDef(MainTab.Settings, Icons.Rounded.Settings, strings.navSettings),
    )

    // iOS 风格 pill 选中色（半透明高亮，参考 iOS tab bar pill 效果）
    // Round68: 用户要求调浅 1/3。亮色 22%→15%(0x26)，暗色 38%→28%(0x47，暗色仍比亮色深）
    // Round69: pill 改为蓝色渐变渲染（替代纯色平涂），此变量仅作 fallback 参考
    val pillColor = if (dark) Color(0x47FFFFFF) else Color(0x26000000)   // ~28% / ~15%
    // Pill 渐变色：基于页面主色(primary)的 iOS 风格水平光泽渐变
    // Round73: 回调透明度。Round72 误降太透明（0.15/0.35/0.55/0.40），用户反馈"弄反了"。
    //   回到适中可见区间（0.35/0.60/0.82/0.62），保留光泽渐变且选中态清晰可辨。
    val primaryColor = MaterialTheme.colorScheme.primary
    val pillGradientBrush = Brush.horizontalGradient(
        colors = listOf(
            primaryColor.copy(alpha = 0.35f),   // 左：浅蓝
            primaryColor.copy(alpha = 0.60f),   // 中左：中等蓝
            primaryColor.copy(alpha = 0.82f),   // 中右：饱和蓝（主视觉）
            primaryColor.copy(alpha = 0.62f),   // 右：稍回落（高光尾部）
        ),
        startX = 0f,
        endX = Float.POSITIVE_INFINITY,
    )
    // Round72: 选中时图标/文字颜色策略：
    //   - 亮色模式：用底栏背景色（surface/浅白）→ 在蓝色 pill 上对比度高、可读性好
    //   - 暗色模式：选中与未选中一致（onSurfaceVariant）→ 避免暗色下蓝底+深色看不清
    val selectedContentColor = if (dark) {
        MaterialTheme.colorScheme.onSurfaceVariant   // 暗色：选中=未选
    } else {
        MaterialTheme.colorScheme.surface            // 亮色：浅白/底栏色，在蓝pill上清晰
    }
    val unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant

    GlassBottomBarContainer(
        modifier = modifier,
    ) {
        // iOS 风格滑动指示器：一个独立、可动画化的 pill 背景，
        // 用 animateIntOffset 跟随选中 tab 的位置平滑滑动过去（非硬切）。
        // 通过 onGloballyPositioned 测量每个 tab 相对父 Box 的真实坐标，
        // 彻底避免 BoxWithConstraints 坐标系错位导致的"乱跑"问题。
        val density = LocalDensity.current
        val indicatorWidth = 108.dp
        val pillHeight = 52.dp   // 固定 pill 高度（比内容区稍大，形成舒适的内边距感）
        val selectedIndex = tabs.indexOfFirst { it.tab == selected }.coerceAtLeast(0)

        // 父 Box 在窗口中的根坐标（用于把 tab 的绝对坐标换算成相对 Box 的偏移）
        var boxPos by remember { mutableStateOf(Offset.Zero) }
        // 每个 tab 相对父 Box 的左上角坐标 + 尺寸（像素）
        val tabRects = remember { mutableStateListOf<Rect>() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords -> boxPos = coords.positionInRoot() },
        ) {
            // 滑动指示器（绘制在 tab 之下，作为选中背景）
            // Round69 修复：pill 高度固定(pillHeight)，在 tab rect 内真正垂直居中
            // （之前 bug：indicatorHpx=tabRects.height 导致 Y 公式退化为顶部对齐）
            val pillHpx = pillHeight.value * density.density
            val targetOffset = if (tabRects.size == tabs.size && selectedIndex < tabRects.size) {
                val r = tabRects[selectedIndex]
                val x = (r.left + (r.width - indicatorWidth.value * density.density) / 2f).roundToInt()
                val y = (r.top + (r.height - pillHpx) / 2f).roundToInt()   // 真正居中
                IntOffset(x, y)
            } else {
                IntOffset(0, 0)
            }
            val animatedOffset by animateIntOffsetAsState(
                targetValue = targetOffset,
                animationSpec = spring(stiffness = 700f, dampingRatio = 0.75f),
                label = "pillIndicator",
            )
            // Round69: pill 改用蓝色渐变渲染（替代纯色 Surface(color=pillColor)）
            // 用 Box + drawBehind + clip 实现渐变背景 + 圆角裁切
            Box(
                modifier = Modifier
                    .offset { animatedOffset }
                    .width(indicatorWidth)
                    .height(pillHeight)
                    .clip(RoundedCornerShape(50))   // 标准半圆
                    .drawBehind {
                        drawRect(brush = pillGradientBrush)
                    },
            ) {}

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, tabDef ->
                    val isSelected = selected == tabDef.tab
                    val view = LocalView.current

                    // 每个 tab 统一渲染：等分宽度占位 + 图标 + 文字，背景由滑动指示器提供
                    Column(
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                val abs = coords.positionInRoot()
                                val size = coords.size
                                val rect = Rect(
                                    left = abs.x - boxPos.x,
                                    top = abs.y - boxPos.y,
                                    right = abs.x - boxPos.x + size.width,
                                    bottom = abs.y - boxPos.y + size.height,
                                )
                                if (tabRects.size == tabs.size) tabRects[index] = rect
                                else tabRects.add(rect)
                            }
                            .clip(RoundedCornerShape(50))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 28.dp),
                            ) { clickHaptic(view); onSelect(tabDef.tab) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            tabDef.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (isSelected) selectedContentColor else unselectedContentColor,
                        )
                        Text(
                            text = tabDef.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) selectedContentColor else unselectedContentColor,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

// ==================== Home Page ====================

/**
 * KSU（源版）入口按钮的红色（显式色值）。
 * 2026-10-02 F4 修复：原先用 MaterialTheme.colorScheme.error —— 它会随动态取色/深浅主题
 * 漂移（可能偏橙/偏粉），而"红=KSU"与"蓝=Next"一样是对外承诺的识别符号，故与 KsuNextBlue
 * 对称地固定为显式色值。（0xFFE53935 与 0xFF1A73E8 明暗主题下亮度相当，均可读）
 */
private val KsuRed = Color(0xFFE53935)

/**
 * KSU-Next 入口按钮的蓝色（显式色值）。
 * 不用 MaterialTheme.colorScheme.primary：动态取色下它不保证是蓝，而这里"蓝=Next"是识别符号。
 */
private val KsuNextBlue = Color(0xFF1A73E8)

/**
 * 首页的 KernelSU / KernelSU-Next 入口按钮。
 * 玻璃描边风格与页面其它卡片统一；大字区分变体，小字为动作说明。
 */
@Composable
private fun KsuVariantButton(
    bigText: String,
    smallText: String,
    accent: Color,
    // ⚠️ 若将来在本组件新增参数，必须排在 onClick **之前**：
    //    现有调用点用尾随 lambda 写法 `KsuVariantButton(…) { … }`，Kotlin 把尾随 lambda
    //    绑给**最后一个参数**。若新参数排在 onClick 之后，lambda 会被当作该参数传入，
    //    编译报 "No value passed for parameter 'onClick'" +
    //    "actual type is '() -> Unit', but '<T>' was expected"。
    // ── 2026-10-02（二次修订）：以下两个可选参数**当前已无任何调用点使用** ──
    //    历史由来：enabled 曾服务于 F3 的"点击中"灰态防重入（同日已撤销）；
    //    dimmed 曾服务于早期主页 DF 入口的"灰色但可点"，该入口后迁至设置页、
    //    并改用 SettingsCard 呈现，不再经过本组件。
    //    两者保留为组件的通用能力，默认值(true / false)即"恒可用、不压灰"，
    //    对当前两处调用点无任何行为影响；若后续确认长期无人使用，可整组删除。
    // 是否可点击（默认 true）
    enabled: Boolean = true,
    // 是否只做"视觉变灰"、但仍可点击（纯视觉提示"暂不可用"，点击照常放行）
    dimmed: Boolean = false,
    onClick: () -> Unit,
) {
    val view = LocalView.current
    val btnInteraction = remember { MutableInteractionSource() }
    // 灰化态：统一压成低透明度中性色（enabled=false 或 dimmed=true 都会灰）
    val tone = if (enabled && !dimmed) accent
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .clickable(
                // ⚠️ dimmed 只做视觉变灰，**绝不**让按钮失活：
                //    设计意图是"灰色但可用"，点了必须仍能进配置引导 ——
                //    否则用户没有任何途径点亮它。
                //    （旧版曾写成 enabled = dfActive，把灰按钮变成死按钮 —— 该行为已废弃）
                enabled = enabled || dimmed,
                interactionSource = btnInteraction,
                indication = ripple(color = tone.copy(alpha = 0.2f)),
            ) {
                clickHaptic(view)
                onClick()
            },
        cornerRadius = 32.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tone.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(bigText, color = tone, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    smallText,
                    color = tone.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    rootExecutor: RootExecutor,
    shizukuManager: com.samsung.rootkit.shizuku.ShizukuManager,
    onStartWizard: (variant: PayloadManager.KsuVariant, shizukuReady: Boolean) -> Unit,
    // DFReroot 持久化（v5.1.0）：状态由父级 RootKitScreen 持有。
    // 2026-10-01：主页入口按钮已移除（改由设置页「打开 DFReroot」进入），
    // 这里仅保留 dfInstalled/dfActive 供状态卡第四行显示持久化状态。
    dfInstalled: Boolean = false,
    dfActive: Boolean = false,
    /**
     * dfe（DFReroot）**总开关**（2026-10-05，与 df 的 [dfRootEnabled] 对称）。
     *
     * <p>关掉 ⇒ 本页两个按钮的 dfe 分支整体失效（回退「开始 Root 引导」），
     * 状态卡第四行（DFReroot 持久化）显示「未配置」。
     * 与设置页「配置 DFReroot」卡的显隐、root 成功后的弹窗门控共用同一事实源
     * （`SettingsStore.df_entry_enabled`）。
     */
    dfEntryEnabled: Boolean = true,
    // 2026-10-01 晚：持久化可用（dfActive）时，主页两个 root 按钮的**第二状态**——
    // 点击不再进 Shizuku/Root 向导，而是直达 DFReroot 界面（与设置页「打开 DFReroot」
    // 同一路径），并把所点按钮对应的变体传过去（KSU 红 → KSU，KSU-Next 蓝 → NEXT）。
    //
    // 参数 ① = 所点按钮对应的变体；
    // 参数 ② = **本页已判出的「系统已 root」事实**（[systemRooted]，2026-10-07 需求①追加）。
    //   父级用它决定要不要写「运行记录」——与 [onRunDfRoot] 把 systemRooted 传上去
    //   是同一个理由、同一种写法：root 检测在本页（hasRoot / ksuWorking），父级拿不到。
    onOpenDf: (variant: PayloadManager.KsuVariant, systemRooted: Boolean) -> Unit = { _, _ -> },
    // ── df（DFRoot）集成（round24：已内嵌本包，不再是独立 App）──
    // ⚠️ 与上面的 dfInstalled/dfActive（= dfe / DFReroot）不是同一个东西。
    /** DFRoot 总开关（关 ⇒ 本页按钮不再走 df 路，回退 dfe / Root 向导）。 */
    dfRootEnabled: Boolean = false,
    /** 本次开机已注入（`/dev/df` 存在）——仅供状态卡第五行显示事实。 */
    dfRootArmed: Boolean = false,
    /**
     * df（DFRoot）**跑链入口**（round24，用户定调「点击 = 立即跑链」）。
     *
     * <p>参数 ① = 所点按钮对应的变体；
     * 参数 ② = **本页已判出的「系统已 root」事实**（[systemRooted]）。
     *
     * <p>为什么把 ② 一同交给父级（2026-10-05 需求④）：用户要求「df 模式下已 root 时点红/蓝
     * **不进入 root 流程**，改为提示『当前系统已 root，请勿重复操作』」，但**例外**是
     * 「开机自启动」打开且目标变体 ≠ 当前工作时仍要保留『切换下次开机管理器』。
     * 这两个分支都需要「是否已 root」，而 root 检测在本页（hasRoot / ksuWorking），
     * 父级拿不到 ⇒ 由本页把结论一起传上去，父级只做分支判断与弹窗。
     */
    onRunDfRoot: (variant: PayloadManager.KsuVariant, systemRooted: Boolean) -> Unit = { _, _ -> },
    /**
     * df 跑链进行中（2026-10-05 需求①）。
     *
     * <p>为 true 时，下面那张**设备信息卡**临时让位显示实时日志（[dfRootOutput]）；
     * 为 false 时保持设备信息（用户定调「操作时在卡内动态显示，不操作时保持现样」）。
     *
     * <p>为什么状态由父级持有：跑链入口在父级（[runDfRootChain]，主页按钮与变体切换
     * 确认窗共用），本页只负责渲染；且链在跑期间用户可能切到别的 tab 再切回来，
     * 状态必须活得比本页的组合生命周期长。
     */
    dfRootBusy: Boolean = false,
    /** df 跑链的实时日志文本（追加式；仅在 [dfRootBusy] 时展示）。 */
    dfRootOutput: String = "",
    /** dfe（DFReroot）**内联**跑链中（2026-10-07 新增）：与 [dfRootBusy] 共用同一张日志卡。 */
    dfeBusy: Boolean = false,
    /** dfe 内联跑链的实时日志文本（追加式；仅在 [dfeBusy] 时展示）。 */
    dfeOutput: String = "",
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var ksuWorking by remember { mutableStateOf(false) }   // KernelSU 内核模块是否已加载
    var hasRoot by remember { mutableStateOf(false) }      // 是否有 root 权限（与 KSU 解耦）
    var shizukuRunning by remember { mutableStateOf(false) }
    var shizukuAuthorized by remember { mutableStateOf(false) }
    var deviceInfo by remember { mutableStateOf<DeviceInfo?>(null) }
    // 当前**真正生效**的 KernelSU 变体（2026-10-02 起由 RootExecutor.detectKernelVariant
    // 读内核自报版本码判定，见 resolveWorkingVariant）。
    // 用途：状态卡「打开管理器」跳到对应那一支，而不是永远跳原生 KSU。
    var activeKsuVariant by remember { mutableStateOf<PayloadManager.KsuVariant?>(null) }

    // 2026-10-02：原 `showNextUnsupported` / `deviceSeries`（KSU-Next × S26 弹窗门禁）
    // 已随门禁取消一并移除——S26 上点 KernelSU-Next 与其它机型走同一路径。

    // ── 2026-10-02（二次修订）：F3 的「点击中」灰态防重入已撤销 ──
    //   原实现：点击后立刻置 actionBusy=true → 按钮 enabled=false 变灰，700ms 后自动复位。
    //   撤销原因：肉眼可感的变灰过程 + 灰态期间点击无效，在进入 DFReroot 之前形成一段
    //   人为延时，主观上就是"卡顿"。而两个按钮的目标动作（startActivity 拉起 DFReroot、
    //   或跳转向导页）本身都不需要防抖 —— 前者由系统 Activity 栈去重，后者是本地页面切换。
    //   结论：恢复为点击即响应，不做任何灰态/节流。

    // ── 2026-10-02 F5 修复：DF 直达入口的可用性 = 已生效 **且** 机型受支持 ──
    //   DeviceSupport.dfSupported() 在 S24(android14-6.1) 等机型上为 false。这类机型正常
    //   路径下装不上 DF（安装/启用前置均已门控），此处与运算为**防御性兜底**：一旦出现
    //   "不受支持机型却 dfActive=true" 的异常状态，按钮回退到「开始 Root 引导」，
    //   而不是带着无效变体去拉 DFReroot。（不在此处另弹"不支持"弹窗：入口按钮在不受支持
    //   机型上本就该回退到向导，DF 启用步骤在向导内部同样受 dfSupported() 门控。）
    //   机型支持度在进程生命周期内不变，用 remember 固定，避免每次重组都重跑内核串解析。
    val dfSupportedOnDevice = remember { DeviceSupport.dfSupported() }
    val dfEntryReady = dfActive && dfSupportedOnDevice

    // ── 2026-10-05：dfe 入口的**可用性**（叠加用户总开关）──
    // 与 df 侧 `dfRootReady = dfRootEnabled && dfRootSupportedOnDevice` 完全同构：
    //   ① dfEntryReady  = 客观能力（已生效 && 机型支持）
    //   ② dfEntryUsable = 用户意愿（总开关）&& 客观能力
    // 本页所有「dfe 分支」（按钮文案 + 点击目标 + 状态卡第四行）一律看 ②，
    // 这样关掉总开关后主页不再宣称「持久化可用」，与设置页 dfe 组整体消失一致。
    // 设置页「卸载 DFReroot」卡仍只看 ①（卸载是清理动作，不受「要不要用」影响）。
    val dfEntryUsable = dfEntryEnabled && dfEntryReady

    // ── df（DFRoot）入口可用性（round24，用户定调：「df 优先，设置里加 DF 总开关」）──
    // 判据：**总开关打开** 且 **机型支持**。
    // round19 用的是「已安装 df APK && 机型支持」——单包整合后 df 已内嵌本包，
    // 「装没装」不再是有效判据，故换成用户显式可控的总开关（关掉即回退 dfe / Root 向导，
    // 这也是用户能主动放弃 df 路径的唯一方式）。
    // 判据仍**不吸收** dfRootLaunchState（已注入/已 root 不改变按钮行为，沿用 round19 定调）。
    val dfRootSupportedOnDevice = remember { DeviceSupport.dfRootSupported() }
    val dfRootReady = dfRootEnabled && dfRootSupportedOnDevice

    // ── 2026-10-05：df 按钮小字的「简约态」判据 ──
    // 系统**尚未取得 root**（两种形态都算：完全没有 root / 有 root 但 KSU 未加载）时，
    // 红蓝两个按钮统一显示简约小字「一键 Root（DFRoot）」——
    // 此刻还没有「当前工作管理器」这个东西，「切换到 X」是句无意义的话
    // （旧逻辑在未 root 时因 activeKsuVariant==null 而走到 switch 分支，
    //   蓝按钮会显示「切换到 KernelSU-Next」，与事实不符）。
    // 已 root 时才恢复「当前变体 / 切换到另一变体」的差异化文案。
    val systemRooted = hasRoot || ksuWorking

    // 统一的检测刷新逻辑：主动读取 Shizuku 状态 + root 状态 + KSU 状态 + 设备信息。
    // 双维度检测：hasRoot（是否有 root）+ ksuWorking（KSU 是否已加载），
    // 支持主页状态卡片三态精确显示。
    suspend fun refreshStates() {
        withContext(Dispatchers.IO) {
            // 1. 主动检测 Shizuku 状态（覆盖用户在 Shizuku 应用里手动授权的场景）
            shizukuRunning = shizukuManager.isShizukuRunning()
            val actualAuthorized = shizukuManager.hasPermission()
            shizukuAuthorized = actualAuthorized

            // 2/3. 实时检测 root / KSU。现代 Samsung 上 untrusted_app 读不到 /proc/modules，
            //      这套 KSU 也不对普通应用响应 prctl，故未授权时这两项可能为 false（下面有多重兜底）。
            var hr = rootExecutor.hasRootAccess()
            var kw = rootExecutor.isKernelSUWorking()
            if (actualAuthorized && !kw) kw = rootExecutor.checkKernelSU()
            if (kw) hr = true

            // ── 兜底 ①（内核事实，round32 新增）：本 boot df 链已注入 ⇒ /dev/df 存在 ──
            //    /dev/df 由 df 的 shellcode 在 init(uid 0) 里以 O_CREAT|O_EXCL 创建，落在 tmpfs
            //    ⇒ **冷重启即消失**，是「本次开机已提权」最硬的证据，零权限、零副作用。
            //    ⚠️ 只能用 File.exists()（= access(F_OK)）探测：该节点的 SELinux 标签连 shell
            //    域的 stat 都拒（真机实测 `stat /dev/df` → Permission denied），而 access 不查
            //    目标节点的 getattr，故仍可用。
            if (!hr && com.samsung.rootkit.dfroot.DfRootBridge.isArmedThisBoot()) hr = true

            // ── 兜底 ②（用户态记录）：本 App 本次开机成功提权过（传统 Root 向导 / df 链都写）──
            if (!hr && com.samsung.rootkit.data.SettingsStore.isRootedThisBoot(context)) hr = true

            // ★ 第三态修正（round32）：**不再**在兜底命中时把 ksuWorking 一并置 true。
            //    标题与副标题的语义要求区分「已 root 但 KernelSU 未加载（缺管理器 / 未授权）」
            //    与「已 root 且 KernelSU 已加载」；旧写法 `if (isRootedThisBoot) kw = true` 会把
            //    前者误报成后者 —— 正是用户报的「系统已提权、无管理器」场景里的错误来源。
            //    而「装了管理器 ⇒ KernelSU 已加载」这条事实，此前**没有任何判据覆盖**
            //    （两个内核侧检测都只看 /proc、不看 PackageManager）⇒ 手动装完管理器状态永不刷新。
            //    现在补上：本 boot 确实注入过（hr）+ 管理器已就位 ⇒ KSU 生态成立。
            //
            //    ⚠️ 本推断**成立的前提是 hr 可信**（2026-10-07 修）：`hr` 的兜底②
            //    必须是「本次开机」权威标识全等（SettingsStore.isRootedThisBoot，boot_id /
            //    开机挂钟起点），而不是旧版那条 `elapsedRealtime` 单调性 —— 后者在冷重启
            //    后会把未 root 设备误判为「本次已提权」，于是下面这行便把**上次 root 装好、
            //    本次早已失效**的管理器 APK 当作「KSU 已加载」，主页错报态3。
            //    ⇒ 结论：管理器 APK **本身不足以**断言 KSU 已加载，它只是「hr 为真」时的
            //       补全项。任何把本行改成「装了管理器 ⇒ kw」的写法都会复现该 bug。
            if (hr && !kw) {
                kw = rootExecutor.isKsuManagerInstalled(rootExecutor.detectActiveKsuVariant())
            }

            hasRoot = hr
            ksuWorking = kw

            // 4. 设备信息无条件获取（getDeviceInfo 已改用 Build API，不依赖 Shizuku），
            //    这样「Shizuku 待授权」状态下也能立刻显示机型/固件版本/内核/Android。
            deviceInfo = try { rootExecutor.getDeviceInfo() } catch (_: Exception) { null }

            // 5. 判定当前生效的 KSU 变体 —— 全项目统一走 resolveWorkingVariant
            //    （最高优先级 = 内核自报版本码（ksud debug version）；其后 df-boot-ok
            //    → df-boot.log 加载记录 → 已安装唯一性，见 RootExecutor 注释；
            //    variant 键是待生效值不能用于判定当前工作，真机实锤）。
            activeKsuVariant = try {
                rootExecutor.resolveWorkingVariant()
            } catch (_: Exception) { null }

        }
    }

    // 首次进入时检测一次
    LaunchedEffect(Unit) {
        refreshStates()
    }

    // 关键修复：APP 从后台（如跳转 Shizuku 应用授权）切回前台时，LaunchedEffect(Unit) 不会重跑，
    // 导致 Shizuku/root 状态不刷新。用 ON_RESUME 生命周期监听，每次回到前台都重新检测。
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { refreshStates() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    // （修复①）原 ksuPkg / managerInstalled 已删除：状态卡点击改为「仅提权成功后跳当前生效变体」，
    // 未持权一律不跳，不再需要"装了管理器就跳"的判据。

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            // 标题区：纯文本，不加玻璃效果
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground_homepage),
                        contentDescription = null,
                        modifier = Modifier.size( 36.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                    )
                    Text("Samsung Root", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp))
                }
            }
        }

        item {
            val interactionSource = remember { MutableInteractionSource() }
            // Shizuku 就绪 = 进程运行中 且 已授权
            val shizukuReady = shizukuRunning && shizukuAuthorized
            // 待授权形态（未 root）：Shizuku 已运行但未授权
            val shizukuNeedAuth = !hasRoot && !ksuWorking && shizukuRunning && !shizukuAuthorized
            // Shizuku 未运行形态（未 root）：Shizuku 进程未运行
            val shizukuNotRunning = !hasRoot && !ksuWorking && !shizukuRunning

            // 三态判断（用户要求的精确状态）：
            //   态1: !hasRoot && !ksuWorking → "KernelSU 未加载，当前系统未ROOT"
            //   态2: hasRoot  && !ksuWorking → "KernelSU 未加载，ROOT为内存态，重启后失效"
            //   态3: hasRoot  && ksuWorking  → "KernelSU 已加载，ROOT为内存态，重启后失效"
            val stateHasKsu = hasRoot && ksuWorking       // 态3：KSU 已加载
            val stateHasRootNoKsu = hasRoot && !ksuWorking // 态2：有 root 但无 KSU

            // 用 GlassCard 统一玻璃描边效果，与页面其他卡片风格一致
            // clip 圆角确保 ripple 波纹不溢出到玻璃描边外部
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .clip(RoundedCornerShape(28.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                    ) {
                        clickHaptic(view)
                        // 修复①：状态卡点击跳转收紧 ——
                        //   提权成功（hasRoot，含"本次开机已成功提权"的内存态兜底）才允许跳转，
                        //   且跳的是**当前生效变体**对应的管理器；未持权 → 不做任何跳转。
                        //   （原先未持权时的三条分支：跳 Shizuku 应用 / 请求 Shizuku 授权 /
                        //    "装了管理器就跳"（managerInstalled）全部移除 —— 未持权时点了没有意义。）
                        if (hasRoot) {
                            fun openManager(preferred: PayloadManager.KsuVariant?) {
                                getInstalledKernelSUPackage(context, preferredVariant = preferred)
                                    ?.let { context.packageManager.getLaunchIntentForPackage(it) }
                                    ?.let { context.startActivity(it) }
                            }
                            val known = activeKsuVariant
                            if (known != null) {
                                // refreshStates()（进页 / 回前台）已判出当前生效变体 → 直接跳对应那支
                                openManager(known)
                            } else {
                                // 进页时没判出 → 点击时补判（与 refreshStates 同源：
                                // resolveWorkingVariant = 内核自报版本码 → df-boot-ok
                                // → df-boot.log 加载记录 → 已安装唯一性），
                                // 在 IO 线程判、判完回主线程再跳。
                                scope.launch(Dispatchers.IO) {
                                    val v = try { rootExecutor.resolveWorkingVariant() } catch (_: Exception) { null }
                                    withContext(Dispatchers.Main) {
                                        activeKsuVariant = v
                                        openManager(v)
                                    }
                                }
                            }
                        }
                    },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        when {
                            stateHasKsu -> Icons.Rounded.CheckCircle      // 态3：绿勾
                            stateHasRootNoKsu -> Icons.Rounded.Info         // 态2：信息蓝
                            else -> Icons.Rounded.Warning                   // 态1：警告黄
                        },
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            when {
                                // 2026-10-01 深夜：标题跟随实际生效变体 ——
                                //   Next 在工作 → "KernelSU-Next 已加载"，否则 "KernelSU 已加载"。
                                //   （activeKsuVariant 由 refreshStates 按 resolveWorkingVariant
                                //   判定：内核自报版本码 ksud 探测 → df-boot-ok → 已安装唯一性）
                                stateHasKsu && activeKsuVariant == PayloadManager.KsuVariant.KSU_NEXT ->
                                    strings.homeKsuNextLoaded
                                stateHasKsu -> strings.homeKsuLoaded           // "KernelSU 已加载"
                                else -> strings.homeKsuNotLoaded               // "KernelSU 未加载"（态1+态2共用标题）
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            when {
                                // 第二行：始终显示系统 root 状态（不混入 Shizuku 操作提示）
                                stateHasKsu || stateHasRootNoKsu -> strings.homeSystemRooted    // "当前系统已ROOT"
                                else -> strings.homeSystemNotRooted                              // "当前系统未ROOT"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = 0.86f),
                        )
                        // 第三行（2026-10-01 晚）：Shizuku 状态行已按需求移除——
                        // 顶部卡只关心 root/持久化结论，Shizuku 属于向导内部细节。

                        // 第三行（集成版 v5.1.0）：DFReroot 持久化状态。
                        // 三态与首页第三个入口的可用性判据一致：dfActive / 已装未生效 / 未配置。
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            // 2026-10-05：dfe 总开关关闭 ⇒ 显示「未配置」。
                            // 与 df 那一行的三态同构（`dfRootEnabled -> 已支持; else -> 未配置`）：
                            // 总开关表达的是「用户要不要持久化」，关掉即不做 —— 此刻若仍显示
                            // 「可用」，会与主页按钮（已回退到「开始 Root 引导」）自相矛盾。
                            val dfStatusText = when {
                                !dfEntryEnabled -> "${strings.dfStatusLabel}: ${strings.dfStatusNotConfigured}"
                                dfActive -> "${strings.dfStatusLabel}: ${strings.dfStatusActive}"
                                dfInstalled -> "${strings.dfStatusLabel}: ${strings.dfStatusInstalledNotActive}"
                                else -> "${strings.dfStatusLabel}: ${strings.dfStatusNotConfigured}"
                            }
                            Text(dfStatusText, style = MaterialTheme.typography.bodyMedium, color = LocalContentColor.current.copy(alpha = 0.7f))
                        }

                        // 第五行（2026-10-04 P3；round20 用户定调）：df（DFRoot）状态。
                        // 与上一行 DFReroot（dfe）状态行对称，**始终显示**，二态：
                        //   已安装 df → 「已支持」；未安装 → 「未配置」。
                        // round14-19 的三态（已注入/已安装/未安装）与「未装整行不显示」已废。
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            // round24：df 已内嵌 ⇒ 「已安装 / 未安装」不再是有效判据。
                            // 改为三态：机型不支持 / 总开关关（未配置）/ 启用中（已支持）。
                            val dfRootStatusText = when {
                                !dfRootSupportedOnDevice -> strings.dfRootStatusNotSupported
                                dfRootEnabled -> strings.dfRootStatusSupported
                                else -> strings.dfRootStatusNotConfigured
                            }
                            Text(
                                "${strings.dfRootStatusLabel}: $dfRootStatusText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LocalContentColor.current.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }

        item {
            // KSU（源版）：红色。三阶段状态机（round19 恢复 dfe 时期终态并接入 df）：
            //   ① df（DFRoot）可用（dfRootReady = 总开关开 && 机型支持，优先级最高）
            //      → 文案/点击走 df 分支（与 dfe 同构：当前工作变体 → 「重启免引导」；
            //      否则 → 「切换到 X」；点击 → onRunDfRoot：查 df 自启 → 需要时弹切换确认
            //      → **在本进程内跑链**）；
            //   ② dfe（DFReroot）持久化已生效（dfEntryUsable = 总开关 && 已生效 && 机型支持）
            //      → 文案/点击走 dfe 分支
            //      （点击 → onOpenDf：查 dfe 自启 → 需要时弹切换确认 → 跳 DFReroot）；
            //   ③ 两者皆无 → 「开始 Root 引导」，进 SRK 自己的向导（Shizuku 检查 → 选机型…）。
            // 历史：2026-10-01 晚双状态（dfe）；F4 显式 KsuRed；F2 按 activeKsuVariant 分文案。
            // round14-18 曾把 df 的跑链/已注入分支插到最前（变灰、Toast、静默记变体），
            // 真机验证行为混乱，round19 按用户定调全部废除重做。
            KsuVariantButton(
                bigText = strings.ksuVariantKsu,
                smallText = when {
                    // 未 root → 简约态（两按钮一致），见 systemRooted 注释
                    dfRootReady && !systemRooted -> strings.dfRootEntrySub
                    dfRootReady && activeKsuVariant == PayloadManager.KsuVariant.KSU ->
                        strings.dfRootEntrySub
                    dfRootReady -> strings.dfRootEntrySubSwitch(strings.ksuVariantKsu)
                    !dfEntryUsable -> strings.homeStartGuideSub
                    activeKsuVariant == PayloadManager.KsuVariant.KSU -> strings.dfEntrySub
                    else -> strings.dfEntrySubSwitch(strings.ksuVariantKsu)
                },
                accent = KsuRed,
            ) {
                when {
                    // 已 root 的处置在父级（需要自启态 + 当前工作管理器才判得出该不该弹切换窗），
                    // 故把本页已判出的 systemRooted 一起传上去（见 onRunDfRoot 注释）。
                    dfRootReady -> onRunDfRoot(PayloadManager.KsuVariant.KSU, systemRooted)
                    dfEntryUsable -> onOpenDf(PayloadManager.KsuVariant.KSU, systemRooted)
                    else -> onStartWizard(PayloadManager.KsuVariant.KSU, shizukuRunning && shizukuAuthorized)
                }
            }
        }

        item {
            // KSU-Next：蓝色。三阶段状态机（round19，同红按钮对称，见上）。
            // 注：原「S26 系列暂不适配」拦截弹窗已随门禁取消移除，
            //     Next 现在与 KSU 变体一样覆盖 S23 / S24 / S25 / S26 全系列。
            KsuVariantButton(
                bigText = strings.ksuVariantNext,
                smallText = when {
                    // 未 root → 简约态（两按钮一致），见 systemRooted 注释
                    dfRootReady && !systemRooted -> strings.dfRootEntrySub
                    dfRootReady && activeKsuVariant == PayloadManager.KsuVariant.KSU_NEXT ->
                        strings.dfRootEntrySub
                    dfRootReady -> strings.dfRootEntrySubSwitch(strings.ksuVariantNext)
                    !dfEntryUsable -> strings.homeStartGuideSub
                    activeKsuVariant == PayloadManager.KsuVariant.KSU_NEXT -> strings.dfEntrySub
                    else -> strings.dfEntrySubSwitch(strings.ksuVariantNext)
                },
                accent = KsuNextBlue,
            ) {
                when {
                    dfRootReady -> onRunDfRoot(PayloadManager.KsuVariant.KSU_NEXT, systemRooted)
                    dfEntryUsable -> onOpenDf(PayloadManager.KsuVariant.KSU_NEXT, systemRooted)
                    else -> onStartWizard(PayloadManager.KsuVariant.KSU_NEXT, shizukuRunning && shizukuAuthorized)
                }
            }
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                // ── df 跑链中：本卡临时让位给**实时日志**（2026-10-05 需求①）────────
                // 用户定调：「操作时在红蓝按钮下面的卡片内动态显示，不操作时保持现样」。
                // 于是判据就是 [dfRootBusy] —— 跑完（含失败/成功）立刻恢复设备信息，
                // 不在卡里留「结果页」（结局由 Toast 交代，完整日志在「运行记录」页）。
                // 位置就在两个 root 按钮正下方：点击按钮的手不用移动视线就能看到进度，
                // 这正是原弹窗做不到的（弹窗盖住整页、连按钮状态都看不见）。
                if (dfRootBusy || dfeBusy) {
                    // 2026-10-07：df 与 dfe 两条链**共用**这一张卡（谁在跑显示谁）。
                    // 文案分两组：df 组说 "DFRoot"，dfe 组说 "DFReroot"（两条链不同名，
                    // 混用会把 DFReroot 的进度误标成 DFRoot）。
                    DfRootLiveLogCard(
                        title = if (dfRootBusy) strings.dfRootRunTitle else strings.dfeRunTitle,
                        running = if (dfRootBusy) strings.dfRootRunning else strings.dfeRunning,
                        output = if (dfRootBusy) dfRootOutput else dfeOutput,
                    )
                } else {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        val di = deviceInfo
                        when {
                            di == null -> {
                                // 还在加载中；如果已经 root 则跳过读取（无需 Shizuku）
                                if (hasRoot || ksuWorking) {
                                    InfoLine(Icons.Rounded.Memory, strings.homeDevice, "—")
                                } else {
                                    InfoLine(Icons.Rounded.Info, strings.homeDevice, strings.homeLoading)
                                }
                            }
                            di.hasAny() -> {
                                // 设备字段：合并 model + board，仅两者都非空时拼接括号部分
                                val deviceLine = when {
                                    !di.model.isNullOrEmpty() && !di.board.isNullOrEmpty() -> "${di.model} (${di.board})"
                                    !di.model.isNullOrEmpty() -> di.model
                                    !di.board.isNullOrEmpty() -> di.board
                                    else -> null
                                }
                                deviceLine?.let { InfoLine(Icons.Rounded.Memory, strings.homeDevice, it) }
                                // 简化版设备信息：不显示完整指纹，只显示 Build ID 作为固件版本号
                                di.firmwareVersion?.takeIf { it.isNotEmpty() }?.let {
                                    InfoLine(Icons.Rounded.Code, strings.homeFirmwareVersion, it)
                                }
                                di.kernel?.takeIf { it.isNotEmpty() }?.let {
                                    InfoLine(Icons.Rounded.Info, strings.homeKernel, it)
                                }
                                di.sdk?.takeIf { it.isNotEmpty() }?.let {
                                    InfoLine(Icons.Rounded.VerifiedUser, strings.homeAndroidLabel, "API $it")
                                }
                            }
                            else -> {
                                // 全部无效（多半是未授权），显示一行友好提示
                                InfoLine(Icons.Rounded.Info, strings.homeDevice, strings.homeDeviceUnavailable)
                            }
                        }
                    }
                }
            }
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(strings.homeSafetyTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text(
                        strings.homeSafetyBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// ==================== Run Log Page ====================
/**
 * 结局的显示名。
 *
 * [RunOutcome.RUNNING] 不可能出现在列表里（`RunLogStore.finish` 会把非最终态强制转成 FAILED，
 * 且暂存记录只在进程启动时被 [RunLogStore.recoverPending] 转成 SUCCESS/ABORTED 后才进列表）。
 * 这里仍把它归到「中断」而不是 `else ->` 兜底：万一将来有人误写，显示成「中断」比显示成
 * 「成功/失败」都更接近事实。
 */
private fun runOutcomeLabel(outcome: RunOutcome, strings: AppStrings): String = when (outcome) {
    RunOutcome.SUCCESS -> strings.historySuccess
    RunOutcome.FAILED -> strings.historyFailed
    RunOutcome.RUNNING, RunOutcome.ABORTED -> strings.historyAborted
}

/**
 * 运行记录页（2026-10-05 起收纳**三条链**的记录）。
 *
 * <h2>收纳范围</h2>
 * 不再只有「SRK 向导的 root 尝试」，而是 SRK / df（DFRoot）/ dfe（DFReroot）三源共用一份存储
 * （见 [RunSource]）。每条记录带三态结局：成功 / 失败 / 运行中断（跑到一半进程就没了）。
 *
 * <h2>交互</h2>
 * - 单击 = 展开/收起该条的详细日志；
 * - **长按 = 弹出「导出 / 分享」**（原先设置页那张「导出运行日志」卡片已取消，功能整体搬到
 *   这里 —— 用户要的是「针对某一条记录」导出，而不是永远导出全部）。
 *   导出走 SAF（`ACTION_CREATE_DOCUMENT`，用户自选保存位置），分享走系统分享面板；
 *   两者都只写 cacheDir / 由系统托管的目标，全程不申请存储权限。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RunLogPage(modifier: Modifier = Modifier, context: Context) {
    val strings = LocalStrings.current
    val view = LocalView.current
    var attempts by remember { mutableStateOf(RunLogStore.loadAttempts(context)) }
    var expanded by remember { mutableStateOf<Int?>(null) }
    /** 长按命中的记录（null = 不显示操作弹窗）。 */
    var actionTarget by remember { mutableStateOf<RunAttempt?>(null) }
    /**
     * 待写入 SAF Uri 的报告文本。
     * 为什么要有这个中转：launcher 的回调只拿得到 Uri，拿不到「点击那一刻的记录」，
     * 而回调又在别的作用域里执行 —— 只能用状态把它带过去。
     */
    var pendingExportText by remember { mutableStateOf<String?>(null) }
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        val text = pendingExportText
        pendingExportText = null
        if (uri != null && text != null) {
            val ok = LogExporter.writeToUri(context, uri, text)
            android.widget.Toast.makeText(
                context,
                if (ok) strings.historyExportDone else strings.historyExportFailed,
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
    }

    actionTarget?.let { target ->
        GlassAlertDialog(
            onDismissRequest = { actionTarget = null },
            title = { Text(strings.historyTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        strings.historySourceName(target.source) + " · " +
                            runOutcomeLabel(target.outcome, strings),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        fmt.format(Date(target.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    )
                    if (target.detail.isNotEmpty()) {
                        Text(
                            target.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    GlassPillButton(
                        text = strings.commonCancel,
                        onClick = { clickHaptic(view); actionTarget = null },
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )
                    GlassPillButton(
                        text = strings.historyExport,
                        onClick = {
                                clickHaptic(view)
                                actionTarget = null
                                // 导出 = 让用户自选保存位置（SAF），落盘后给一次性提示。
                                pendingExportText = LogExporter.buildAttemptReport(context, target)
                                exportLauncher.launch(LogExporter.fileName("srk_run"))
                        },
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )
                    GlassPillButton(
                        text = strings.historyShare,
                        onClick = {
                                clickHaptic(view)
                                actionTarget = null
                                // 分享 = 先落到 cacheDir，再拉系统分享面板（微信/QQ/邮件均可）。
                                val report = LogExporter.buildAttemptReport(context, target)
                                val f = LogExporter.exportToFile(context, report, "srk_run")
                                if (f == null) {
                                    android.widget.Toast.makeText(
                                        context, strings.historyExportFailed, android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                                } else {
                                    LogExporter.share(
                                        context, f, strings.historyExportSubject, strings.historyShare,
                                    )
                                }
                        },
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pageHeaderIconCircle(Icons.Rounded.History)()
                        Text(strings.historyTitle, style = MaterialTheme.typography.headlineLarge)
                    }
                    if (attempts.isNotEmpty()) {
                        TextButton(onClick = {
                            clickHaptic(view)
                            RunLogStore.clear(context)
                            attempts = emptyList()
                        }) {
                            Text(strings.historyClear)
                        }
                    }
                }
                // 长按是隐藏操作，必须明确写出来，否则没人会去试。
                Text(
                    strings.historyLongPressHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (attempts.isEmpty()) {
            item {
                GlassCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(32.dp))
                    Column {
                        Text(strings.historyEmpty, style = MaterialTheme.typography.titleMedium)
                        Text(strings.historyEmptyDesc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            }
        } else {
            items(attempts.size) { idx ->
                val a = attempts[idx]
                val isExp = expanded == idx
                // 三态配色：成功=主色容器，失败=错误容器，中断=中性容器。
                // 中断单独给中性色（而不是并进失败）：它多半是「被软重启掐断」这类
                // 过程性事件，标成红色失败会把真正需要关注的失败淹没掉。
                val container = when (a.outcome) {
                    RunOutcome.SUCCESS -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    RunOutcome.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                }
                val tintIcon = when (a.outcome) {
                    RunOutcome.SUCCESS -> Icons.Rounded.CheckCircle
                    RunOutcome.FAILED -> Icons.Rounded.Cancel
                    else -> Icons.Rounded.Warning
                }
                val tintColor = when (a.outcome) {
                    RunOutcome.SUCCESS -> MaterialTheme.colorScheme.primary
                    RunOutcome.FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                // 外层用 GlassCard 提供统一的玻璃描边效果，与设置页卡片风格一致
                // clip 圆角确保 ripple 波纹不溢出到玻璃描边外部
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .clip(RoundedCornerShape(28.dp))
                        // 单击展开 / **长按弹「导出·分享」**。
                        // ⚠️ 这里刻意不传 interactionSource + indication（其它卡片是显式传的）：
                        //    组合点击的稳定重载只用 LocalIndication 取波纹，显式传参那一版在新版
                        //    Foundation 里已废弃；行为差异仅是波纹来源，观感一致。
                        .combinedClickable(
                            onLongClickLabel = strings.historyLongPressHint,
                            onLongClick = {
                                clickHaptic(view)
                                actionTarget = a
                            },
                        ) {
                            clickHaptic(view)
                            expanded = if (isExp) null else idx
                        },
                    cornerRadius = 28.dp,
                    // ★ round32：本页卡片**必须**关掉 per-card 实时模糊。
                    //   根因：GlassCard 的 blurBehind 默认是 true，而它每帧都要
                    //   layerBackdrop 捕获 + blur(16dp) + lens(24dp)；本页卡片展开日志后
                    //   高度可达数百行，模糊面积随之暴涨 ⇒ 稳定掉到 60fps
                    //   （用户报障：「点开日志详情时滑动变 60Hz」）。
                    //   本页背景是统一渐变，半透明填充本身已足够呈现玻璃观感，
                    //   关掉后观感几乎无差、滑动跟手。
                    blurBehind = false,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(container)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                tintIcon,
                                contentDescription = null,
                                tint = tintColor,
                                modifier = Modifier.size(30.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        runOutcomeLabel(a.outcome, strings),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    // 来源标签：SRK / DFRoot / DFReroot（三条链共用一页，必须能区分）
                                    // ⚠️ 必须 maxLines=1 + softWrap=false：本行是 Row（结局文字 + 本标签），
                                    //    窄屏/长文案时 Compose 会把标签按"能塞多少塞多少"逐字换行
                                    //    （真机实证：「DFRoot」被折成「DFR」+「oot」两排，见 round28 报障）。
                                    //    标签是固定品牌名，不允许折行；宁可让它整体占宽。
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                                    ) {
                                        Text(
                                            strings.historySourceName(a.source),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false,
                                        )
                                    }
                                }
                                Text("${a.model} · ${a.firmware}", style = MaterialTheme.typography.bodySmall, color = LocalContentColor.current.copy(alpha = 0.8f))
                                // 结果原因一行（失败原因 / 中断原因 / 成功摘要）；为空则不占位。
                                if (a.detail.isNotEmpty()) {
                                    Text(
                                        a.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LocalContentColor.current.copy(alpha = 0.7f),
                                    )
                                }
                            }
                            Text(fmt.format(Date(a.timestamp)), style = MaterialTheme.typography.labelMedium, color = LocalContentColor.current.copy(alpha = 0.7f))
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = LocalContentColor.current.copy(alpha = 0.6f),
                            )
                        }
                        if (isExp && a.logLines.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                            ) {
                                Text(
                                    a.logLines.joinToString("\n"),
                                    modifier = Modifier.padding(12.dp),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// ==================== Settings Page ====================
@Composable
fun SettingsPage(
    modifier: Modifier = Modifier,
    context: Context,
    themeModeState: MutableState<AppThemeMode>,
    dynamicColorState: MutableState<Boolean>,
    onReenterDisclaimer: () -> Unit,
    onLanguageChanged: (String) -> Unit,
    // DFReroot 卸载（集成版 v5.1.0）：状态只用于显示"已安装/未安装"与禁用条件，
    // 真正执行在父级的弹窗里（SettingsPage 不直接依赖 DfRunner 与协程作用域）。
    dfInstalled: Boolean = false,
    dfActive: Boolean = false,
    // 2026-10-02 F8 修复：DF 入口是否**真正可用** —— 由父级按「已生效(dfActive) 且 机型受支持
    // (DeviceSupport.dfSupported())」算好传入。本页不直接依赖 DeviceSupport（UI 层无判定逻辑）。
    // 不受支持机型上即使 dfActive=true（跨版本遗留）也显示「未配置」、点击走配置分支，
    // 由父级 onConfigureDf 弹「暂不适配」—— 与主页两个按钮同一判据，不再分叉。
    dfEntryReady: Boolean = false,
    // ── dfe（DFReroot）总开关（2026-10-05）──
    // 与 df 组的 dfRootEnabled 同构的**用户意愿**主开关：关闭 ⇒ 「配置 DFReroot」卡不渲染、
    // 主页按钮的 dfe 分支失效、root 成功后不弹持久化引导。
    // 与 dfEntryReady 的分工：本项 = 意愿；dfEntryReady = 客观能力（已生效 && 机型支持），
    // 后者继续单独门控「卸载 DFReroot」卡 —— 关掉总开关也必须留一条卸载已装 DFReroot 的路。
    dfEntryEnabled: Boolean = true,
    onDfEntryEnabledChange: (Boolean) -> Unit = {},
    // ── dfe（DFReroot）的「开机自启动」（2026-10-05 新增）──
    // 三态：null = 读不到（无 su 授权 / 文件不存在）⇒ 卡片禁用（状态没读到就不许拨）。
    // true/false = `/data/system/df-boot-prefs` 的 autostart，与 DFReroot 自身界面同源。
    // 父级负责经 su 读写（本页不依赖 RootExecutor）。
    dfEntryAutostart: Boolean? = null,
    onDfEntryAutostartChange: (Boolean) -> Unit = {},
    // dfe 的「D2 修复」（2026-10-07 新增，用户需求：与 DFReroot 的 D2 开关联动）。
    // 与 dfEntryAutostart 同构：三态（null = 读不到 ⇒ 卡片禁用），值来自
    // /data/system/df-boot-prefs 的 d2fix 键（父级负责经 su 读写，本页不依赖 RootExecutor）。
    dfEntryD2Fix: Boolean? = null,
    onDfEntryD2FixChange: (Boolean) -> Unit = {},
    // dfe 的「自动软重启」（2026-10-08 新增，用户需求：与 DFReroot 的自动软重启开关联动）。
    // 与 dfEntryAutostart / dfEntryD2Fix 同构：三态（null = 读不到 ⇒ 描述改「读取失败」），
    // 值来自 /data/system/df-boot-prefs 的 softreboot 键（父级经 su 读写）。
    // 卡片位置 = 「开机自启动」**下面**（用户定）；显隐 = 随 dfe 总开关（见渲染处）。
    dfEntryAutoSoftReboot: Boolean? = null,
    onDfEntryAutoSoftRebootChange: (Boolean) -> Unit = {},
    // 密钥是否已注入 packages.xml；null=未知（无 root / 检测失败）。
    // 修复⑤：设置行按「密钥 × DFReroot 安装」区分 未配置 / 已注入\未配置 / 已配置。
    dfKeyInjected: Boolean? = null,
    onUninstallDf: () -> Unit = {},
    // 「打开 DFReroot」入口（2026-10-01）：主页入口迁入高级分区，点击直达 DFReroot UI。
    onOpenDf: () -> Unit = {},
    // 「配置 DFReroot」入口（2026-10-01 深夜）：DFReroot 未加载（dfActive=false）时，
    // 点击「打开 DFReroot」卡片改为弹配置弹窗（复用 root 完成后的 showDfEnableDialog）。
    onConfigureDf: () -> Unit = {},
    // ── df（DFRoot / `df.root`）区（2026-10-04 P3 / S6）──
    // ⚠️ 上面的 df* 全是 dfe（DFReroot）；本组 dfRoot* 才是 df（DFRoot）。
    // 区渲染门控用静态判据（函数体内 dfRootSupportedOnDevice，见区内注释）——
    // 不吃 dfRootLaunchState：launchState==UNSUPPORTED 可能来自 S7 实测失败，
    // 此时仍须保留重试入口。
    /** DFRoot 总开关（本区第一张卡；关掉后另两张开关不渲染）。 */
    dfRootEnabled: Boolean = false,
    dfRootAutostart: Boolean = false,
    dfRootAutoSoftReboot: Boolean = false,
    onDfRootEnabledChange: (Boolean) -> Unit = {},
    onDfRootAutostartChange: (Boolean) -> Unit = {},
    onDfRootAutoSoftRebootChange: (Boolean) -> Unit = {},
) {
    val strings = LocalStrings.current
    val view = LocalView.current
    val density = LocalDensity.current
    // df（DFRoot）机型静态预判（= !isS24()，拍板④）。DFRoot 区渲染门控用它
    // 而不是 dfRootLaunchState：launchState==UNSUPPORTED 可能来自 S7 实测失败
    // （静态仍支持，须保留重试入口），两种「不支持」语义必须分开（见区内注释）。
    val dfRootSupportedOnDevice = remember { DeviceSupport.dfRootSupported() }
    var showAbout by remember { mutableStateOf(false) }
    // dfe 总开关「已配置不可关闭」提醒弹窗（2026-10-07 用户定稿交互）：
    // 由下方那张总开关卡的 onCheckedChange 在「已配置 + 请求关闭」时置 true。
    var showDfEntryLockDialog by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var languageMenuTop by remember { mutableStateOf(32.dp) }
    // 2026-10-05：原「导出运行日志」卡片（含此处统计的记录条数 logCount）已取消 ——
    // 该功能整体移入「运行记录」页（长按任意记录 → 导出 / 分享）。
    val currentLanguage = SettingsStore.getLanguage(context)

    if (showLanguageMenu) {
        SideChoiceMenu(
            choices = languageOptions.map { it.first },
            selectedIndex = languageOptions.indexOfFirst { it.second == currentLanguage }.coerceAtLeast(0),
            topOffset = languageMenuTop,
            onSelected = { index ->
                showLanguageMenu = false
                val tag = languageOptions[index].second
                SettingsStore.setLanguage(context, tag)
                applyLanguage(tag) { onLanguageChanged(tag) }
            },
            onDismiss = { showLanguageMenu = false },
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    // ── dfe 总开关「不可关闭」提醒（2026-10-07 用户定稿交互）──────────────
    // 已配置 dfe 持久化时，总开关**不置灰**（置灰只让人困惑、不知为何点不动），
    // 而是保持可点击 —— 点「关」时弹本窗说明原因，并给出唯一出路：先卸载 DFReroot。
    if (showDfEntryLockDialog) {
        GlassAlertDialog(
            onDismissRequest = { showDfEntryLockDialog = false },
            title = { Text(strings.settingsDfEntryEnabledLockedTitle) },
            text = { Text(strings.settingsDfEntryEnabledLockedBody) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    GlassPillButton(
                        text = strings.commonConfirm,
                        onClick = { showDfEntryLockDialog = false },
                        isPrimary = false,
                        modifier = Modifier.widthIn(min = 148.dp),
                    )
                }
            },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 100.dp),
        // 组间间距（= 本页 LazyColumn 的 item 间距）刻意取大于组内的 settingsCardGap，
        // 「哪些卡属于同一组」靠这个间距差一眼可辨（见 settingsGroupGap 注释）。
        verticalArrangement = Arrangement.spacedBy(settingsGroupGap),
    ) {
        item {
            Column(modifier = Modifier.padding(top = 20.dp, bottom = 0.dp)) {
                Text(strings.settingsTitle, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            }
        }

        // ── 2026-10-05：恢复「分组卡片流」（用户定调 4 组，组名 + 组间留白双重区分）────
        // 历史：2026-10-02 第 1 轮取消分栏标题、第 2 轮把组内/组间间距压成同一个
        //       settingsCardGap ⇒ 分组的视觉线索一并消失，整页变成「一长串等距卡片」。
        // 现状（2026-10-05 二次修订）：**显式加回组名**（[SettingsGroupLabel]），留白只作辅助。
        // 组名按用户指定，依次为：通用 / DFRoot / DFReroot / 其它。
        //   组1 通用    ：主题三按钮 → 使用系统动态颜色 → 语言
        //   组2 DFRoot  ：DFRoot 总开关 → 开机自启 → 自动软重启（df，内嵌引擎）
        //   组3 DFReroot：DFReroot 总开关 → 配置 DFReroot → 卸载 DFReroot（仅已生效时）
        //   组4 其它    ：重新阅读免责声明 → 关于
        // 两组顺序：**dfe 组在 df 组之后**（用户定调）—— 主页按钮已由 df 优先，
        // 设置页同样把「主力路径（df）」排在「持久化补充（dfe）」前面，与主页认知一致。
        // 另：原「导出运行日志」卡片已取消，功能移入「运行记录」页长按弹窗。
        item {
            Column(verticalArrangement = Arrangement.spacedBy(settingsCardGap)) {
                SettingsGroupLabel(strings.settingsGeneral)
                ThemeModeSelector(themeModeState.value) { mode ->
                    themeModeState.value = mode
                    SettingsStore.setThemeMode(context, mode)
                }
                SettingsSwitchCard(
                    icon = Icons.Rounded.VerifiedUser,
                    title = strings.settingsDynamicColor,
                    description = strings.settingsDynamicColorDesc,
                    checked = dynamicColorState.value,
                    position = SettingsCardPosition.Single,
                    compact = true,
                    onCheckedChange = {
                        dynamicColorState.value = it
                        SettingsStore.setDynamicColor(context, it)
                    },
                )
                // 圆角与相邻卡片一致：四个角都用 Single(16dp)，
                // 不用连接组的 Top/Middle/Bottom（中间卡片会被压成 6dp 方角，观感不一致）。
                SettingsCard(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        languageMenuTop = with(density) { coordinates.positionInWindow().y.toDp() }
                    },
                    icon = Icons.Rounded.Language,
                    title = strings.settingsLanguage,
                    description = strings.settingsLanguageDesc,
                    value = languageLabel(currentLanguage, strings),
                    position = SettingsCardPosition.Single,
                    compact = true,
                    onClick = { clickHaptic(view); showLanguageMenu = true },
                )
            }
        }

        // ── 组2：df（DFRoot / `df.root`）区（round24：单包内嵌后只剩开关）──
        // 卡片构成（round24 用户定调「去掉安装/卸载，保留开关」＋「设置里加 DF 总开关」）：
        //     DFRoot 总开关 → 开机自启 → 自动软重启
        // 相对 round20 的删除项：**「打开/安装 DFRoot」与「卸载 DFRoot」两张卡**。
        //   理由：df 已内嵌本包 ⇒ 没有独立包可装可卸，「打开它的界面」这个动作也不存在了
        //   （主页按钮改为直接跑链）。这两张卡历史上承载的「重试入口 / 装了卸不掉」
        //   等问题随之自然消失，不再需要「残留安装也必须能卸载」这类补丁式条件。
        //
        // 整区门控：**静态机型判据** dfRootSupportedOnDevice（= !isS24()，拍板④）。
        //   不再有「|| 已安装」的放宽分支 —— 那是为「静态不支持却残留装着 df」的机型
        //   留的卸载入口，单包后不存在该状态。
        //   ⚠️ 不用 dfRootLaunchState：launchState==UNSUPPORTED 可能来自 S7 实测失败，
        //   而静态仍支持的机型必须保留开关（重试成功后即生效）。
        if (dfRootSupportedOnDevice) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(settingsCardGap)) {
                    SettingsGroupLabel(strings.settingsDfRootGroup)
                    // 总开关始终渲染：它是本区「怎么用 df」的唯一入口，也是用户主动
                    // 放弃 df 路径（回退 dfe / Root 向导）的唯一手段。
                    SettingsSwitchCard(
                        icon = Icons.Rounded.Security,
                        title = strings.settingsDfRootEnabled,
                        description = strings.settingsDfRootEnabledDesc,
                        checked = dfRootEnabled,
                        position = SettingsCardPosition.Single,
                        compact = true,
                        onCheckedChange = { clickHaptic(view); onDfRootEnabledChange(it) },
                    )
                    // 两张行为开关仅在总开关打开时渲染：总开关关掉后跑链根本不会发生，
                    // 自启与软重启都没有意义，留着只会让用户以为「开了却没用」。
                    // （值本身不会被清掉 —— 关总开关只是不渲染，再打开还是原值；
                    //   开机自启的组件态由 DfRootPrefs 按「总开关 && 自启」派生，已被停用。）
                    if (dfRootEnabled) {
                        SettingsSwitchCard(
                            icon = Icons.Rounded.Refresh,
                            title = strings.settingsDfRootAutostart,
                            description = strings.settingsDfRootAutostartDesc,
                            checked = dfRootAutostart,
                            position = SettingsCardPosition.Single,
                            compact = true,
                            onCheckedChange = { clickHaptic(view); onDfRootAutostartChange(it) },
                        )
                        SettingsSwitchCard(
                            icon = Icons.Rounded.Timer,
                            title = strings.settingsDfRootAutoSoftReboot,
                            description = strings.settingsDfRootAutoSoftRebootDesc,
                            checked = dfRootAutoSoftReboot,
                            position = SettingsCardPosition.Single,
                            compact = true,
                            onCheckedChange = { clickHaptic(view); onDfRootAutoSoftRebootChange(it) },
                        )
                    }
                }
            }
        }

        // ── 组3：dfe（DFReroot）区 ──
        // ⚠️ 与上面的 dfRoot* 不是同一个东西：dfe 是**独立 App** 的持久化侧
        //   （com.polygraphene.df.reroot）。两者分区相邻便于区分，但互不干扰。
        //
        // 卡片构成（2026-10-08 更新）：
        //   DFReroot 总开关                           门控 = （本组始终渲染）
        //   → 配置 DFReroot                           门控 = 总开关
        //   → 开机自启动 · 自动软重启 · D2 修复         门控 = dfEntryReady && 总开关（三张一起显隐）
        //   → 卸载 DFReroot                           门控 = dfEntryReady（**有意不吃总开关**）
        // ※ 2026-10-08：中间那三张行为卡由「只看 dfEntryReady」收紧为「dfEntryReady && 总开关」
        //   ——用户要求「dfe 总开关关闭，这些按钮也隐藏」（自动软重启卡是本轮新增的那一张）。
        //   卸载卡**不动**：关掉总开关后若 DFReroot 还装着，必须留一条卸掉它的路。
        //   · 总开关：与 df 组同构的「用户意愿」主开关。关掉即「不要持久化 root」——
        //     主页两个按钮的 dfe 分支失效（回退 Root 向导）、root 成功后不再弹持久化引导。
        //   · 配置 DFReroot：门控 = 总开关打开（用户要求「总开关关了这张卡不显示」）。
        //     功能不变：已生效 → 直达 DFReroot UI；未生效 → 弹配置弹窗（showDfEnableDialog）；
        //     机型不支持 → 弹「暂不适配」。
        //   · 卸载 DFReroot：门控仍是**客观能力** dfEntryReady，有意不叠加总开关 ——
        //     用户关掉持久化后若 DFReroot 还装着，必须留一条卸掉它的路，
        //     否则就成了「装了卸不掉」。
        item {
            Column(verticalArrangement = Arrangement.spacedBy(settingsCardGap)) {
                SettingsGroupLabel(strings.settingsDfRerootGroup)
                // ── DFReroot 总开关（2026-10-05 新增；dfe 组第一张卡，与 df 组同构）──
                //
                // ★ 2026-10-07 需求④（交互改版）：**dfe 持久化已配置好时，本开关不允许关闭** ——
                //   但**不再置灰**（用户反馈：灰掉只让人困惑，不知道为何点不动），
                //   而是保持可点击：点「关」时弹窗说明原因（"…没卸载，不支持关闭，请卸载后再关闭"）。
                //   （用户原话：「不是直接灰色不可用，是按这个按钮时弹窗提醒」。）
                //   · 判据取 dfEntryReady（= dfActive && 机型支持），即「持久化已生效」——
                //     与「配置 DFReroot」卡状态行、主页按钮小字同一事实源。
                //   · 只拦**开 → 关**这一个方向：故条件是 `!next && dfEntryReady`。
                //     反向（关 → 开）永远放行 —— 否则遇到「历史遗留：已配置但开关是关的」
                //     就变成**开也开不了、关也关不了**的死锁卡。
                //   · 弹窗关闭后**不回调** onDfEntryEnabledChange ⇒ 开关视觉保持「开」。
                //   · 父级 setDfEntryEnabled 里另有一道同名守卫兜底（防其它入口绕过 UI 关闭）。
                SettingsSwitchCard(
                    icon = Icons.Rounded.Lock,
                    title = strings.settingsDfEntryEnabled,
                    description = strings.settingsDfEntryEnabledDesc,
                    checked = dfEntryEnabled,
                    position = SettingsCardPosition.Single,
                    compact = true,
                    onCheckedChange = { next ->
                        clickHaptic(view)
                        if (!next && dfEntryReady) showDfEntryLockDialog = true
                        else onDfEntryEnabledChange(next)
                    },
                )
                if (dfEntryEnabled) {
                    // ── 配置 DFReroot（2026-10-01 自主页入口迁入设置页；
                    //    2026-10-05 由「打开 DFReroot」改名，功能不变）──
                    // 点击会把 SRK 当前选中的 root 变体（首页 KSU / KSU-Next 二选一）经
                    // Intent extra 传给 DFReroot，实现「SRK 选哪个、DFReroot 就用哪个」。
                    // 2026-10-01 深夜：卡片增加状态识别 —— DFReroot 已加载显示「可用」、
                    // 未加载显示「未配置」；未配置时点击改为弹配置弹窗（复用 root 完成后的
                    // showDfEnableDialog），已加载才直达 DFReroot UI。
                    // 2026-10-02 F8：状态行与点击分支统一改用 dfEntryReady（= 已生效 且 机型受支持），
                    // 不再单独看 dfActive —— 否则 S24 上会显示「可用」并能打开 DFReroot，
                    // 与主页按钮（同状态下回退 Root 向导）互相矛盾。
                    SettingsCard(
                        icon = Icons.Rounded.OpenInNew,
                        title = strings.settingsDfOpen,
                        description = strings.settingsDfOpenDesc,
                        value = if (dfEntryReady) strings.dfStatusActive else strings.dfStatusNotConfigured,
                        position = SettingsCardPosition.Single,
                        compact = true,
                        onClick = {
                            clickHaptic(view)
                            if (dfEntryReady) onOpenDf() else onConfigureDf()
                        },
                    )
                }
                // ── 开机自启动（2026-10-05 新增，用户需求：与 DFReroot 里的自启开关联动）──
                // 这张卡是**对端状态的镜像**，不是本应用自己的设置：
                //   值来自 /data/system/df-boot-prefs 的 autostart（经 su 读），
                //   拨动经 su 写回 DFReroot 的状态通道 —— 与它界面上的开关是同一份状态。
                //
                // ★ 2026-10-08 门控收紧（用户定调）：由 `dfEntryReady` 改为
                //   **`dfEntryReady && dfEntryEnabled`** —— dfe 总开关关掉时，
                //   「开机自启动 / 自动软重启 / D2 修复」**三张行为卡一起隐藏**。
                //   历史沿革：旧注释曾说明这三张卡"有意不吃总开关"，理由是 DFReroot 的
                //   boot receiver 在它自己包里、SRK 关不掉它 ⇒ 必须留一条能关的路。
                //   用户 2026-10-08 明确要求「dfe 总开关关闭，这些按钮也隐藏」，故改之。
                //   该顾虑不构成阻塞：总开关**永远可以再打开**（setDfEntryEnabled 只拦
                //   「开→关」且仅在 dfe 持久化已配置时弹窗提醒），打开后三张卡立刻回来。
                // enabled：**只要 DFReroot 已生效（即本 if 门控成立）就可拨** —— 2026-10-07 用户反馈修正。
                //   原判据 `enabled = (dfEntryAutostart != null)`（读到值才允许拨）会让
                //   「刚装好 DFReroot、`df-boot-prefs` 尚未生成」或「su 一时读不到」的卡片**灰掉**，
                //   用户反馈「只要 DFReroot 安装成功就该可用，现在有时候会显示灰色」。
                //   现改为不置灰：状态读不到时描述仍如实提示「读取失败」，但允许点击尝试拨动 ——
                //   写入路径（setDfAutostart）自己会重新探测 su，失败另有 Toast 兜底，
                //   比一张"点不动的死卡"更有出路。
                //   「没 root 时也不可用」由外层门控保证：装不上 DFReroot 就进不来。
                if (dfEntryReady && dfEntryEnabled) {
                    SettingsSwitchCard(
                        icon = Icons.Rounded.Refresh,
                        title = strings.settingsDfEntryAutostart,
                        description = if (dfEntryAutostart == null)
                            strings.settingsDfEntryAutostartUnavailable
                        else strings.settingsDfEntryAutostartDesc,
                        checked = dfEntryAutostart == true,
                        position = SettingsCardPosition.Single,
                        compact = true,
                        onCheckedChange = { clickHaptic(view); onDfEntryAutostartChange(it) },
                    )
                    // ── 自动软重启（2026-10-08 新增，用户需求：与 DFReroot 的自动软重启开关联动）──
                    // 与上面那张自启卡**完全同构**：同一门控（dfEntryReady && dfEntryEnabled）、
                    // 同一三态语义（null ⇒ 描述改「读取失败」）、同一份状态文件
                    // （/data/system/df-boot-prefs，键 softreboot，与 autostart/d2fix 同一次写入）。
                    // ★ 位置：**紧跟在「开机自启动」下面**（用户原话「加在开机自动启动下面」）。
                    //   · 语义 = 跑完提权链（late-load 成功）后自动软重启一次，让 LSPosed 生效；
                    //     **默认关**；唯一消费方是 DFReroot 的链脚本（stage1.S 的 chain_sh 直接
                    //     grep 该键决定要不要给 late-load 追加 --soft-reboot）——SRK 侧不做任何
                    //     重启动作，也没有"立即生效"的即时反馈：下一次跑链（手动 / 开机自启）才生效。
                    //   · enabled：与自启卡同一条 2026-10-07 修正 —— 不按「读到值才可拨」置灰。
                    SettingsSwitchCard(
                        icon = Icons.Rounded.Timer,
                        title = strings.settingsDfEntryAutoSoftReboot,
                        description = if (dfEntryAutoSoftReboot == null)
                            strings.settingsDfEntryAutoSoftRebootUnavailable
                        else strings.settingsDfEntryAutoSoftRebootDesc,
                        checked = dfEntryAutoSoftReboot == true,
                        position = SettingsCardPosition.Single,
                        compact = true,
                        onCheckedChange = { clickHaptic(view); onDfEntryAutoSoftRebootChange(it) },
                    )
                    // ── D2 修复（2026-10-07 新增，用户需求：与 DFReroot 的 D2 开关联动）──
                    // 与上面两张卡**完全同构**：同一门控、同一三态语义、同一份状态文件
                    // （/data/system/df-boot-prefs，键 d2fix）。
                    //   · 门控取 dfEntryReady && dfEntryEnabled ⇒ 未配置或总开关关掉时本卡不渲染
                    //     （用户 2026-10-08 定调：三张行为卡一起随总开关隐藏）。
                    //   · enabled：与自启卡同一条 2026-10-07 修正 —— **不再按「读到值才可拨」置灰**，
                    //     只要 DFReroot 已生效就可拨（读不到时描述提示「读取失败」，写入另有 Toast 兜底）。
                    //     D2 与 root 链路**无依赖关系**（只写一个 vault 标志位解 Odin 下载模式的锁），
                    //     故这里不做「需要先 root」的额外提示。
                    SettingsSwitchCard(
                        icon = Icons.Rounded.BugReport,
                        title = strings.settingsDfEntryD2Fix,
                        description = if (dfEntryD2Fix == null)
                            strings.settingsDfEntryD2FixUnavailable
                        else strings.settingsDfEntryD2FixDesc,
                        checked = dfEntryD2Fix == true,
                        position = SettingsCardPosition.Single,
                        compact = true,
                        onCheckedChange = { clickHaptic(view); onDfEntryD2FixChange(it) },
                    )
                }
                // ── 卸载 DFReroot（2026-09-26 迁入设置页）──
                // 卸载属系统级操作（先 pm uninstall，再改 /data/system/packages.xml），
                // 点击只打开父级的确认弹窗，真正执行不在本页。
                // round20 用户定调：**与 df 区自动软重启卡同模式 —— dfe 持久化未配置好时
                // 本卡不渲染**（判据 = dfEntryReady = dfActive && dfSupportedOnDevice）。
                // 2026-10-05 补充：本卡**不**吃 dfe 总开关 —— 总开关回答「要不要用」，
                // 本卡回答「怎么把已经用过的清理掉」，两者正交（关掉开关也必须能卸）。
                // 历史上的修复⑦「无条件渲染」（为「密钥已注入但 dfe 未装」保留移密钥入口）
                // 仍废除 —— 那个场景如今由「配置 DFReroot」卡的配置链路（含失败重试）覆盖。
                if (dfEntryReady) {
                    SettingsCard(
                        icon = Icons.Rounded.LockOpen,
                        title = strings.settingsDfUninstall,
                        description = strings.settingsDfUninstallDesc,
                        // 修复⑤ 状态行三态（按「密钥是否已注入 packages.xml × DFReroot 是否安装」）：
                        //   密钥不在 + DF 未装                    → 未配置
                        //   密钥已在 + DF 未装                    → 已注入\未配置（双排）
                        //   密钥已在 + DF 已装（含已生效）        → 已配置
                        // 密钥状态未知（无 root / 检测失败）时回退旧行为：只看 dfActive/dfInstalled。
                        value = when {
                            dfActive -> strings.dfStatusActive
                            dfInstalled && dfKeyInjected != false -> strings.dfStatusConfigured
                            !dfInstalled && dfKeyInjected == true ->
                                strings.dfStatusInjected + "\n" + strings.dfStatusNotConfigured
                            dfInstalled -> strings.dfStatusInstalledNotActive
                            else -> strings.dfStatusNotConfigured
                        },
                        valueMaxLines = 2,
                        position = SettingsCardPosition.Single,
                        compact = true,
                        onClick = {
                            clickHaptic(view)
                            onUninstallDf()
                        },
                    )
                }
            }
        }

        // ── 组4「其它」：把「重新阅读免责声明」「关于」放在设置页最下面（2026-10-02 需求 ③）──
        item {
            Column(verticalArrangement = Arrangement.spacedBy(settingsCardGap)) {
                SettingsGroupLabel(strings.settingsOther)
                // 「清理日志」卡片（2026-10-01 晚）已按需求移除。
                SettingsCard(
                    icon = Icons.Rounded.Description,
                    title = strings.settingsReDisclaimer,
                    description = strings.settingsReDisclaimerDesc,
                    value = "",
                    position = SettingsCardPosition.Single,
                    compact = true,
                    onClick = { clickHaptic(view); onReenterDisclaimer() },
                )
                SettingsCard(
                    icon = Icons.Rounded.Info,
                    title = strings.settingsAbout,
                    description = strings.settingsAboutDesc,
                    value = "",
                    position = SettingsCardPosition.Single,
                    compact = true,
                    onClick = { clickHaptic(view); showAbout = true },
                )
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// ── 设置页卡片间距（2026-10-05 恢复分组）──────────────────────────────────────
// 组**内**相邻卡片的间距。设置页所有组内 Column 都引用它。
private val settingsCardGap = 14.dp

// 组**间**间距 = 设置页 LazyColumn 的 item 间距（每个「组」就是一个 item，
// 组内是一个 Column(spacedBy(settingsCardGap))）。
//
// 2026-10-05 二次修订：组名（[SettingsGroupLabel]）已显式加回，分组不再**只**靠留白。
// 该值仍比 settingsCardGap 大一截，作用变成「把上一组最后一张卡与下一组组名拉开」，
// 取值 26dp ≈ 卡片高度(约 64dp)的 40%，肉眼可辨为「另一组」而不至于松散。
private val settingsGroupGap = 26.dp

/**
 * 设置页**分组标题**（2026-10-05 按用户要求恢复显式组名）。
 *
 * <p>历史：2026-10-02 第 1 轮取消分栏标题、第 2 轮又把组内/组间间距压成同一个值 ⇒
 * 分组信息彻底丢失，整页退化成一长串等距卡片。本轮把组名加回来。
 *
 * <p>样式沿用项目原版 SectionLabel 观感（labelLarge + primary 色 + 左缩进 18dp
 * 与卡片内容对齐），只把上下留白收紧：与上一组的距离交给 LazyColumn 的 item 间距
 * （[settingsGroupGap]），与组内首张卡的距离交给外层 Column 的 spacedBy
 * （[settingsCardGap]）⇒ 标题视觉上"贴"在自己的组上，不会像上一组的尾巴。
 *
 * <p>注意：本函数是**组内 Column 的第一个子项**（不是独立 item），
 * 因此组名的显隐天然跟随该组的渲染门控（如 df 组 / 卸载卡的 if）。
 */
@Composable
private fun SettingsGroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 18.dp, top = 4.dp),
    )
}

private val languageOptions = listOf(
    "跟随系统" to "",
    "中文" to "zh",
    "English" to "en",
)

private fun languageLabel(tag: String, strings: AppStrings): String = when (tag) {
    "system", "" -> strings.langFollowSystem
    "zh" -> strings.langChinese
    "en" -> strings.langEnglish
    else -> tag
}

@Composable
private fun ThemeModeSelector(
    themeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit,
) {
    val view = LocalView.current
    val strings = LocalStrings.current
    val themeModes = AppThemeMode.entries
    // 选中态用 primary 色填充 + 描边，未选中态用 GlassCard 半透明玻璃风格。
    // 这样浅色/深色界面下选中项都有明确的颜色区分。
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        themeModes.forEach { mode ->
            val isSelected = themeMode == mode
            if (isSelected) {
                // 选中态：primary 容器色填充 + primary 描边 + onPrimary 文字
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.RadioButton }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(),
                        ) {
                            clickHaptic(view)
                            onThemeModeChanged(mode)
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = when (mode) {
                                AppThemeMode.System -> Icons.Rounded.BrightnessAuto
                                AppThemeMode.Light -> Icons.Rounded.LightMode
                                AppThemeMode.Dark -> Icons.Rounded.DarkMode
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            themeModeLabel(mode, strings),
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            } else {
                // 未选中态：GlassCard 玻璃描边风格
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.RadioButton }
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(),
                        ) {
                            clickHaptic(view)
                            onThemeModeChanged(mode)
                        },
                    cornerRadius = 20.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = when (mode) {
                                AppThemeMode.System -> Icons.Rounded.BrightnessAuto
                                AppThemeMode.Light -> Icons.Rounded.LightMode
                                AppThemeMode.Dark -> Icons.Rounded.DarkMode
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            themeModeLabel(mode, strings),
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

private fun themeModeLabel(themeMode: AppThemeMode, strings: AppStrings): String = when (themeMode) {
    AppThemeMode.System -> strings.themeModeSystem
    AppThemeMode.Light -> strings.themeModeLight
    AppThemeMode.Dark -> strings.themeModeDark
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    value: String,
    // 右侧状态值允许的行数（默认 1 行）；DF 卸载卡传 2 以显示「已注入\未配置」双排（修复⑤）
    valueMaxLines: Int = 1,
    position: SettingsCardPosition = SettingsCardPosition.Single,
    // compact=true 时上下内边距各缩 5dp(15→10),用于「语言」下方的次级按钮,让整页更紧凑
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassRadii.Card))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
            ) { clickHaptic(view); onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = if (compact) 10.dp else 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = valueMaxLines,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
    }
}

@Composable
private fun SettingsSwitchCard(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    position: SettingsCardPosition = SettingsCardPosition.Single,
    // compact=true 时上下内边距各缩 5dp(15→10),与 SettingsCard 的 compact 保持一致
    compact: Boolean = false,
    // enabled=false：整张卡不可交互 + 整体降透明度（2026-10-05 新增）。
    // 用途是 dfe 的「开机自启动」：读不到对端状态（无 root 授权）时把开关锁死 ——
    // 状态都没读到就让用户拨，写回去的值不可信，只会把对端状态改坏。
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
            ) { clickHaptic(view); onCheckedChange(!checked) },
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = if (compact) 10.dp else 15.dp)
                // 用 alpha 统一降透明度：图标 / 标题 / 描述 / 开关一次覆盖，
                // 不必逐处改 color（逐处改容易漏掉某一行，出现「半灰半亮」）。
                .alpha(if (enabled) 1f else 0.45f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    }
}

@Composable
private fun SideChoiceMenu(
    choices: List<String>,
    selectedIndex: Int,
    topOffset: Dp,
    onSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 0.34f else 0f,
        animationSpec = tween(durationMillis = if (visible) 160 else 180),
        label = "menu-scrim",
    )

    fun closeMenu(afterAnimation: () -> Unit) {
        if (closing) return
        closing = true
        visible = false
        coroutineScope.launch {
            delay(MENU_EXIT_WAIT_MILLIS)
            afterAnimation()
        }
    }

    LaunchedEffect(Unit) { visible = true }

    Popup(
        onDismissRequest = { closeMenu(onDismiss) },
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            clippingEnabled = false,
        ),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val estimatedHeight = 16.dp + 56.dp * choices.size
            val constrainedTop = minOf(topOffset, maxHeight - estimatedHeight - 24.dp).coerceAtLeast(16.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { closeMenu(onDismiss) },
                    ),
            )
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = constrainedTop, end = 18.dp),
                enter = scaleIn(
                    animationSpec = keyframes {
                        durationMillis = 200
                        1.025f at 95
                        0.995f at 155
                    },
                    initialScale = 0.94f,
                    transformOrigin = TransformOrigin(1f, 0f),
                ),
                exit = scaleOut(
                    animationSpec = tween(durationMillis = MENU_EXIT_ANIMATION_MILLIS),
                    targetScale = 0.86f,
                    transformOrigin = TransformOrigin(1f, 0.5f),
                ) + fadeOut(animationSpec = tween(durationMillis = 160, delayMillis = 20)),
            ) {
                Surface(
                    color = Color.Transparent,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    // 语言选择弹窗接入玻璃效果——与 CustomAlertDialog 同款 drawBackdrop
                    val menuBackdrop = LocalBackdrop.current
                    val glassOn = LocalGlassEnabled.current // 玻璃总开关（执行页自动停用）：false 时跳过 blur/lens
                    val menuShape = MaterialTheme.shapes.extraLarge
                    val menuSurfaceTint = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
                    Box(
                        modifier = Modifier
                            .width(196.dp)
                            .heightIn(max = 620.dp)
                            .drawBackdrop(
                                backdrop = menuBackdrop,
                                shape = { menuShape },
                                effects = { if (glassOn) { blur(16.dp.toPx()); lens(20.dp.toPx(), 20.dp.toPx()) } },
                                onDrawSurface = { drawRect(color = menuSurfaceTint) },
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                            ),
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            itemsIndexed(choices) { index, choice ->
                                val selected = index == selectedIndex
                                Surface(
                                    onClick = { clickHaptic(view); closeMenu { onSelected(index) } },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = if (selected) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.medium,
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        if (selected) {
                                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(22.dp))
                                        }
                                        Text(text = choice, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val MENU_EXIT_ANIMATION_MILLIS = 180
private const val MENU_EXIT_WAIT_MILLIS = 200L

private fun applyLanguage(lang: String, postAction: (() -> Unit)? = null) {
    // ── Round65 新方案：完全绕过 AppCompatDelegate.setApplicationLocales() ──
    //
    // 根因（经过 Round61-64 六轮验证确认）：
    //   setApplicationLocales() 在 API 33+ 必触发 Activity 重建（无视 configChanges），
    //   导致：① 旧Activity销毁→新Activity创建间隙暴露黑色窗口背景 = 黑屏闪
    //         ② MainActivity.key(configVersion) 强制整个 Compose 树销毁重建 = 跳回主页
    //   所有 Window/Theme/Animation 级别的防护均无法消除此根因。
    //
    // 新方案：纯 Compose 内语言切换，零 Activity 生命周期变更：
    //   1. SettingsStore.setLanguage() 已在调用方（SideChoiceMenu.onSelected）保存了偏好 ✅
    //   2. 此处仅执行 postAction 回调 → 更新 currentLanguage Compose 状态
    //   3. Compose 重组 → AppStringResources.forLanguage() 返回新语言字符串 → UI 即时更新
    //   4. 零黑屏闪、零跳转、零状态丢失
    //
    // 持久化保障：
    //   - 本次运行：Compose 状态驱动，即时生效
    //   - 下次冷启动：MainActivity.onCreate() 预加热 setApplicationLocales(已保存语言)
    //     （在 setContent 之前执行，UI 不可见时支付"首次调用"重建代价）
    postAction?.invoke()
}

/**
 * Shizuku 授权玻璃弹窗 —— 与 AboutDialog / CustomAlertDialog 同款 drawBackdrop 磨砂效果。
 * 替代直接调用 requestPermission() 时显示的系统原生弹窗（无磨砂质感），
 * 先展示玻璃弹窗供用户确认，确认后再触发真正的系统授权请求。
 */
@Composable
private fun ShizukuAuthDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val backdrop = LocalBackdrop.current
    val glassOn = LocalGlassEnabled.current // 玻璃总开关（执行页自动停用）：false 时跳过 blur/lens
    val dialogShape = RoundedCornerShape(20.dp)
    val surfaceTint = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = dialogShape,
            color = Color.Transparent,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { dialogShape },
                        effects = { if (glassOn) { blur(16.dp.toPx()); lens(24.dp.toPx(), 24.dp.toPx()) } },
                        onDrawSurface = { drawRect(color = surfaceTint) },
                    )
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 顶部图标
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 标题
                    Text(
                        strings.shizukuTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 描述文字
                    Text(
                        "要允许 Samsung Root 使用 Shizuku 吗？",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalContentColor.current.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        strings.shizukuNeedGrant,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalContentColor.current.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // 操作按钮行 —— 半透明药丸玻璃按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // 拒绝
                        GlassPillButton(
                            text = "拒绝",
                            onClick = onDismiss,
                            isPrimary = false,
                            modifier = Modifier.weight(1f),
                        )
                        // 始终允许
                        GlassPillButton(
                            text = "始终允许",
                            onClick = onConfirm,
                            isPrimary = false,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 药丸形玻璃按钮 —— 弹窗内操作按钮（拒绝 / 始终允许）。
 *
 * 玻璃层构成（与 GlassCard 静态玻璃分支 / FrostedGlassPillButton 降级分支同一套观感语言）：
 *   ① 半透明玻璃填充（次按钮=白色玻璃、主按钮=primary 玻璃）
 *   ② 1dp 发丝描边勾勒玻璃边缘（此前次按钮 borderColor=Transparent → 无边界）
 *   ③ 顶部白色 sheen 渐变（0 → 0.5 处透明）模拟受光高光
 * 三者叠加才有「玻璃块」的实体感。
 *
 * 为什么不用 backdrop 真模糊：本按钮只出现在 ShizukuAuthDialog 内，
 *   弹窗表面已是 90% 不透明的染色层（surfaceTint alpha=0.9f）——
 *   再采样一次 backdrop 模糊几乎被不透明染色完全遮住，视觉增益为零，
 *   却要为 Dialog 场景额外引入 backdrop 采样/纹理重建风险（见 FrostedGlassPillButton
 *   与 GlassBottomBarContainer 的渲染竞态红线），故采用静态玻璃层。
 *
 * 圆角全满(pill)，固定 46dp 高；主次两种样式。
 */
@Composable
private fun GlassPillButton(
    text: String,
    onClick: () -> Unit,
    isPrimary: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    // ── 主题判定：应用支持 跟随系统/浅色/深色 三态（AppThemeMode），
    //    必须按当前 ColorScheme 亮度判定。isSystemInDarkTheme() 只反映系统主题，
    //    应用被强制为深色/浅色时会误判 → 玻璃层取错色，故不可用。
    //    与 GlassComponents.isDarkTheme / CustomAlertDialog 内联判定同逻辑。
    val bg = MaterialTheme.colorScheme.background
    val dark = (bg.red * 0.2126f + bg.green * 0.7152f + bg.blue * 0.0722f) < 0.5f
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val pillShape = RoundedCornerShape(999.dp)
    // 按下反馈：玻璃层整体加浓（而非原先的整块变淡）
    val pressBoost = if (pressed) 1.45f else 1f
    // 2026-10-08：新增 enabled —— 弹窗按钮在 dfBusy 等「执行中」需整体压淡且不可点
    // （弹窗内 GlassAlertDialog 的药丸按钮全部走这里，故禁用态必须在此统一处理）。
    val dim = if (enabled) 1f else 0.4f

    // ① 玻璃填充 —— 关键修正：此前次按钮填充用的是 surfaceContainerHigh，
    //    与弹窗表面（surfaceContainerHigh.copy(alpha=0.9f)）是同一个 token，
    //    同色半透明叠加后与弹窗表面完全同色 → 按钮在视觉上「消失」，只剩文字。
    //    这里改用白色半透明玻璃底，与弹窗表面形成明确亮度差，按钮才立得起来。
    val fillBase = if (isPrimary) {
        MaterialTheme.colorScheme.primary.copy(alpha = if (dark) 0.55f else 0.22f)
    } else {
        Color.White.copy(alpha = if (dark) 0.10f else 0.58f)
    }
    val fillColor = fillBase.copy(alpha = fillBase.alpha * dim)

    val textColor = (if (isPrimary) {
        // 主按钮：白色文字（在 primary 半透明底上清晰可读）
        Color.White
    } else {
        // 次按钮：蓝色文字（primary 蓝），在半透明玻璃底上清晰可读
        MaterialTheme.colorScheme.primary
    }).copy(alpha = dim)

    // ② 发丝描边 —— 明暗两套：深色用白色发丝边；浅色底上白边不可见，改用 primary 淡色边
    val borderBase = if (isPrimary) {
        MaterialTheme.colorScheme.primary.copy(alpha = if (dark) 0.45f else 0.30f)
    } else if (dark) {
        Color.White.copy(alpha = 0.20f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    }
    val borderColor = borderBase.copy(alpha = borderBase.alpha * dim)

    // ③ 顶部 sheen 高光
    val sheenAlpha = if (dark) 0.07f else 0.20f

    Surface(
        modifier = modifier.height(46.dp),
        shape = pillShape,
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(pillShape)
                .background(fillColor.copy(alpha = (fillColor.alpha * pressBoost).coerceAtMost(1f)))
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.White.copy(alpha = (sheenAlpha * pressBoost).coerceAtMost(1f)),
                            0.5f to Color.Transparent,
                        )
                    )
                )
                .border(
                    BorderStroke(1.dp, borderColor.copy(alpha = (borderColor.alpha * pressBoost).coerceAtMost(1f))),
                    pillShape,
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    enabled = enabled,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Medium,
                // 2026-10-08：药丸是「等宽 + 固定 46dp 高」，最窄文案（如「继续，打开管理器」）
                // 在标准字体下约 113dp、而按钮可用宽约 121dp 已接近上限；系统大字体
                // （fontScale > 1.1）会让文字换行并撑破固定高度被 clip 裁掉。
                // 故强制单行 + 省略号：正常字体下完全无影响，极端字体下退化为「…」而非破版。
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 白色磨砂玻璃药丸按钮（成功页底部「再次执行 / 清理缓存」专用）。
 *
 * 使用纯色方案模拟磨砂玻璃质感：primary 蓝半透明填充 + 描边 + 顶部白色微高光。
 * 不依赖 drawBackdrop 纹理，避免 Activity 后台恢复时 backdrop 重建导致 ResultPage 状态丢失。
 *
 * 固定 46dp 高度（由外层 Row 的 weight(1f) 给定宽度），尺寸有界，
 * 内部 Box 的 fillMaxSize 与 GlassBottomBarContainer 同理，无 wrap 卡片膨胀风险。
 */
/**
 * 白色真·磨砂玻璃药丸按钮（成功页底部「再次执行 / 清理缓存」专用）。
 *
 * 与 GlassCard / GlassBottomBarContainer 使用同一套已验证稳定的局部玻璃管线：
 *   局部 rememberLayerBackdrop() + layerBackdrop(捕获) + drawBackdrop(blur+lens) + drawRect(白染色) + 顶部高光。
 *
 * ⚠️ 红线（GlassBottomBarContainer 注释铁证 + Round86 实测）：绝不使用全局 LocalBackdrop.current
 * 作为 drawBackdrop 参数 —— 会引发「捕获→模糊→再捕获」递归导致 RenderThread 崩溃 / Activity 重建，
 * 使 ResultPage 在从 KSU Manager 返回时丢失。本函数坚持用局部 backdrop，规避该递归。
 *
 * ⚠️ 额外保险：仅在 Lifecycle RESUMED 时启用 drawBackdrop。Activity 被 am start 推到后台
 * （KSU 前台）期间 isResumed=false，不创建/重建 backdrop 层，彻底排除后台恢复时的纹理重建竞态。
 *
 * 固定 46dp 高度，由外层 Row 的 weight(1f) 给定确定宽度；内部 Box 处于确定尺寸容器内，
 * 不触发 GlassCard 红线所述的 wrap 卡片膨胀。
 */
@Composable
private fun FrostedGlassPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val dark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pillShape = RoundedCornerShape(999.dp)
    val isResumed = rememberIsResumed()

    // 白色磨砂玻璃染色层：模糊之上叠一层低透明度白，让底层模糊透出 → 真·白玻璃质感
    // （按下态略深）。不使用任何蓝色描边/染色。
    val fillColor = Color.White.copy(
        alpha = if (pressed) 0.52f else (if (dark) 0.14f else 0.40f)
    )
    // 极细白色发丝边，仅勾勒玻璃边缘（非蓝色描边）
    val borderColor = Color.White.copy(
        alpha = if (dark) 0.16f else 0.38f
    )
    val sheenAlpha = if (dark) 0.06f else 0.14f
    // 文字/图标用主题 primary 蓝（白玻璃上的标准强调色，非蓝色描边），明暗都清晰可读
    val textColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier.height(46.dp),
        shape = pillShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) { // 确定尺寸容器(46dp×weight)，fillMaxSize 安全
            // 玻璃总开关关闭（执行页 LocalGlassEnabled=false）时直接走下方 else 静态玻璃分支：不做 backdrop 捕获/blur/lens
            if (isResumed && LocalGlassEnabled.current) {
                val pillBackdrop = rememberLayerBackdrop()
                // 捕获层：放在模糊层之前（绘制顺序在下），捕获其下方兄弟（页面内容）作模糊源。
                Box(Modifier.fillMaxSize().layerBackdrop(pillBackdrop))
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBackdrop(
                            backdrop = pillBackdrop,
                            shape = { pillShape },
                            effects = {
                                blur(16.dp.toPx())
                                lens(24.dp.toPx(), 24.dp.toPx())
                            },
                            onDrawSurface = { drawRect(color = fillColor) },
                        )
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.White.copy(alpha = sheenAlpha),
                                    0.5f to Color.Transparent,
                                )
                            )
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                            onClick = onClick,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    RowPillContent(text, icon, textColor)
                }
            } else {
                // 后台/未恢复降级：静态白玻璃（无 backdrop 纹理，规避重建竞态）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(fillColor)
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.White.copy(alpha = sheenAlpha),
                                    0.5f to Color.Transparent,
                                )
                            )
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                            onClick = onClick,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    RowPillContent(text, icon, textColor)
                }
            }
        }
    }
}

@Composable
private fun RowPillContent(text: String, icon: ImageVector?, textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon?.let {
            Icon(it, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
        }
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/**
 * 仅在 Activity 处于 RESUMED 时返回 true，用于安全启用 drawBackdrop。
 * 规避 Activity 被 am start 推到后台再恢复时 backdrop 纹理重建竞态（该竞态也曾导致 ResultPage 丢失）。
 */
@Composable
private fun rememberIsResumed(): Boolean {
    val owner = LocalLifecycleOwner.current
    var resumed by remember {
        mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            resumed = event == Lifecycle.Event.ON_RESUME
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return resumed
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    // Phase 3 扩展：关于弹窗接入真·玻璃模糊（与 CustomAlertDialog 同款 Kyant0/Backdrop）。
    val backdrop = LocalBackdrop.current
    val glassOn = LocalGlassEnabled.current // 玻璃总开关（执行页自动停用）：false 时跳过 blur/lens
    val dialogShape = RoundedCornerShape(20.dp)
    val surfaceTint = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
    val strings = LocalStrings.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val view = LocalView.current
    val version = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.1"
    } catch (_: Exception) { "1.0.1" }
    val coroutineScope = rememberCoroutineScope()

    // 机型 → 固件 build 号 映射（数据来源：用户提供的支持机型清单，非凭空捏造）。
    val modelVersions: Map<String, List<String>> = mapOf(
        "Galaxy S23 (SM-S9110)" to listOf(
            "S9110ZCS8FZE3", "S9110ZCS8FZF5", "S9110ZCS8FZG1",
        ),
        "Galaxy S23+ (SM-S9160)" to listOf(
            "S9160ZCS8FZE3", "S9160ZCS8FZG1", "S9160ZHS8FZE3",
        ),
        "Galaxy S23 Ultra (SM-S9180)" to listOf(
            "S9180ZCS8FZE3", "S9180ZCS8FZG1", "S9180ZHS8FZG1",
            "S918U1UES8FZG1",
            "S9180ZCS8FZH3", "S9180ZHS8FZH3",
            "S9180ZCS8FZI1", "S9180ZHS8FZI1",
        ),
        "Galaxy S24 (SM-S9210)" to listOf(
            "S9210ZCS6DZF2", "S9210ZCU4BYH7", "BYH7",
            "S9210ZCS6DZE2", "S9210ZHS6DZG1",
        ),
        "Galaxy S24+ (SM-S9260)" to listOf(
            "S9260ZCS6DZF2", "S9260ZCS6DZG1", "S9260ZCS6DZE2",
        ),
        "Galaxy S24 Ultra (SM-S9280)" to listOf(
            "S9280ZCS6DZF2", "S9280ZHS6DZF2", "S9280ZHS6DZG1",
            "S928BXXS6DZG1", "S928U1UES6DZF2",
            // 旧版短标签 CZA1 / CZA1-CHC / DZE2 已按机型版本号规范化，改用下列完整固件号
            "S9280ZCS6DZE2",
            "S9280ZCS4CZA1", "S9280ZHS4CZA1", "S9280ZHS6DZE2",
            "S9280ZCS6DZH3", "S928BXXS6DZF2", "S928USQS6DZF2", "S928W-S928USQS6DZF2",
        ),
        "Galaxy S25 (SM-S9310)" to listOf(
            "S9310ZCSBCZE1", "S9310ZCSCCZG1", "S9310ZCUBCZF1", "S9310ZHSCCZG1",
        ),
        "Galaxy S25+ (SM-S9360)" to listOf(
            "S9360ZCUBCZF1",
        ),
        "Galaxy S25 Ultra (SM-S9380)" to listOf(
            // CZG1 为 root-my-s24 合并基线（覆盖 S9310/S9360/S9380 的 CZG1 构建）
            "S9380ZCSBCZE1", "S9380ZCSCCZG1", "S9380ZCSABZD1", "S9380ZCS6BYJ2",
            "S9380ZCS7BYL1", "S9380ZCS8BZA1", "S9380ZCS9BZB2", "S9380ZCS9BZC1",
            "S9380ZCU1AYA5", "S9380ZCU5AYHA", "S9380ZCU5BYI3", "S9380ZCUACZDP",
            "S9380ZCUBCZF1", "S9380ZHSBCZE1", "S9380ZHSCCZG1", "S9380ZHU1AYA1",
            "S9380ZHUBCZF1", "S938BXXS9CZE1", "S938NKSUACZF1",
            "S938USQS8BYLX", "S938USQU5BYI3", "CZG1",
            "S9380ZCSDCZH1", "S9380ZHSDCZH1",
        ),
        "Galaxy S25 Edge (SM-S9370)" to listOf(
            "S9370ZCU7CZDP", "S9370ZCU8CZF1", "S9370ZHS9CZG1",
        ),
        "Galaxy S26 (SM-S942)" to listOf(
            "S942U1UES4AZG3", "S9420ZCS4AZG1",
        ),
        "Galaxy S26 Ultra (SM-S948)" to listOf(
            "S9480ZCS3AZF1", "S9480ZCS4AZG1",
        ),
        "Galaxy Z Fold5 (SM-F9460)" to listOf(
            "F9460TBS9GZF1", "F946NKSS6GZF2", "F9460ZCS9GZF1",
        ),
        "Galaxy Z Flip5 (SM-F7310)" to listOf(
            "F7310ZCS9GZF1", "F731NKSS6FZCS", "F731USQS8GZF1",
            "F7310TBS9GZF1",
        ),
        "Galaxy Z Flip6 (SM-F7410)" to listOf(
            "F741BXXS4DZG3", "F741USQS4DZF2",
            "F7410ZCS4DZF2", "F7410ZCS4DZG3",
        ),
        "Galaxy Z Fold7 (SM-F9660)" to listOf(
            "F9660ZCU9BZDP", "F9660ZCUABZF1", "F966BXXUABZF1", "F966U1UES8AZC1",
        ),
        "Galaxy W26 (SM-W9026)" to listOf(
            "W9026ZCS8BZG3", "W9026ZCU7BZF1",
        ),
        "Galaxy Tab S9 Ultra (SM-X910)" to listOf(
            "X910ZCU6EZE3",
        ),
        // —— 第一批移植（root-my-s24，移植待测）新增机型 ——
        "Galaxy Z Fold6 (SM-F9560)" to listOf(
            "F9560ZCS4DZG3", "F956USQS4DZG3",
        ),
        "Galaxy Z Flip7 (SM-F7660)" to listOf(
            "F7660ZCSBBZG3",
        ),
        "Galaxy W25 (SM-W9025)" to listOf(
            "W9025ZCS4DZF2", "W9025ZCS4DZG3",
        ),
        "Galaxy S26+ (SM-S9470)" to listOf(
            "S9470ZCS4AZG1",
        ),
    )

    val pagerState = rememberPagerState(pageCount = { 3 })
    var selectedModel by remember { mutableStateOf<String?>(null) }
    var showSpecialThanks by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { dialogShape },
                    effects = { if (glassOn) { blur(16.dp.toPx()); lens(24.dp.toPx(), 24.dp.toPx()) } },
                    onDrawSurface = { drawRect(color = surfaceTint) },
                )
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(strings.aboutTitle, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.height(420.dp)) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = false,
                    ) { page ->
                        when (page) {
                            0 -> {
                                // ── 第0页：关于信息 + 已支持机型入口 ──
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                        Text(strings.aboutVersion(version), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.align(Alignment.CenterStart))
                                    }
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    // 2026-10-02：内容新增 DFRoot / DFReroot 两项后，总高可能超出上方 420dp 容器，
                                    // 这里补 verticalScroll 兜底（内容装得下时无任何视觉差异，装不下时可滚动而非被裁掉）。
                                    Column(
                                        modifier = Modifier.verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(strings.aboutDescription, style = MaterialTheme.typography.bodyMedium)

                                        // 已支持机型入口（点击进第1页）——纯文字行，无容器框
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = ripple(),
                                                    onClick = { clickHaptic(view); coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                                                )
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Icon(Icons.Rounded.List, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(strings.aboutSupportedModels, style = MaterialTheme.typography.titleSmall)
                                                Text("${modelVersions.size} 款设备", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        HorizontalDivider()

                                        // KernelSU 链接——纯文字行，无容器框
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = ripple(),
                                                    onClick = {
                                                        clickHaptic(view)
                                                        try {
                                                            uriHandler.openUri("https://kernelsu.org/")
                                                        } catch (_: Exception) {
                                                            // 兜底：直接用 Intent 启动浏览器
                                                            context.startActivity(
                                                                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://kernelsu.org/"))
                                                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            )
                                                        }
                                                    },
                                                )
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                            Column(modifier = Modifier.weight(1f)) {
                                                // 2026-10-02：按需求取消该项下方小字（原 strings.aboutKsuDesc）
                                                Text("KernelSU", style = MaterialTheme.typography.titleSmall)
                                            }
                                            Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        // Root-My-Galaxy 链接——纯文字行，无容器框
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = ripple(),
                                                    onClick = {
                                                        clickHaptic(view)
                                                        try {
                                                            uriHandler.openUri("https://github.com/BuSung-dev/Root-My-Galaxy")
                                                        } catch (_: Exception) {
                                                            // 兜底：直接用 Intent 启动浏览器
                                                            context.startActivity(
                                                                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/BuSung-dev/Root-My-Galaxy"))
                                                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            )
                                                        }
                                                    },
                                                )
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                            Column(modifier = Modifier.weight(1f)) {
                                                // 2026-10-02：按需求取消该项下方小字（原 strings.aboutRmgDesc）
                                                Text("Root-My-Galaxy", style = MaterialTheme.typography.titleSmall)
                                            }
                                            Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        // DFRoot 链接——纯文字行，无容器框（2026-10-02 新增，排在 Root-My-Galaxy 之下）
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = ripple(),
                                                    onClick = {
                                                        clickHaptic(view)
                                                        try {
                                                            uriHandler.openUri("https://github.com/diabl0w/DFRoot")
                                                        } catch (_: Exception) {
                                                            // 兜底：直接用 Intent 启动浏览器
                                                            context.startActivity(
                                                                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/diabl0w/DFRoot"))
                                                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            )
                                                        }
                                                    },
                                                )
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                            Column(modifier = Modifier.weight(1f)) {
                                                // 2026-10-02：显示文字改为「账号/仓库」形式；点击打开的链接不变。
                                                Text("diabl0w/DFRoot", style = MaterialTheme.typography.titleSmall)
                                            }
                                            Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        // DFReroot 链接——纯文字行，无容器框（2026-10-02 新增，排在 DFRoot 之下）
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = ripple(),
                                                    onClick = {
                                                        clickHaptic(view)
                                                        try {
                                                            uriHandler.openUri("https://github.com/polygraphene/DFReroot")
                                                        } catch (_: Exception) {
                                                            // 兜底：直接用 Intent 启动浏览器
                                                            context.startActivity(
                                                                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/polygraphene/DFReroot"))
                                                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            )
                                                        }
                                                    },
                                                )
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                            Column(modifier = Modifier.weight(1f)) {
                                                // 2026-10-02：显示文字改为「账号/仓库」形式；点击打开的链接不变。
                                                Text("polygraphene/DFReroot", style = MaterialTheme.typography.titleSmall)
                                            }
                                            Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        HorizontalDivider()

                                        // 特别感谢——点击打开致谢弹窗
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = ripple(),
                                                    onClick = { clickHaptic(view); showSpecialThanks = true },
                                                )
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(strings.aboutSpecialThanks, style = MaterialTheme.typography.titleSmall)
                                            }
                                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        HorizontalDivider()

                                        // 作者信息——带蓝色图标，上下两层显示（上层"作者"，下层名字）
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("作者", style = MaterialTheme.typography.titleSmall)
                                                Text("大尾巴狼", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // 禁止倒卖（红字，保留在最下面）
                                        Text(strings.aboutNoResale, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            1 -> {
                                // ── 第1页：机型列表（点击进入第2页版本号）──
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        IconButton(onClick = { clickHaptic(view); coroutineScope.launch { pagerState.animateScrollToPage(0) } }) {
                                            Icon(Icons.Rounded.ArrowBack, contentDescription = strings.commonBack, modifier = Modifier.size(20.dp))
                                        }
                                        Text(strings.aboutSupportedModels, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    }
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxSize(),
                                    ) {
                                        items(modelVersions.keys.toList()) { model ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = ripple(),
                                                        onClick = {
                                                            clickHaptic(view)
                                                            selectedModel = model
                                                            coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                                        },
                                                    ),
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                                ) {
                                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                    Text(model, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // ── 第2页：选中机型的版本号列表（可滚动）──
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        IconButton(onClick = { clickHaptic(view); coroutineScope.launch { pagerState.animateScrollToPage(1) } }) {
                                            Icon(Icons.Rounded.ArrowBack, contentDescription = strings.commonBack, modifier = Modifier.size(20.dp))
                                        }
                                        Text(selectedModel ?: strings.aboutSupportedModels, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    }
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val versions = (selectedModel?.let { modelVersions[it] } ?: emptyList())
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxSize(),
                                    ) {
                                        items(versions) { ver ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                                ) {
                                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                    Text(ver, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(strings.aboutDismiss) }
                }
            }
        }
    }
    // 特别感谢弹窗（从"特别感谢"项点击触发）
    if (showSpecialThanks) {
        SpecialThanksDialog(onDismiss = { showSpecialThanks = false })
    }
}

/**
 * 特别感谢弹窗 —— 从关于页"特别感谢"项点击打开。
 * 列出 4 条致谢内容，玻璃模糊背景，与 AboutDialog 同款风格。
 */
@Composable
private fun SpecialThanksDialog(onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val backdrop = LocalBackdrop.current
    val glassOn = LocalGlassEnabled.current // 玻璃总开关（执行页自动停用）：false 时跳过 blur/lens
    val dialogShape = RoundedCornerShape(20.dp)
    val surfaceTint = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { dialogShape },
                    effects = { if (glassOn) { blur(16.dp.toPx()); lens(24.dp.toPx(), 24.dp.toPx()) } },
                    onDrawSurface = { drawRect(color = surfaceTint) },
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(strings.aboutSpecialThanks, style = MaterialTheme.typography.headlineSmall)
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 每条致谢：Row 布局实现 • 符号固定宽度 + 文字独立折行，保证竖向对齐
                    ThanksItem(strings.aboutSpecialThanks1)
                    ThanksItem(strings.aboutSpecialThanks2)
                    ThanksItem(strings.aboutSpecialThanks3)
                    ThanksItem(strings.aboutSpecialThanks4)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(strings.aboutDismiss) }
                }
            }
        }
    }
}

/**
 * 致谢条目 —— 左侧固定宽度 • 装饰符号 + 右侧文字独立折行。
 * 折行后的文字左边缘与首行文字对齐（不与 • 符号对齐），解决 inline bullet 错位问题。
 */
@Composable
private fun ThanksItem(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // • 符号固定宽度，不参与折行
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(14.dp),
            softWrap = false,
        )
        // 文字区域：fillMaxWidth 允许折行，折行后自动缩进（Row 布局保证对齐）
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun resolveEffectiveLanguage(lang: String): String {
    return when {
        lang == "en" -> "en"
        lang == "system" || lang.isEmpty() -> {
            val sysLang = LocaleListCompat.getDefault()[0]?.language ?: "zh"
            if (sysLang.startsWith("en")) "en" else "zh"
        }
        else -> "zh"
    }
}


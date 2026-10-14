package com.samsung.rootkit.dfroot

import android.content.Context
import android.util.Log
import com.samsung.rootkit.data.PayloadManager
import com.samsung.rootkit.data.SettingsStore
import df.root.DfChain
import df.root.IReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * df 一次跑链的结果。
 *
 * <p>字段比「A 路线」时期少了两项（`requestId` / `fromBroadcast`）——
 * 那两个是**跨 App 通道**的产物（用请求号匹配异步回传、区分结果是广播还是兜底文件）。
 * 单包后触发是一次阻塞的函数调用，返回值就是结果本身，不需要任何关联/来源标记。
 */
data class DfRootResult(
    val rc: Int,
    val ksuNext: Boolean,
    val softReboot: Boolean,
    /** 跑链耗时（毫秒；排障用）。 */
    val elapsedMs: Long,
) {
    /** 是否「拿到 root 或本来就已注入」——UI 的「成功」判据。 */
    val ok: Boolean get() = rc == DfRootBridge.RC_OK || rc == DfRootBridge.RC_ALREADY_ARMED

    /** 本次开机此前已注入（无需重复跑）。 */
    val alreadyArmed: Boolean get() = rc == DfRootBridge.RC_ALREADY_ARMED

    /**
     * 是否「**链真的跑了、且在本固件上失败**」（rc=1/2/3）。
     *
     * <p>这是三件事的共同判据，必须只有一个定义、处处复用：
     * <ol>
     *   <li>写失败记录（[DfRootFailureRecordStore]）；</li>
     *   <li>自动关闭 DFRoot 总开关（见 [handleChainFailure]）；</li>
     *   <li>UI 弹「当前系统版本不支持 ⇒ 进入 SRK 的 Root 流程」引导。</li>
     * </ol>
     * 与之相对，rc=9（已注入）不是失败、rc=-1 等链路级失败「链根本没跑」
     * （无 su / 调用异常 ⇒ 是 SRK 自身环境问题），两者都**不得**触发上面前两件事，
     * 否则会把 df 入口在一台本来能用的设备上错误锁死。
     */
    val chainFailedOnDevice: Boolean
        get() = rc == DfRootBridge.RC_KSUD_ERROR ||
            rc == DfRootBridge.RC_TIMEOUT ||
            rc == DfRootBridge.RC_PATCH_FAIL

    /** 人类可读摘要（写日志用；UI 侧另有本地化文案）。 */
    val summary: String
        get() = when (rc) {
            DfRootBridge.RC_OK -> "成功获取 Root"
            DfRootBridge.RC_ALREADY_ARMED -> "本次开机已注入（无需重复）"
            DfRootBridge.RC_KSUD_ERROR -> "失败：ksud 执行出错"
            DfRootBridge.RC_TIMEOUT -> "失败：执行超时"
            DfRootBridge.RC_PATCH_FAIL -> "失败：文件补丁失败/异常"
            else -> "未知结果（rc=$rc）"
        }
}

/**
 * df 的开关状态快照（设置页与主页状态行共用）。
 *
 * <p>全部来自**本应用自己的 DE prefs**（[DfRootPrefs]）—— 单包后 df 不再是别的 App，
 * 不存在「SRK 读不到对方私有存储、只能等回传」的问题，故不再有
 * `autoSoftRebootKnown`（「是否真实回读过」）这种字段：读的就是自己的存储，一定读得到。
 */
data class DfRootSwitchState(
    /** 总开关。 */
    val enabled: Boolean,
    /** 开机自启（已含总开关语义的原始值，不强乘总开关；UI 自行决定怎么呈现）。 */
    val autostart: Boolean,
    /** 自动软重启。 */
    val autoSoftReboot: Boolean,
    /** 自启 / 跑链所用变体（true = KSU-Next）。 */
    val ksuNext: Boolean,
)

/**
 * df（DFRoot）的**编排层** —— 跑链入口 + 开关读写。
 *
 * <h2>相对「A 路线」的变化</h2>
 * 原实现是本包最重的一层，因为它要跨 App：拉 `TriggerActivity`、注册广播收结果、
 * 读兜底文件兜底、用 PackageInstaller 装内嵌 APK、轮询版本号决定要不要同步安装。
 * 单包整合后**这些全部消失**，只剩两件事：
 * <ol>
 *   <li>[runChain] —— 在**本进程内**调用 [DfChain.run]；</li>
 *   <li>开关读写 —— 直接读写本应用自己的 DE prefs / 组件态（[DfRootPrefs]）。</li>
 * </ol>
 * 于是本类从「带超时与双通道的跨进程编排」退化为「一层薄封装」，
 * 这也是这次整合在**可靠性**上的主要收益：不再有广播丢失、不再有超时误判、
 * 不再有「指令发出去了但对方没装」这类需要兜底的状态。
 */
object DfRootManager {

    private const val TAG = "DFROOT"

    // ─────────────────────────── 跑链 ───────────────────────────

    /**
     * 跑一次完整的 df 链（**必须在协程里调用**；内部切到 IO 线程）。
     *
     * <p>阻塞时长从数秒到分钟级不等（取决于内核版本与是否需要软重启），
     * 调用方应保证 UI 上有「执行中」的反馈（见 [onProgress]）。
     *
     * @param ksuNext    true = KernelSU-Next 路（会写 DE prefs 的 `ksu_next`，
     *                   与自启共用同一键 —— 「最后一次实际使用的变体」）
     * @param softReboot 是否软重启；调用方通常传「自动软重启」设置值
     * @param onProgress 进度回调（在 IO 线程被调用，**不是**主线程；
     *                   UI 侧需自行切线程或用可线程安全写入的状态容器）
     * @return 结果（rc 语义见 [DfRootBridge]）
     */
    suspend fun runChain(
        ctx: Context,
        ksuNext: Boolean,
        softReboot: Boolean,
        onProgress: ((String) -> Unit)? = null,
    ): DfRootResult = withContext(Dispatchers.IO) {
        // 变体先落盘再跑：即使本次跑链中途失败/被 kill，自启变体也已与用户点击的按钮一致。
        // 与 df 原 MainActivity.startExploit 的写法一致（那里也是先 putBoolean 再 execute）。
        DfRootPrefs.setKsuNext(ctx, ksuNext)

        val reporter = IReporter { msg ->
            if (msg != null) {
                Log.i(TAG, msg.trim())
                onProgress?.invoke(msg)
            }
        }

        val started = System.currentTimeMillis()
        // ⚠️ 必须用 DE context 跑：ksud 暂存目标 = 「运行本代码的 App 的 DE 目录」，
        //    而 ko 的 UMH 命令写死的就是该路径（/data/user_de/0/com.samsung.root.kit/ksud）。
        val deCtx = ctx.createDeviceProtectedStorageContext()
        val rc = try {
            DfChain.run(deCtx, reporter, softReboot, ksuNext)
        } catch (t: Throwable) {
            Log.e(TAG, "runChain exception", t)
            onProgress?.invoke("\n发生异常：$t\n")
            DfChain.RC_PATCH_FAIL
        }
        val result = DfRootResult(
            rc = rc,
            ksuNext = ksuNext,
            softReboot = softReboot,
            elapsedMs = System.currentTimeMillis() - started,
        )
        Log.i(TAG, "runChain rc=$rc (${result.summary}) in ${result.elapsedMs}ms")

        // 失败 ⇒ 累计「连续失败次数」；**仅第 3 次**才关总开关 + 落黑名单记录
        // （唯一入口 handleChainFailure，见其 KDoc；2026-10-08 由「一次即锁」改为「三次」）。
        // 成功不写「失败记录」：本工程只记失败（2026-10-05 用户定调）。
        if (result.chainFailedOnDevice) handleChainFailure(ctx, rc)

        // ── 但「成功」必须留一条跨进程的痕迹（round32 修）──
        // 这套 KSU 对普通应用完全隐藏（untrusted_app 读不到 /proc/modules、无 su，
        // 无 Shizuku 时连内核版本码都探不到），主页状态卡的「当前系统已ROOT」只能靠
        // ① 内核事实 `/dev/df`（见 RootKitScreen.refreshStates）与 ② 本记录 判出。
        // 此前**没有任何路径写过它** ⇒ df 提权成功后主页仍显示「当前系统未ROOT」。
        // ⚠️ 必须同步落盘（SettingsStore 内部用 commit）：本链随即可能软重启
        //    （kill $(pidof system_server)），本进程会被一并杀掉，异步 apply 来不及。
        if (result.ok) SettingsStore.markRootedThisBoot(ctx)

        // ── 成功 ⇒ **连续失败计数清零**（2026-10-08）────────────────────────────
        // 与上面「已 root 标记」并列但职责不同：那条回答「有没有 root」，
        // 这条只维护失败计数器。阈值判的是**连续**失败，成功已证明本固件可用 ⇒
        // 必须清零，否则「失败 2 次 → 成功 → 再失败 1 次」会被误判成 3 连败而锁死。
        // ⚠️ 开机自启那一趟走 DfBootReceiver，不经本函数 —— 那边也必须调
        //    handleChainSuccess，否则计数会跨成功累计（见其 KDoc）。
        if (result.ok) handleChainSuccess(ctx)

        // ── 「本次开机实际注入所用变体」落盘（2026-10-07）────────────────────────
        // 只在 rc=RC_OK（**真的注入了**）时写；RC_ALREADY_ARMED 不写（本次注入的变体
        // 由更早那一次记录），见 DfRootPrefs.setArmedVariant 的告警。
        // 这是 df 链模式下「当前工作变体」的唯一可靠来源 —— 没有它，
        // resolveWorkingVariant() 在纯 df 场景恒为 null，主页红/蓝按钮的
        // 「切换下次开机管理器」确认窗永不弹出（用户 2026-10-07 报障）。
        if (result.rc == DfRootBridge.RC_OK) DfRootPrefs.setArmedVariant(ctx, ksuNext)

        result
    }

    // ─────────────────────── 失败处置（链跑过且失败时唯一入口） ───────────────────────

    /**
     * 「本固件上 df 失败」的统一处置 —— 两个调用点（手动跑链 / 开机自启）共用。
     *
     * <p><b>★ 2026-10-08 用户修正：由「失败 1 次即处置」改为「连续 3 次才处置」。</b>
     * 用户原话：「DF 链同样是用了漏洞，是有可能一次不成功的，不代表下次不成功，
     * 需要改为记录 3 次 root 不成功，自动关闭 df 总开关」。
     * 于是本方法变成**带阈值的计数器**：
     * <ul>
     *   <li>第 1、2 次失败：只调 [DfRootFailureRecordStore.recordFailure] 计数，
     *       **不关总开关、不拦入口**，用户可以立刻重试；</li>
     *   <li>第 [DfRootFailureRecord.FAIL_THRESHOLD] 次（[DfRootFailureRecord.confirmed]）：
     *       才做旧规则的全部动作 ——
     *       <ol>
     *         <li>使 [DfRootFailureRecordStore.read] 具备黑名单语义 ⇒ ① 启动判定出口回退
     *             （[DfRootLaunchState.UNSUPPORTED]）、② 设置页总开关打不开、③ 弹
     *             「当前系统版本不支持」（都带固件绑定，系统升级后自动作废/解锁）；</li>
     *         <li>关掉 DFRoot 总开关（[setEnabled]）：用户原话「然后自动关闭 df 总开关」。
     *             副作用是同时停用开机自启的组件态（`enabled && autostart` 派生）——
     *             这正是想要的效果：一个在当前固件上**连续三次**失败的链，
     *             不该每次开机再白跑一遍。</li>
     *       </ol></li>
     * </ul>
     *
     * <p>⚠️ 调用前必须确认 [DfRootResult.chainFailedOnDevice] —— 见该属性的注释：
     * rc=9（已注入）与 rc=-1（链没跑）都**不得**走这里。
     *
     * <p>⚠️ **写盘失败时不关总开关（fail-open）**：计数没能落盘就无法证明「连续 3 次」，
     * 凭一次无法记录结果的失败锁死设备，比让它继续重试更有害 ——
     * 见 [DfRootFailureRecordStore.recordFailure] 的 `@return`。
     *
     * @param rc 触发本次处置的结果码（1/2/3，落盘备查）
     */
    fun handleChainFailure(ctx: Context, rc: Int) {
        val rec = DfRootFailureRecordStore.recordFailure(ctx, rc)
        if (rec == null) {
            Log.w(TAG, "df chain failed (rc=$rc) 但失败计数写盘失败 ⇒ 不关总开关（fail-open）")
            return
        }
        if (!rec.confirmed) {
            Log.w(
                TAG,
                "df chain failed (rc=$rc) — 连续失败 ${rec.fails}/" +
                    "${DfRootFailureRecord.FAIL_THRESHOLD}，未达阈值：" +
                    "总开关保持打开，可直接重试（还差 ${rec.remaining} 次才会自动关闭）",
            )
            return
        }
        Log.w(
            TAG,
            "df chain failed ${rec.fails} times on this firmware (rc=$rc) — " +
                "已达阈值：记录版本并关闭 DFRoot 总开关",
        )
        if (!setEnabled(ctx, false)) {
            // 写失败不抛出：记录已落盘，下次任意一次开关读写都会重新派生组件态。
            Log.w(TAG, "handleChainFailure: setEnabled(false) failed")
        }
    }

    /**
     * 「df 链在本固件上**成功**」的统一处置 —— 把**连续失败计数清零**（2026-10-08）。
     *
     * <p>为什么必须清零：阈值判的是**连续**失败。成功本身已经证明该固件可用，
     * 若不清零，「失败 2 次 → 成功 → 再失败 1 次」会被误算成 3 连败而锁死设备 ——
     * 这是计数器语义下最容易漏掉、也最难复现的一类缺陷。
     *
     * <p>两个调用点（手动跑链 [runChain] 的 `result.ok` 分支 / 开机自启
     * [DfBootReceiver] 的 `ok` 分支）必须都调，否则「开机自启成功」这一路
     * 不会清零，计数仍会跨成功累计。
     *
     * <p>无记录时零开销早退（避免每次成功都做一次无谓的删文件 IO）。
     * 注意本方法**无权**决定「是否已 root」——那只由 [SettingsStore.markRootedThisBoot]
     * 负责；这里只维护失败计数器。
     */
    fun handleChainSuccess(ctx: Context) {
        if (!DfRootFailureRecordStore.exists(ctx)) return
        DfRootFailureRecordStore.clear(ctx)
        Log.i(TAG, "df chain succeeded on this firmware — 连续失败计数已清零")
    }

    // ─────────────────────────── 开关读写（全部零 su、零权限） ───────────────────────────

    /**
     * 打开/关闭 DFRoot 总开关。
     *
     * <p>副作用不止「记一个布尔」：会同步派生 [DfBootReceiver] 的组件启用态
     * （关 ⇒ 组件停用，系统不再投递开机广播），见 [DfRootPrefs.syncBootReceiverComponent]。
     */
    fun setEnabled(ctx: Context, enable: Boolean): Boolean =
        DfRootPrefs.setEnabled(ctx, enable)

    /** 打开/关闭开机自启（同样会重新派生组件态）。 */
    fun setAutostart(ctx: Context, enable: Boolean): Boolean =
        DfRootPrefs.setAutostart(ctx, enable)

    /** 打开/关闭自动软重启。 */
    fun setAutoSoftReboot(ctx: Context, enable: Boolean): Boolean =
        DfRootPrefs.setAutoSoftReboot(ctx, enable)

    /**
     * 记下**下次开机**自启所用的变体（本次开机不跑链）。
     *
     * <p>用于主页「点另一个变体」的场景：`/dev/df` 是 O_EXCL 互斥锁，
     * 本次开机必然无法重复注入 ⇒ 只能把变体记下、等下次开机生效。
     */
    fun setVariant(ctx: Context, ksuNext: Boolean): Boolean =
        DfRootPrefs.setKsuNext(ctx, ksuNext)

    /** 读开关状态（零副作用，可随页面 onResume 常刷）。 */
    suspend fun readSwitchState(ctx: Context): DfRootSwitchState = withContext(Dispatchers.IO) {
        readSwitchStateNow(ctx)
    }

    /**
     * [readSwitchState] 的同步版本。
     *
     * <p>读的是**本进程自己**的 DE prefs + 组件态，不涉及 IPC 轮询、不涉及对方进程，
     * 开销与读一个内存里的 SharedPreferences 同量级 ⇒ 无需强制 IO 线程。
     * 保留 [readSwitchState] 的 suspend 版本是为了让既有调用点（`withContext(Dispatchers.IO)`）
     * 无需改动，二者行为完全一致。
     */
    fun readSwitchStateNow(ctx: Context): DfRootSwitchState = DfRootSwitchState(
        enabled = DfRootPrefs.isEnabled(ctx),
        autostart = DfRootPrefs.isAutostart(ctx),
        autoSoftReboot = DfRootPrefs.isAutoSoftReboot(ctx),
        ksuNext = DfRootPrefs.isKsuNext(ctx),
    )

    /** 本次开机是否已注入（转发 [DfRootBridge.isArmedThisBoot]，方便 UI 只依赖本类）。 */
    fun isArmedThisBoot(): Boolean = DfRootBridge.isArmedThisBoot()

    /**
     * **本次开机由 df 链实际注入**所用的变体；不是 df 注入 / 无记录 ⇒ null。
     *
     * <p>供 [com.samsung.rootkit.exploit.RootExecutor.resolveWorkingVariant] 作为
     * 「当前工作变体」的一级可靠来源（见 [DfRootPrefs.KEY_ARMED_VARIANT] 的成因说明）。
     *
     * <p>**门控**：先看 [isArmedThisBoot]（`/dev/df` 存在）—— 不是本次开机 df 注入，
     * 或冷重启后记录已作废，一律返回 null，绝不猜。
     */
    fun armedVariantThisBoot(ctx: Context): PayloadManager.KsuVariant? {
        if (!isArmedThisBoot()) return null
        return when (DfRootPrefs.armedVariantNextOrNull(ctx)) {
            true -> PayloadManager.KsuVariant.KSU_NEXT
            false -> PayloadManager.KsuVariant.KSU
            null -> null
        }
    }
}

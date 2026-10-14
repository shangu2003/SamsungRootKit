package com.samsung.rootkit.dfroot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.samsung.rootkit.data.RunLogStore
import com.samsung.rootkit.data.RunOutcome
import com.samsung.rootkit.data.RunSource
import df.root.DfChain
import df.root.IReporter

/**
 * df（DFRoot，已内嵌）的**开机自启接收器**。
 *
 * <p>职责等价于原 df 独立包的 `df.root.BootReceiver`，但有两点不同：
 * <ol>
 *   <li>链现在跑在**本进程内**（[DfChain.run]），不再是跨 App 启动 df 的 Activity；</li>
 *   <li>「是否该跑」由 [DfRootPrefs] 的 `enabled && autostart` 决定 —— 而该组合被
 *       [DfRootPrefs.syncBootReceiverComponent] 同步到**本组件的启用态**，所以实际
 *       绝大多数情况下系统根本不会投递广播（组件已停用）。onReceive 里再查一次是
 *       防御性冗余：防「组件态与 prefs 曾短暂不一致」的历史包袱。</li>
 * </ol>
 *
 * <h2>为什么同时监听 LOCKED_BOOT_COMPLETED 与 BOOT_COMPLETED</h2>
 * 与 df 原实现一致：`LOCKED_BOOT_COMPLETED` 在用户**解锁前**就投递，能更早拿到 root；
 * `BOOT_COMPLETED` 是兜底 —— 部分 ROM 在 Direct Boot 未启用（或 DE 存储不可用）时
 * 只投递后者。<b>两个广播都处理，靠 [DfChain.isArmedThisBoot] 的 `/dev/df` 互斥去重</b>：
 * 若 LOCKED_BOOT 已经跑成功，BOOT 那次进来会立刻看到 `/dev/df` 存在并返回 ——
 * 无需自己维护「本次开机是否已处理过」的记忆状态（那才是真的容易写错）。
 *
 * <h2>线程与唤醒锁</h2>
 * onReceive 必须**快速返回**（主线程，超时会被 ANR/杀进程），故真正跑链丢到独立线程；
 * 链最长可达分钟级，期间设备可能想休眠 —— 持一个 PARTIAL_WAKE_LOCK 避免跑一半被挂起
 * （`/dev/df` 的 O_EXCL 已被消费但链未完成 ⇒ 本次开机此后再也拿不到 root）。
 * 锁**必须**在 finally 里释放：跑链抛异常时不释放会永久拖住电源管理。
 */
class DfBootReceiver : BroadcastReceiver(), IReporter {

    /**
     * 本次开机自启跑链的日志行。
     *
     * <p>[report] 会被 native 层从**跑链线程**回调，而收尾在主线程之外读它，
     * 故用同步列表而不是普通 MutableList。
     * <p>接收器每次 onReceive 都是新实例（系统不缓存接收器对象），不必担心跨广播串数据。
     */
    private val bootLogLines: MutableList<String> =
        java.util.Collections.synchronizedList(mutableListOf())

    override fun report(msg: String) {
        // 接收器没有界面；repoter 只进 logcat（与 df 原 BootReceiver 同策略）。
        Log.i(TAG, msg.trim())
        if (msg.isNotBlank()) bootLogLines.add(msg.trim())
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "boot: $action")

        // 防御性再查一次（组件态已派生自这两项，正常不会走到 false 分支）。
        if (!DfRootPrefs.isEnabled(context) || !DfRootPrefs.isAutostart(context)) {
            Log.i(TAG, "boot: disabled (enabled/autostart off), skip")
            return
        }
        // ⚠️ 必须用 DE context 跑链：ExploitRunner.stageFile 把 ksud 写到
        //    「运行代码所在 App 的 DE 目录」，而 ko 的 UMH 命令写死的就是该路径。
        //    此处若用 context（CE 视角），在锁屏期会写到不可用/错误的目录。
        val deCtx = context.createDeviceProtectedStorageContext()

        // ── 管理器提醒 ────────────────────────────────────────────────────────────
        //  ⚠️ **必须放在下面 `/dev/df` 早退之前**，理由见 [refreshManagerNotice] KDoc。
        refreshManagerNotice(context, deCtx)

        // 本次开机已注入 ⇒ 无需重复（O_EXCL 互斥，第二次必然失败）。
        if (DfChain.isArmedThisBoot()) {
            Log.i(TAG, "boot: already armed this boot, skip")
            return
        }

        val softReboot = DfRootPrefs.isAutoSoftReboot(deCtx)
        // 跑链所用的变体（与 [refreshManagerNotice] 内各自独立读取，互不依赖）。
        val ksuNext = DfRootPrefs.isKsuNext(deCtx)

        val wakeLock = try {
            (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "srk:dfroot-boot")
                ?.also { it.acquire() }
        } catch (t: Throwable) {
            Log.w(TAG, "boot: acquire wakelock failed", t)
            null
        }

        Thread({
            var rc = DfChain.RC_PATCH_FAIL
            // ── 运行记录 begin（DF 源）────────────────────────────────────────────
            // 开机自启跑链同样是一条「运行」，必须能被运行记录页看到 ——
            // 这一类**最**需要留痕：无人值守、没有界面，失败了用户完全无从知晓，
            // 只会表现为「这次开机怎么没有 root」。
            // 用 deCtx：开机早期（锁屏）只有 DE 可写；RunLogStore 内部也固定走 DE 目录。
            val attempt = RunLogStore.begin(
                deCtx,
                RunSource.DF,
                RunLogStore.defaultModel(),
                RunLogStore.defaultFirmware(),
            )
            try {
                rc = DfChain.run(deCtx, this, softReboot, ksuNext)
                // 「本次开机实际注入所用变体」落盘（**仅真正注入成功那一次**）——
                // 这是 df 链模式下「当前工作变体」的唯一可靠来源，供主页红/蓝按钮的
                // 「切换下次开机管理器」确认窗判定用，见 DfRootPrefs.KEY_ARMED_VARIANT。
                // 只认 RC_OK：RC_ALREADY_ARMED 说明本次注入是更早那一次干的，不能覆盖。
                if (rc == DfChain.RC_OK) DfRootPrefs.setArmedVariant(deCtx, ksuNext)
                Log.i(TAG, "boot: exploit rc=$rc ksuNext=$ksuNext softReboot=$softReboot")
            } catch (t: Throwable) {
                Log.e(TAG, "boot: exploit exception", t)
            } finally {
                try {
                    wakeLock?.release()
                } catch (t: Throwable) {
                    Log.w(TAG, "boot: release wakelock failed", t)
                }
            }
            // 运行记录收尾：结局判据与手动跑链（DfRootManager.runChain）保持一致
            // —— rc=0（成功）与 rc=9（本次开机已注入）都算成功。
            // detail 前缀「开机自启」：一眼区分这条是自动跑的，还是用户点了按钮。
            val ok = rc == DfChain.RC_OK || rc == DfChain.RC_ALREADY_ARMED
            RunLogStore.finish(
                deCtx,
                attempt,
                if (ok) RunOutcome.SUCCESS else RunOutcome.FAILED,
                "开机自启 · " + rcSummary(rc),
                synchronized(bootLogLines) { bootLogLines.toList() },
            )
            // 失败 ⇒ 累计「连续失败次数」（与手动跑链同一入口，见
            // DfRootManager.handleChainFailure KDoc）。用 DE context：锁屏期 CE 不可写 ——
            // 那正是本接收器存在的前提。
            // ★ 2026-10-08：只有**连续第 3 次**失败才真正关总开关；第 1、2 次只计数 ⇒
            //   下次开机仍会再跑一遍（这正是用户要的「一次不成功不代表下次不成功」）。
            if (rc == DfChain.RC_KSUD_ERROR || rc == DfChain.RC_TIMEOUT || rc == DfChain.RC_PATCH_FAIL) {
                DfRootManager.handleChainFailure(deCtx, rc)
            }
            // 成功 ⇒ 连续失败计数清零（2026-10-08）。**这一路必须调** ——
            // 开机自启不走 DfRootManager.runChain，漏了它就会出现
            // 「开机成功过一次、计数却跨成功累计」的隐蔽缺陷（见 handleChainSuccess KDoc）。
            if (ok) DfRootManager.handleChainSuccess(deCtx)
        }, "dfroot-boot").start()
    }

    /**
     * 复查并刷新「缺少管理器」提醒（发 / 撤）。
     *
     * <h2>为什么单独抽出来、且**必须**在 `/dev/df` 早退之前调用</h2>
     * 本接收器同时监听 `LOCKED_BOOT_COMPLETED`（锁屏期）与 `BOOT_COMPLETED`（解锁后）。
     * 提醒的判定与撤销都必须在**解锁后那一趟**真正跑一遍，否则会有两个坑：
     * <ol>
     *   <li><b>锁屏期判定不可信</b>：`LOCKED_BOOT_COMPLETED` 阶段用户处于 locked，
     *       系统「按用户状态只匹配可运行组件」—— 非 `directBootAware` 的管理器
     *       （KernelSU-Next，内嵌 APK 实测无该属性）会被判成「未安装」，
     *       于是发出**不该发**的提醒。故此处显式要求「用户已解锁」才判定。</li>
     *   <li><b>撤销窗口会被早退吃掉</b>：若上一趟（锁屏期）已跑成功并建了 `/dev/df`，
     *       解锁后这趟进来会命中 `isArmedThisBoot()` 早退 —— 若提醒逻辑排在其后，
     *       「已装 ⇒ 撤销提醒」就**永远执行不到**，误发的提醒会一直挂在通知栏。
     *       这正是用户 2026-10-07 报障的另一半：提醒发出后不消失。</li>
     * </ol>
     *
     * <p>因此顺序固定为：**先刷新提醒（解锁后），再判 `/dev/df` 早退**。
     * 解锁与否用 [android.os.UserManager.isUserUnlocked] 判定 —— 它比「看是哪个 action」
     * 更准：部分机型/无凭据设备上 `LOCKED_BOOT_COMPLETED` 时用户其实已是解锁态。
     */
    private fun refreshManagerNotice(context: Context, deCtx: Context) {
        val um = context.getSystemService(Context.USER_SERVICE) as? android.os.UserManager
        val unlocked = try {
            um?.isUserUnlocked ?: true   // 取不到（极老 API/异常）⇒ 按解锁处理，退回旧行为
        } catch (_: Throwable) {
            true
        }
        if (!unlocked) {
            Log.i(TAG, "boot: user locked — defer manager notice to BOOT_COMPLETED")
            return
        }

        val ksuNext = DfRootPrefs.isKsuNext(deCtx)
        // ── round35：缺管理器 ⇒ 发通知提醒（**不**尝试安装）──────────────────────
        //  本机无界面（锁屏期就被拉起），而 ③ PackageInstaller 通道要拉起系统安装确认窗，
        //  既拉不起来也不该在用户刚开机时无声无息地弹东西 ⇒ 用户 2026-10-06 拍板：
        //  开机只**提醒**，由用户打开 SRK 后点按钮完成安装。
        //  链**照跑**：root 是主线目标，不能被「管理器还没装」挡住 ——
        //  这正是方案 B「安装提到跑链之前」换来的自由度（不再依赖事后补装）。
        if (!DfManagerInstaller.isInstalled(deCtx, ksuNext)) {
            Log.i(TAG, "boot: manager missing, notify user (no install from receiver)")
            try {
                DfManagerNotice.notifyMissingManager(context, ksuNext)
            } catch (t: Throwable) {
                Log.w(TAG, "boot: notify missing manager failed", t)
            }
        } else {
            // 已装 ⇒ 撤掉可能残留的旧提醒（比如上次开机缺、用户后来手动装上了；
            // 或锁屏期误发但当时判不出来的那条）。取消是幂等的。
            try { DfManagerNotice.cancel(context) } catch (t: Throwable) {
                Log.w(TAG, "boot: cancel notice failed", t)
            }
        }
    }

    /** rc → 短摘要（措辞与 [DfRootResult.summary] 保持一致，便于两处日志对照）。 */
    private fun rcSummary(rc: Int): String = when (rc) {
        DfChain.RC_OK -> "成功获取 Root"
        DfChain.RC_ALREADY_ARMED -> "本次开机已注入（无需重复）"
        DfChain.RC_KSUD_ERROR -> "失败：ksud 执行出错"
        DfChain.RC_TIMEOUT -> "失败：执行超时"
        DfChain.RC_PATCH_FAIL -> "失败：文件补丁失败/异常"
        else -> "未知结果（rc=$rc）"
    }

    private companion object {
        const val TAG = "DFROOT"
    }
}

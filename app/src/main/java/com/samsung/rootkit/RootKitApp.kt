package com.samsung.rootkit

import android.app.Application
import com.samsung.rootkit.data.PayloadManager
import com.samsung.rootkit.data.RunLogStore
import com.samsung.rootkit.dfroot.DfPostRootCleaner
import com.samsung.rootkit.dfroot.DfRootBridge
import com.samsung.rootkit.dfroot.DfRootPrefs
import com.samsung.rootkit.exploit.RootExecutor
import com.samsung.rootkit.shizuku.ShizukuManager
import com.samsung.rootkit.R.drawable

class RootKitApp : Application() {
    lateinit var shizukuManager: ShizukuManager
    lateinit var payloadManager: PayloadManager
    lateinit var rootExecutor: RootExecutor

    override fun onCreate() {
        super.onCreate()
        instance = this
        shizukuManager = ShizukuManager(this)
        payloadManager = PayloadManager(this)
        rootExecutor = RootExecutor(this, shizukuManager, payloadManager)
        recoverInterruptedRunLog()
    }

    /**
     * 收尾上次没跑完的运行记录（SRK / df / dfe 三源共用，见 [RunLogStore]）。
     *
     * <p>为什么放在 Application 而不是运行记录页：
     * <ol>
     *   <li>「跑到一半就退出」的记录**必须在下次启动时**才可能被判定 —— 退出那一刻进程已经没了，
     *       没人能替它写回执；</li>
     *   <li>本进程并不只在用户打开 App 时启动：df 的**开机自启**（DfBootReceiver）同样会拉起它，
     *       那种场景用户压根不会进运行记录页，只有在这里做才不会漏；</li>
     *   <li>df 链的软重启会杀进程，而「链跑完」到「写回执」之间存在窗口期，
     *       恰好在窗口期被杀的那些记录只能靠这里收尾。</li>
     * </ol>
     * 放子线程做磁盘 IO，不拖慢冷启动。
     *
     * <h2>★ 顺序仍是硬要求：先做完「接力动作」，再收尾运行记录</h2>
     * [RunLogStore.recoverPending] 一旦执行，那条记录就定格了 —— 所以任何要并进日志的
     * 动作都必须排在它**之前**。
     *
     * <h2>round35：接力动作只剩一项</h2>
     * 原先这里还要「补装管理器」（round30：跑链**后**软重启 ⇒ 靠持久化标记跨进程接力）。
     * 用户 2026-10-06 改走**方案 B** —— 安装提到跑链**之前**、走 ③ PackageInstaller
     * （前台、一次系统确认，装不成当场中止不跑链）⇒ 不再有跨进程待办，
     * com.samsung.rootkit.dfroot.DfManagerProvisioner 已整段删除。
     * 剩下的唯一接力动作是 round34 的「提权成功后清理 /data/local/tmp」
     * （被软重启杀掉的那一路，见 `RootKitScreen.runDfRootChain`）。
     */
    private fun recoverInterruptedRunLog() {
        Thread({
            try {
                recoverNow()
            } catch (_: Throwable) {
                // 日志收尾 / 接力清理失败绝不能影响 App 启动
            }
        }, "srk-runlog-recover").start()
    }

    /**
     * [recoverInterruptedRunLog] 的实际工作体（只在上面那个后台线程里跑）。
     *
     * <p>拆成独立方法是为了能用**普通的 `return`** 提前退出 —— 写在 `Thread({ … })` 的
     * lambda 里就只能用 `return@Thread`，而该隐式标签对「以构造器形式接收 SAM lambda」
     * 的写法是否成立并不确定（本工程无编译器时无法验证），没必要冒这个险。
     */
    private fun recoverNow() {
        // ── 前置闸门：用户已解锁？────────────────────────────────────────────
        // 本进程并不只在用户打开 App 时启动：df 的**开机自启**（DfBootReceiver）会在
        // LOCKED_BOOT_COMPLETED（用户还没解锁）时把它拉起。那种状态下：
        //   · `pm install` 并不可靠（PMS 正处于 Direct Boot 阶段），装出来的包
        //     在解锁前也无法运行；而一旦失败会清掉待补装标记 ⇒ 功能静默丢失；
        //   · 更关键的是：**运行记录也不能在那时定格** —— 否则就变成
        //     「管理器还没装，日志却已经结束了」，与用户要求的顺序相反。
        // 所以锁屏期**整段推迟**：标记与暂存记录都原样留着，等解锁后的下一次
        // 进程启动一起处理（用户必然会打开 App 看这次 root 的结果，那一次就是解锁态）。
        val unlocked = try {
            getSystemService(android.os.UserManager::class.java)?.isUserUnlocked ?: true
        } catch (_: Throwable) {
            true   // 取不到就按「已解锁」放行：不能因为读不到状态而永久不做事
        }
        if (!unlocked) {
            android.util.Log.i(
                "DFROOT",
                "run-log recover deferred: user still locked (marker & pending record kept)",
            )
            return
        }

        // 本次开机 `/dev/df` 已存在 ⇒ 上次那条 df 记录其实注入成功了，
        // 只是回执没写成，应记「成功」而非「中断」（判据详见 RunLogStore.recoverPending）。
        val armed = try { DfRootBridge.isArmedThisBoot() } catch (_: Throwable) { false }

        // ①（round35 已删除）补装管理器。
        //    原设计是「跑链后补装」：链会软重启杀进程 ⇒ 落持久化标记 ⇒ 本次启动接力。
        //    用户 2026-10-06 改走**方案 B**：安装提到跑链**之前**、走 ③ PackageInstaller，
        //    装不装得成当场就知道 ⇒ 跨进程记忆整段不需要了（DfManagerProvisioner 已删）。

        // ② 提权成功后的 /data/local/tmp 清理（round34 需求② 的「下次打开 SRK 时」那一路）。
        //    链成功**且**开着自动软重启时，跑链那次落下了待清理标记
        //    （见 RootKitScreen.runDfRootChain 的「③」段）—— 本进程被软重启杀掉了，
        //    标记是唯一线索。用户原话：「自动软重启后肯定已经是 root 状态」，
        //    故此刻清理才做得动；这也正是这一路存在的前提。
        //    与补装一样是阻塞调用（su → Shizuku），本方法本来就跑在后台线程。
        var clean: DfPostRootCleaner.Result? = null
        if (armed && DfRootPrefs.isTmpCleanArmed(this)) {
            clean = try {
                DfPostRootCleaner.cleanNow(shizukuManager)
            } catch (_: Throwable) {
                null
            }
            // 成败都清标记：失败原因已随下面的 lines 写进运行记录、对用户可见；
            // 留着它只会让每次冷启动都白跑一遍清理尝试（与补装标记同一取舍）。
            try { DfRootPrefs.clearTmpCleanArmed(this) } catch (_: Throwable) {}
        } else if (!armed) {
            // 没提权成功 ⇒ 待清理标记作废（不会再有「提权成功后」这一刻了）。
            try { DfRootPrefs.clearTmpCleanArmed(this) } catch (_: Throwable) {}
        }

        // ③ 收尾运行记录；把清理过程一并写进那条记录。
        //   ★ round35：不再按「管理器有没有装」改写结局 ——
        //     方案 B 下管理器在跑链**之前**就已装好（装不成当场就中止、根本不会跑链），
        //     所以「已注入」本身就足以代表整轮流程走完，沿用 RunLogStore 的原判据。
        RunLogStore.recoverPending(
            this,
            armed,
            extraLogLines = clean?.lines ?: emptyList(),
            finalOutcome = null,
            finalDetail = null,
        )
    }

    companion object {
        lateinit var instance: RootKitApp
            private set

        // 防止 R8 shrinkResources 删除 launcher 图标资源（Manifest引用在release模式下可能被忽略）
        @Suppress("unused")
        private val KEEP_LAUNCHER_RESOURCES = arrayOf(
            drawable.ic_launcher_foreground,
            drawable.ic_launcher_foreground_homepage,
        )
    }
}

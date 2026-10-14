package com.samsung.rootkit.dfroot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log

/**
 * [DfManagerInstaller] ③ 通道的**安装回执接收器**（manifest 注册，`exported=false`）。
 *
 * <h2>为什么必须有一个接收器</h2>
 * `PackageInstaller.Session#commit` 是异步的：它只把会话交给系统，结果通过
 * `PendingIntent` 回投。没有接收器就**永远不知道**用户到底点了「安装」还是「取消」，
 * 也就无从执行用户拍板的「取消 ⇒ 中止本次流程，不跑链」。
 *
 * <h2>两类回执的不同处理（关键）</h2>
 * <ol>
 *   <li>`STATUS_PENDING_USER_ACTION` —— 系统把「要不要装这个应用」的确认窗以
 *       [Intent.EXTRA_INTENT] 回传，**必须由本进程自己 `startActivity` 拉起来**。
 *       这不是终态 ⇒ **不终结** [DfManagerInstaller] 里那次挂起，继续等用户操作后的终态。</li>
 *   <li>其余状态（SUCCESS / FAILURE_*）—— 终态 ⇒ 回调 [DfManagerInstaller.complete]。</li>
 * </ol>
 *
 * <h2>为什么不开后台启动白名单也够用</h2>
 * ③ 通道整段都在**前台**（用户刚点了主页按钮，SRK 在前台）⇒ 从接收器起 Activity
 * 不受 Android 10+ 的后台启动限制。这正是「把安装提到跑链之前」换来的红利。
 */
class DfManagerInstallReceiver : BroadcastReceiver() {

    @Suppress("DEPRECATION") // minSdk 31 < 33 ⇒ 只能用非泛型重载（泛型版要 API 33）
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DfManagerInstaller.ACTION_INSTALL_STATUS) return
        val sessionId = intent.getIntExtra(DfManagerInstaller.EXTRA_SESSION_ID, -1)
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE,
        )
        val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        Log.i(TAG, "mgr install receipt: session=$sessionId status=$status msg=$msg")

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm: Intent? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    } else {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT)
                    }
                if (confirm == null) {
                    // 拿不到确认 Intent 就等于卡死在这里 —— 绝不能让调用方永远挂起，
                    // 直接按「取消」终结（与用户拍板的语义一致：没装成 ⇒ 不跑链）。
                    Log.w(TAG, "mgr install: PENDING_USER_ACTION without EXTRA_INTENT")
                    DfManagerInstaller.complete(
                        sessionId, PackageInstaller.STATUS_FAILURE_ABORTED, "系统未给出安装确认界面",
                    )
                    return
                }
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(confirm)
                    Log.i(TAG, "mgr install: system confirm UI launched")
                } catch (t: Throwable) {
                    Log.w(TAG, "mgr install: launch confirm UI failed", t)
                    DfManagerInstaller.complete(
                        sessionId, PackageInstaller.STATUS_FAILURE,
                        "无法拉起系统安装确认界面：${t.javaClass.simpleName}",
                    )
                }
                // ★ 有意不 complete —— 等用户在系统窗上操作后的**终态**再终结。
            }
            else -> DfManagerInstaller.complete(sessionId, status, msg)
        }
    }

    private companion object {
        const val TAG = "DFROOT"
    }
}

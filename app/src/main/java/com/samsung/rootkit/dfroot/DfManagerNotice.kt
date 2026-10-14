package com.samsung.rootkit.dfroot

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.samsung.rootkit.data.PayloadManager
import com.samsung.rootkit.ui.MainActivity

/**
 * 「开机自启缺管理器」的**提醒通知**。
 *
 * <h2>为什么开机这条路径不能装、只能提醒</h2>
 * [DfBootReceiver] 在 `LOCKED_BOOT_COMPLETED` 就被拉起，**没有界面、没有前台 Activity**，
 * 而 ③ 通道（[DfManagerInstaller]）必须拉起系统安装确认窗 —— 无界面时既拉不起来，
 * 也不该在用户刚开机时无声无息地弹东西。
 * ⇒ 用户 2026-10-06 拍板：开机发现缺管理器时**发一条通知**引导用户打开 SRK，
 *    由用户在 SRK 里点按钮完成安装。链本身**照跑**（root 是主线，不被管理器挡住）。
 *
 * <h2>通知权限</h2>
 * API 33+ 需要 `POST_NOTIFICATIONS`（manifest 已声明，运行时在用户打开「DFRoot 开机自启」
 * 时申请，见 [DfManagerInstaller.requestNotificationPermissionIfNeeded]）。
 * 未授权时 [notifyMissingManager] 会**静默返回并记日志** —— 通知是提醒，不是主流程，
 * 绝不能因为缺权限就让开机跑链失败。
 */
object DfManagerNotice {

    private const val TAG = "DFROOT"
    private const val CHANNEL_ID = "df_manager_missing"
    private const val CHANNEL_NAME = "缺少管理器提醒"
    /** 通知 id（固定 ⇒ 重复提醒只会替换，不会堆叠）。 */
    private const val NOTIF_ID = 0x4D4752 // 'MGR'

    /** 建渠道（Android 8+；重复调用无副作用）。 */
    private fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = NotificationManagerCompat.from(ctx)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val ch = android.app.NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, android.app.NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "DFRoot 开机自启时若缺少对应管理器，在此提醒安装"
            enableVibration(true)
        }
        runCatching { nm.createNotificationChannel(ch) }
            .onFailure { Log.w(TAG, "create notification channel failed", it) }
    }

    /**
     * 发「缺少管理器」提醒。
     *
     * @param ksuNext 决定提醒里说的是 KernelSU 还是 KernelSU-Next
     * @return 是否真的发出去了
     */
    fun notifyMissingManager(ctx: Context, ksuNext: Boolean): Boolean {
        if (!DfManagerInstaller.canPostNotifications(ctx)) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted — skip missing-manager notice")
            return false
        }
        val variant = if (ksuNext) PayloadManager.KsuVariant.KSU_NEXT
        else PayloadManager.KsuVariant.KSU
        val name = PayloadManager.variantDisplayName(variant)
        ensureChannel(ctx)

        val content = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val tap = PendingIntent.getActivity(ctx, NOTIF_ID, content, flags)

        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("缺少 $name 管理器")
            .setContentText("DFRoot 已取得 Root，但系统内没有 $name 管理器。点此打开 SRK 安装。")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "DFRoot 开机自启已取得 Root，但系统内没有检测到 $name 管理器。\n" +
                        "点此打开 SRK，在主页点击对应按钮即可完成安装（需点一次系统安装确认）。",
                ),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        return try {
            NotificationManagerCompat.from(ctx).notify(NOTIF_ID, n)
            Log.i(TAG, "missing-manager notice posted ($name)")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "post missing-manager notice failed", t)
            false
        }
    }

    /** 已装上管理器 / 已关闭自启 ⇒ 撤掉提醒，避免陈旧通知一直挂着。 */
    fun cancel(ctx: Context) {
        runCatching { NotificationManagerCompat.from(ctx).cancel(NOTIF_ID) }
            .onFailure { Log.w(TAG, "cancel notice failed", it) }
    }
}

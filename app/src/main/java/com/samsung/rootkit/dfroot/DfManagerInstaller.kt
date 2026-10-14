package com.samsung.rootkit.dfroot

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.samsung.rootkit.data.PayloadManager
import df.root.ExploitRunner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * DF 链路的**管理器安装通道 ③**：`PackageInstaller` 会话 + `REQUEST_INSTALL_PACKAGES`
 * + `PendingIntent`（会弹一次系统安装确认，用户点「安装」即完成）。
 *
 * <h2>为什么必须有这条通道</h2>
 * 前两条在真机上都不成立：
 * <ol>
 *   <li>①`su` / ②`Shizuku` —— 缺管理器的场景正是「**还没 root**」，
 *       `su` 结构性不可用（round31 实测：late-load 不注册 sucompat kprobe、本应用不在
 *       allowlist、`/data/adb` 对普通身份 0700 不可达）；Shizuku 则默认没装。</li>
 *   <li>链内（round33 的 wrapper「1b 段」）—— UMH 里 `pm install` 实测装不上，
 *       round35 已整段删除。</li>
 * </ol>
 * ⇒ **③ 是唯一一条「无需 root、无需第三方、必然可用」的通道**，代价只是要点一次确认。
 *
 * <h2>时序：为什么提到「跑链之前」</h2>
 * 用户 2026-10-06 拍板走**方案 B**：检测 → 装 → 才跑链。理由是 Next 路跑链成功会
 * **主动软重启并杀掉本进程**，而 ③ 依赖「应用在前台 + 用户交互」——链跑完后这个条件
 * 可能已经没了。放在跑链前，全程都在前台，一次交互即可覆盖所有变体与所有软重启设置。
 *
 * <h2>取消 / 失败的语义</h2>
 * 用户拍板：**取消或安装失败 ⇒ 中止本次流程，不跑链**。
 * 因此 [Result.passed] 只在 `ALREADY` / `INSTALLED` 时为 true，调用方据此决定是否继续。
 *
 * <h2>包名纪律</h2>
 * `applicationId = "com.samsung.root.kit"` 而 `namespace = "com.samsung.rootkit"`
 * （两者**不同**）⇒ 任何显式组件都必须 `ComponentName(ctx, X::class.java)`，
 * 广播也必须 `.setPackage(ctx.packageName)` 显式限定本包，不能靠隐式广播。
 */
object DfManagerInstaller {

    private const val TAG = "DFROOT"

    /** 安装回执广播的 action（只投给本包，见 [buildStatusIntent]）。 */
    internal const val ACTION_INSTALL_STATUS = "com.samsung.root.kit.DF_MANAGER_INSTALL_STATUS"
    /** 回执里带回 sessionId，用于唤醒对应的一次挂起调用。 */
    internal const val EXTRA_SESSION_ID = "df_manager_session_id"

    /**
     * 等待用户在系统确认窗上操作的上限。
     *
     * <p>给足 5 分钟：用户可能把 SRK 切到后台去查「这个管理器是什么」再回来点。
     * 超时按「取消」处理（[Outcome.CANCELLED]）—— 超时不等于失败，不写任何失败记录。
     */
    private const val AWAIT_TIMEOUT_MS = 5 * 60 * 1000L

    /** 会话名（PackageInstaller 里的 APK 条目名，任意字符串）。 */
    private const val SESSION_ENTRY = "df_manager.apk"

    /** 正在进行的安装：sessionId → 等待者。由 [DfManagerInstallReceiver] 终结。 */
    private val pending = ConcurrentHashMap<Int, CompletableDeferred<Pair<Int, String>>>()

    /**
     * 结果。
     *
     * @param passed 是否放行（只有「本来就装了」和「本轮装上」为 true）。
     * @param lines  供运行记录显示的中文行（与项目既有风格一致：日志行是中文字面量）。
     */
    enum class Outcome { ALREADY, INSTALLED, CANCELLED, FAILED, NO_PERMISSION }

    data class Result(
        val outcome: Outcome,
        val pkg: String,
        val displayName: String,
        val lines: List<String>,
    ) {
        /**
         * 闸门判据 —— **取消与失败都必须中止流程**（用户 2026-10-06 拍板：
         * 「用户在系统安装确认窗点取消（或安装失败）时，中止本次流程，不跑链」）。
         */
        val passed: Boolean get() = outcome == Outcome.ALREADY || outcome == Outcome.INSTALLED
    }

    /**
     * 系统内是否已装当前变体对应的管理器。
     *
     * <p>判据复用 [ExploitRunner.isManagerInstalled]（**单一事实源**：包已装且有启动入口）。
     */
    fun isInstalled(ctx: Context, ksuNext: Boolean): Boolean =
        ExploitRunner.isManagerInstalled(ctx, ksuNext)

    /**
     * 是否持有「安装未知应用」授权（Android 8+ 的 `REQUEST_INSTALL_PACKAGES`）。
     *
     * <p>没有它，`PackageInstaller` 会话**根本创建不出来**（直接抛 SecurityException），
     * 所以必须在开会话**之前**预检并引导用户去设置页授权。
     */
    fun canRequestInstalls(ctx: Context): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    } catch (t: Throwable) {
        Log.w(TAG, "canRequestPackageInstalls probe failed", t)
        true // 探针本身出错时不拦（真正开会话失败会有明确回执）
    }

    /**
     * 跳「允许安装未知应用」设置页（本应用那一项）。
     * 调用方应在返回前台后重试（SRK 有 ON_RESUME 观察者机制）。
     */
    fun openUnknownSourcesSettings(ctx: Context) {
        val i = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:" + ctx.packageName),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(i)
        } catch (t: Throwable) {
            Log.w(TAG, "open unknown-source settings failed", t)
        }
    }

    /** 通知权限（API 33+）是否已授予 —— 开机缺管理器的提醒依赖它。 */
    fun canPostNotifications(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * 申请通知权限（仅在 [activity] 真的是 Activity 时才发起；否则静默跳过）。
     *
     * <p>只在用户打开「DFRoot 开机自启」时调用 —— 那条路径是唯一会用到通知的地方，
     * 在别处弹权限请求既无必要也惹人烦。结果不回传也无所谓（通知只是提醒，best-effort）。
     */
    fun requestNotificationPermissionIfNeeded(activity: Context?) {
        if (activity !is Activity) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (canPostNotifications(activity)) return
        try {
            activity.requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0x4452, // 'DF'
            )
        } catch (t: Throwable) {
            Log.w(TAG, "request POST_NOTIFICATIONS failed", t)
        }
    }

    /**
     * 确保当前变体的管理器已安装；缺则走 ③ 通道安装，**挂起直到拿到系统回执**。
     *
     * <p>必须在协程里调用（内部会挂起等用户在系统确认窗上操作）。
     *
     * @param ctx      **前台** context（系统确认窗由本进程拉起）
     * @param pm       载荷管理器（读内置管理器 APK 字节）
     * @param ksuNext  true = KernelSU-Next，false = 官方 KernelSU
     */
    suspend fun install(ctx: Context, pm: PayloadManager, ksuNext: Boolean): Result {
        val variant = if (ksuNext) PayloadManager.KsuVariant.KSU_NEXT
        else PayloadManager.KsuVariant.KSU
        val pkg = PayloadManager.managerPackage(variant)
        val name = PayloadManager.variantDisplayName(variant)
        val lines = mutableListOf<String>()

        // ── ① 已装 ⇒ 零副作用直接放行 ──
        if (isInstalled(ctx, ksuNext)) {
            lines += "$name 管理器（$pkg）已安装，跳过安装"
            return Result(Outcome.ALREADY, pkg, name, lines)
        }

        // ── ② 缺「安装未知应用」授权 ⇒ 引导，本次不放行 ──
        if (!canRequestInstalls(ctx)) {
            lines += "缺少「安装未知应用」授权，无法安装 $name 管理器"
            return Result(Outcome.NO_PERMISSION, pkg, name, lines)
        }

        // ── ③ 读内置 APK（assets，IO 线程）──
        val bytes = withContext(Dispatchers.IO) {
            runCatching { pm.getKsuApk(variant) }.getOrNull()
        }
        if (bytes == null || bytes.isEmpty()) {
            lines += "读取内置 $name 管理器 APK 失败"
            return Result(Outcome.FAILED, pkg, name, lines)
        }
        lines += "已读取内置 $name 管理器 APK（${bytes.size} 字节），准备安装"

        val pi = ctx.packageManager.packageInstaller
        val sessionId = try {
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            params.setAppPackageName(pkg)
            pi.createSession(params)
        } catch (t: Throwable) {
            lines += "创建安装会话失败：${t.javaClass.simpleName}: ${t.message}"
            return Result(Outcome.FAILED, pkg, name, lines)
        }

        val deferred = CompletableDeferred<Pair<Int, String>>()
        pending[sessionId] = deferred

        try {
            pi.openSession(sessionId).use { s ->
                // 流式写入 + **session.fsync**（★ fsync 是 Session 的实例方法，
                // OutputStream 上没有；漏了它写入可能还没落盘就 commit）
                s.openWrite(SESSION_ENTRY, 0, bytes.size.toLong()).use { out ->
                    out.write(bytes)
                    s.fsync(out)
                }
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                // ★ FLAG_MUTABLE 必须：系统要给这个 PendingIntent 回填 EXTRA_STATUS 等，
                //   不可变（Android 12+ 默认）的话回填会被丢弃 ⇒ 永远收不到回执。
                val sender = PendingIntent.getBroadcast(
                    ctx, sessionId, buildStatusIntent(ctx, sessionId), flags,
                ).intentSender
                s.commit(sender)
            }
        } catch (t: Throwable) {
            pending.remove(sessionId)
            lines += "提交安装会话失败：${t.javaClass.simpleName}: ${t.message}"
            return Result(Outcome.FAILED, pkg, name, lines)
        }
        lines += "已提交安装会话（sessionId=$sessionId），等待系统安装确认…"

        // ── ④ 挂起等回执 ──
        // PENDING_USER_ACTION 由接收器直接拉起系统确认窗**且不终结本 Deferred**，
        // 所以这里收到的一定是终态（SUCCESS / ABORTED / FAILURE …）。
        val pair = withTimeoutOrNull(AWAIT_TIMEOUT_MS) { deferred.await() }
        if (pair == null) {
            pending.remove(sessionId)
            lines += "等待安装回执超时（${AWAIT_TIMEOUT_MS / 1000}s），按取消处理"
            return Result(Outcome.CANCELLED, pkg, name, lines)
        }
        val (status, msg) = pair
        if (msg.isNotBlank()) lines += "系统回执：$msg"

        // ── ⑤ 事实判据：不看命令退出码，看**装完后再查一次**在不在 ──
        //   PackageInstaller 的 STATUS_SUCCESS 只代表会话提交成功；真正可信的是
        //   「包现在能不能被 LaunchIntent 找到」（与已删除的 DfManagerProvisioner 同纪律）。
        return when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                val real = withContext(Dispatchers.IO) {
                    runCatching { isInstalled(ctx, ksuNext) }.getOrDefault(false)
                }
                if (real) {
                    lines += "$name 管理器安装完成"
                    Result(Outcome.INSTALLED, pkg, name, lines)
                } else {
                    lines += "系统返回安装成功，但复查未找到 $name 管理器（按失败处理）"
                    Result(Outcome.FAILED, pkg, name, lines)
                }
            }
            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                lines += "用户在系统安装确认窗取消，本次未执行提权"
                Result(Outcome.CANCELLED, pkg, name, lines)
            }
            else -> {
                lines += "管理器安装失败（status=$status${if (msg.isNotBlank()) " · $msg" else ""}）"
                Result(Outcome.FAILED, pkg, name, lines)
            }
        }
    }

    /** 回执广播的 Intent：action + sessionId + **显式限定本包**（不广播给其它 App）。 */
    private fun buildStatusIntent(ctx: Context, sessionId: Int): Intent =
        Intent(ACTION_INSTALL_STATUS)
            .setPackage(ctx.packageName)
            .putExtra(EXTRA_SESSION_ID, sessionId)
            .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)

    /**
     * 由 [DfManagerInstallReceiver] 回调：终结对应 sessionId 的那次挂起。
     *
     * @param status PackageInstaller 的 `STATUS_*`
     * @param message 系统附带的 `EXTRA_STATUS_MESSAGE`
     */
    internal fun complete(sessionId: Int, status: Int, message: String?) {
        val d = pending.remove(sessionId)
        if (d == null) {
            Log.w(TAG, "mgr install: no waiter for session $sessionId (status=$status)")
            return
        }
        d.complete(status to (message ?: ""))
    }
}

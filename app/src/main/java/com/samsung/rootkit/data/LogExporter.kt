package com.samsung.rootkit.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 运行日志导出：把「设备信息 + App 版本 + 运行记录」汇总成一个 txt，
 * 通过 FileProvider 走系统分享面板发出去（微信 / QQ / 电报 / 邮件均可），
 * 让测试者能把现场一次性回传，避免来回追问。
 *
 * 约束与取舍：
 * - 不申请任何存储权限：分享走 FileProvider + 系统分享面板，
 *   另存为走 SAF（`ACTION_CREATE_DOCUMENT`，由调用方发起后把 Uri 交给 [writeToUri]）。
 *   文件写在 cacheDir/log_exports/ 下，命中 file_paths.xml 里已声明的 `<cache-path>`。
 * - 只导出文本，不读取任何设备唯一标识（IMEI / 序列号 / ANDROID_ID 等一律不碰）。
 * - （round25 起本 App 已申请 `INTERNET` —— 那是 df 的 IPsec 通道所需，
 *   与导出无关：导出全程仍然只走本地文件 + SAF，不发任何网络请求。）
 */
object LogExporter {

    private const val DIR_NAME = "log_exports"
    private const val KSU_PKG = "me.weishu.kernelsu"
    private const val KSU_NEXT_PKG = "com.rifsxd.ksunext"

    private val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val fileStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /**
     * 组装**全量**排查报告（纯文本，含所有运行记录）。任何一步失败都不抛异常，尽量把能拿到的都带上。
     *
     * <p>⚠️ 2026-10-05 起 UI 侧已无调用点：设置页那张「导出运行日志」卡片被取消，
     * 导出/分享改为在「运行记录」页**长按某一条**记录触发（走 [buildAttemptReport]）。
     * 本方法作为「一次性拿走全部现场」的能力保留（R8 在 release 下会按 unused 裁掉，无体积成本）。
     */
    fun buildReport(context: Context): String {
        val sb = StringBuilder(16 * 1024)
        appendHeader(sb, context, "SamsungRootKit 运行日志导出")

        val attempts = try {
            RunLogStore.loadAttempts(context)
        } catch (_: Exception) {
            emptyList()
        }
        sb.appendLine("----- 运行记录（共 " + attempts.size + " 次，按时间倒序）-----")
        if (attempts.isEmpty()) {
            sb.appendLine("(暂无记录：一次运行结束后才会写入)")
        } else {
            attempts.forEachIndexed { index, a ->
                appendAttempt(sb, index + 1, a)
            }
        }

        sb.appendLine()
        sb.appendLine("===== END =====")
        return sb.toString()
    }

    /**
     * 单条记录的报告（运行记录页「长按 → 导出 / 分享」用）。
     *
     * <p>带上设备与 App 段，是因为接收方多半只拿到这一条 txt，
     * 没有设备上下文就没法判断「这台机器上到底发生了什么」。
     */
    fun buildAttemptReport(context: Context, attempt: RunAttempt): String {
        val sb = StringBuilder(8 * 1024)
        appendHeader(sb, context, "SamsungRootKit 运行记录")
        sb.appendLine("----- 运行记录 -----")
        appendAttempt(sb, 1, attempt)
        sb.appendLine()
        sb.appendLine("===== END =====")
        return sb.toString()
    }

    /** 导出文件名（不含目录）：`srk_log_<时间戳>.txt` / `srk_run_<时间戳>.txt`。 */
    fun fileName(prefix: String = "srk_log"): String =
        prefix + "_" + fileStamp.format(Date()) + ".txt"

    /** 写盘，返回文件；失败返回 null（不抛异常，调用方按 null 处理） */
    fun exportToFile(context: Context, text: String, prefix: String = "srk_log"): File? = try {
        val dir = File(context.cacheDir, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        val f = File(dir, fileName(prefix))
        f.writeText(text)
        f
    } catch (_: Exception) {
        null
    }

    /** 把文本写进 SAF 返回的 Uri（「导出到文件」用）。成功 true。 */
    fun writeToUri(context: Context, uri: Uri, text: String): Boolean = try {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
            out.flush()
        }
        true
    } catch (_: Exception) {
        false
    }

    /** 拉起系统分享面板，返回是否成功 */
    fun share(context: Context, file: File, subject: String, chooserTitle: String): Boolean = try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, chooserTitle)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
        true
    } catch (_: Exception) {
        false
    }

    // ─────────────────────────── 内部 ───────────────────────────

    private fun appendHeader(sb: StringBuilder, context: Context, title: String) {
        sb.appendLine("===== $title =====")
        sb.appendLine("导出时间 : " + stamp.format(Date()))
        sb.appendLine()

        sb.appendLine("----- 设备 -----")
        sb.appendLine("MODEL       : " + Build.MODEL)
        sb.appendLine("DEVICE      : " + Build.DEVICE)
        sb.appendLine("BRAND       : " + Build.BRAND)
        sb.appendLine("DISPLAY     : " + Build.DISPLAY)
        sb.appendLine("FINGERPRINT : " + Build.FINGERPRINT)
        sb.appendLine("ANDROID     : " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")")
        sb.appendLine("ABI         : " + Build.SUPPORTED_ABIS.joinToString(", "))
        sb.appendLine()

        sb.appendLine("----- App -----")
        sb.appendLine("package     : " + context.packageName)
        try {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            sb.appendLine("versionName : " + pi.versionName)
            @Suppress("DEPRECATION")
            sb.appendLine("versionCode : " + pi.versionCode)
        } catch (_: Exception) {
            sb.appendLine("versionName : (读取失败)")
        }
        sb.appendLine("KSU 管理器  : " + installedVersion(context, KSU_PKG))
        sb.appendLine("Next 管理器 : " + installedVersion(context, KSU_NEXT_PKG))
        sb.appendLine()
    }

    private fun appendAttempt(sb: StringBuilder, index: Int, a: RunAttempt) {
        sb.appendLine()
        sb.appendLine(
            "### #" + index + "  " + stamp.format(Date(a.timestamp)) +
                "  source=" + a.source.name + "  result=" + a.outcome.name
        )
        sb.appendLine("model=" + a.model + "   firmware=" + a.firmware)
        if (a.detail.isNotEmpty()) sb.appendLine("detail=" + a.detail)
        a.logLines.forEach { sb.appendLine("  " + it) }
    }

    private fun installedVersion(context: Context, pkg: String): String = try {
        context.packageManager.getPackageInfo(pkg, 0).versionName ?: "已安装"
    } catch (_: Exception) {
        "未安装"
    }
}

package com.samsung.rootkit.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream

/**
 * Shizuku 权限管理器 —— 通过 Shizuku 获取 ADB 级别的 shell 执行能力。
 *
 * 完全替代 run_root.sh 中通过 `adb shell` 执行命令的方式，
 * 使整个 root 流程可以在手机上独立完成，无需连接电脑。
 *
 * 核心原理：Shizuku 以 shell (UID=2000) 权限运行，
 * 等同于通过 USB 连接的 adb shell，拥有写 /data/local/tmp 的权限。
 */
class ShizukuManager(private val context: Context) {

    companion object {
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val PERMISSION_REQUEST_CODE = 1001

        // IShellService 的 Binder 描述符和 transaction code
        // 来自 Android 框架 android.os.IShell
        private const val SHELL_DESCRIPTOR = "android.os.IShell\$Stub"
        private const val TRANSACTION_EXEC = 1 // transact code for execShellCommand
    }

    /**
     * 获取 IShizukuService —— 通过 Shizuku 的公开 Binder 接口获得 ADB 级别的 shell 执行能力。
     * Shizuku.newProcess() 在 api 桩中被 refine 为 private，因此走 IShizukuService.newProcess()。
     */
    private fun getService(): IShizukuService? {
        return try {
            val binder = Shizuku.getBinder() ?: return null
            if (!binder.pingBinder()) return null
            IShizukuService.Stub.asInterface(binder)
        } catch (e: Exception) {
            null
        }
    }

    /** Shizuku 是否已安装 */
    fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /** Shizuku 服务是否在运行 */
    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 本应用是否已获得 Shizuku 授权
     *
     * 注意：Shizuku 未安装/未启动时 Shizuku.checkSelfPermission() 会抛异常，
     * 必须兜底，否则 App 在“无 Shizuku”场景下进场即闪退。
     */
    fun hasPermission(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Shizuku binder 是否可用（已安装且正在运行）。
     * 整合 isShizukuInstalled + isShizukuRunning，且全程不抛异常。
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            isShizukuInstalled() && Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    /** 请求 Shizuku 授权（异步，结果通过一次性 listener 回调） */
    fun requestPermission(callback: (Boolean) -> Unit) {
        if (hasPermission()) {
            callback(true)
            return
        }
        // 注册一次性 listener 捕获授权结果，避免重复/泄漏
        var listener: Shizuku.OnRequestPermissionResultListener? = null
        listener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            listener?.let { Shizuku.removeRequestPermissionResultListener(it) }
            callback(grantResult == PackageManager.PERMISSION_GRANTED)
        }
        Shizuku.addRequestPermissionResultListener(listener)
        try {
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
        } catch (e: Exception) {
            listener?.let { Shizuku.removeRequestPermissionResultListener(it) }
            callback(false)
        }
    }

    /**
     * 通过 Shizuku 执行 shell 命令，返回 stdout + stderr。
     * 等价于 `adb shell <command>`。
     *
     * 实现: 通过 IShizukuService.newProcess() 以 shell (UID=2000) 权限启动进程。
     *       先启动读取线程，再 waitFor，避免死锁。
     */
    fun execShell(command: String): ShellResult {
        return try {
            val service = getService() ?: return ShellResult("", "Shizuku binder 不可用（请确认 Shizuku 正在运行并已授权）", -1)
            val process = service.newProcess(arrayOf("sh", "-c", command), null, null)

            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            val stdoutThread = Thread {
                try {
                    ParcelFileDescriptor.AutoCloseInputStream(process.inputStream)
                        .bufferedReader().use { r ->
                            var line: String?
                            while (r.readLine().also { line = it } != null) {
                                stdoutBuilder.appendLine(line)
                            }
                        }
                } catch (_: Exception) {}
            }

            val stderrThread = Thread {
                try {
                    ParcelFileDescriptor.AutoCloseInputStream(process.errorStream)
                        .bufferedReader().use { r ->
                            var line: String?
                            while (r.readLine().also { line = it } != null) {
                                stderrBuilder.appendLine(line)
                            }
                        }
                } catch (_: Exception) {}
            }

            stdoutThread.start()
            stderrThread.start()

            val exitCode = process.waitFor()
            stdoutThread.join(10000)
            stderrThread.join(10000)
            process.destroy()

            ShellResult(stdoutBuilder.toString().trim(), stderrBuilder.toString().trim(), exitCode)
        } catch (e: Exception) {
            ShellResult("", e.message ?: "Shizuku exec failed", -1)
        }
    }

    /**
     * 执行 shell 命令并实时输出每一行（用于日志显示）
     */
    fun execShellStreaming(command: String, onLine: (String) -> Unit): ShellResult {
        return try {
            val service = getService() ?: return ShellResult("", "Shizuku binder 不可用（请确认 Shizuku 正在运行并已授权）", -1)
            val process = service.newProcess(arrayOf("sh", "-c", command), null, null)
            val stdout = StringBuilder()
            val stderr = StringBuilder()

            val stdoutReader = Thread {
                ParcelFileDescriptor.AutoCloseInputStream(process.inputStream).bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stdout.append(line).append("\n")
                        onLine(line!!)
                    }
                }
            }

            val stderrReader = Thread {
                ParcelFileDescriptor.AutoCloseInputStream(process.errorStream).bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stderr.append(line).append("\n")
                        onLine(line!!)
                    }
                }
            }

            stdoutReader.start()
            stderrReader.start()
            val exitCode = process.waitFor()
            stdoutReader.join(15000)
            stderrReader.join(15000)
            process.destroy()

            ShellResult(stdout.toString().trim(), stderr.toString().trim(), exitCode)
        } catch (e: Exception) {
            ShellResult("", e.message ?: "Shizuku exec failed", -1)
        }
    }

    /**
     * 将本地文件推送到设备上的指定路径（适合小文件）。
     * 通过 Shizuku newProcess 启动 cat，将原始字节写入进程 stdin。
     * 避免 base64 编码的 shell 参数长度限制问题。
     */
    /**
     * 转义 Shell 参数中的特殊字符，防止 Shell 命令注入或路径中的单引号引发语法错误。
     */
    fun escapeShellArg(arg: String): String {
        return "'" + arg.replace("'", "'\\''") + "'"
    }

    /**
     * 将本地文件推送到设备上的指定路径（适合小文件）。
     * 通过 Shizuku newProcess 启动 cat，将原始字节写入进程 stdin。
     * 避免 base64 编码的 shell 参数长度限制问题。
     */
    fun pushFile(localFile: java.io.File, remotePath: String): Boolean {
        return try {
            val data = localFile.readBytes()
            pushRaw(data, remotePath)
        } catch (e: Exception) {
            Log.e("ShizukuManager", "pushFile failed: ${e.message}")
            false
        }
    }

    /**
     * 推送大文件（真正流式传输，避免内存占用过高导致 OOM）。
     * 使用 FileInputStream 分块读取，每个 chunk 通过独立的 cat >> 进程写入。
     */
    fun pushFileChunked(localFile: java.io.File, remotePath: String, onProgress: (Int) -> Unit = {}): Boolean {
        if (!localFile.exists()) return false
        val total = localFile.length()
        if (total == 0L) return true
        val chunkSize = 2 * 1024 * 1024 // 2MB per chunk (stdin direct, no base64 overhead)
        val escapedPath = escapeShellArg(remotePath)
        val buffer = ByteArray(chunkSize)

        return try {
            // 清空目标文件
            execShell("rm -f $escapedPath")

            var bytesTransferred = 0L
            var isFirst = true

            java.io.FileInputStream(localFile).use { fis ->
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    val chunk = if (bytesRead == chunkSize) buffer else buffer.copyOfRange(0, bytesRead)
                    val redirect = if (isFirst) ">" else ">>"
                    if (!pushRawWithRedirect(chunk, remotePath, redirect)) {
                        Log.e("ShizukuManager", "push chunk failed at offset $bytesTransferred/$total")
                        return false
                    }
                    isFirst = false
                    bytesTransferred += bytesRead
                    if (total > 0) {
                        onProgress(((bytesTransferred * 100) / total).toInt())
                    }
                }
            }

            true
        } catch (e: Exception) {
            Log.e("ShizukuManager", "pushFileChunked failed: ${e.message}")
            false
        }
    }

    /**
     * 将原始字节通过 cat 写入远程文件（覆盖模式）。
     * 启动 shell 进程 `cat > path`，将 bytes 写入 stdin，等待进程退出。
     */
    private fun pushRaw(data: ByteArray, remotePath: String): Boolean {
        return pushRawWithRedirect(data, remotePath, ">")
    }

    /**
     * 将原始字节通过 cat 写入远程文件，指定重定向操作符。
     *
     * 核心思路：使用 Shizuku newProcess 启动 `cat redirect path`，
     * 将二进制数据直接写入进程的 stdin (outputStream)，
     * 完全避免 shell 参数长度限制和 base64 编码开销。
     */
    private fun pushRawWithRedirect(data: ByteArray, remotePath: String, redirect: String): Boolean {
        return try {
            val service = getService() ?: return false
            val escapedPath = escapeShellArg(remotePath)
            // cat 从 stdin 读取，重定向到目标文件
            val process = service.newProcess(
                arrayOf("sh", "-c", "cat $redirect $escapedPath"),
                null, null
            )

            // 写入二进制数据到 cat 的 stdin
            val stdin: OutputStream = ParcelFileDescriptor.AutoCloseOutputStream(process.outputStream)
            stdin.write(data)
            stdin.flush()
            stdin.close()

            // 等待 cat 完成
            val exitCode = process.waitFor()

            // 消费 stdout/stderr 防止管道阻塞
            try {
                ParcelFileDescriptor.AutoCloseInputStream(process.inputStream).use { it.readBytes() }
                ParcelFileDescriptor.AutoCloseInputStream(process.errorStream).use { it.readBytes() }
            } catch (_: Exception) {}

            process.destroy()
            exitCode == 0
        } catch (e: Exception) {
            Log.e("ShizukuManager", "pushRawWithRedirect failed: ${e.message}")
            false
        }
    }

    /**
     * 获取 Shizuku 的启动 Intent
     */
    fun getShizukuIntent(): Intent? {
        return context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
    }

    /** 注册唯一的权限结果监听（重复调用会先移除旧的，避免泄漏/重复触发） */
    fun setPermissionResultListener(listener: (Int, Int) -> Unit) {
        permListener?.let { Shizuku.removeRequestPermissionResultListener(it) }
        permListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            listener(requestCode, grantResult)
        }
        Shizuku.addRequestPermissionResultListener(permListener!!)
    }

    private var permListener: Shizuku.OnRequestPermissionResultListener? = null

    /** 移除已注册的权限监听 */
    fun clearPermissionResultListener() {
        permListener?.let { Shizuku.removeRequestPermissionResultListener(it) }
        permListener = null
    }
}

data class ShellResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int
) {
    val isSuccess: Boolean get() = exitCode == 0
    val output: String get() = when {
        stdout.isNotEmpty() && stderr.isNotEmpty() -> "$stdout\n$stderr"
        stdout.isNotEmpty() -> stdout
        else -> stderr
    }
}

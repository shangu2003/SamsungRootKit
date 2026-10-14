package com.samsung.rootkit.dfroot

import android.util.Log
import com.samsung.rootkit.data.PayloadManager
import com.samsung.rootkit.exploit.RootExecutor
import com.samsung.rootkit.shizuku.ShizukuManager
import java.util.concurrent.TimeUnit

/**
 * 提权成功之后清理设备端 `/data/local/tmp` 下**本工具产物**（round34 需求②③）。
 *
 * <h2>需求原文</h2>
 * <ol>
 *   <li>「DF链路提权成功也需要清理tmp下缓存文件；如果打开自动软重启，则下次打开 srk 时
 *       自动清理 tmp 下缓存文件（自动软重启后肯定已经是 root 状态）」；</li>
 *   <li>「DFE链路提权成功也需要清理 tmp 下缓存文件」。</li>
 * </ol>
 *
 * <h2>为什么需要「链内 + 应用侧」两半（缺一不可）</h2>
 * 清理要能删掉 `/data/local/tmp` 里的文件，前提是**有权限**。本机（SM-S9180 / 官方 KSU
 * 32601 / late-load 模式）round31 实测：无管理器时 `su` **结构性不可用**
 * （late-load 不注册 sucompat kprobe、本应用不在 allowlist、`/data/adb` 对普通身份 0700
 * 不可达），且未装 Shizuku ⇒ 应用侧两条通道都可能够不着。所以：
 * <ul>
 *   <li><b>链内那一半</b>（[com.samsung.rootkit.dfroot] 无对应类，实现在
 *       `assets/dfroot/ksud_wrap.sh` 与 `ksud_next_wrap.sh` 的「1a 段」）：
 *       跑在 UMH ⇒ **uid 0 + 整机 permissive**（dirtyfrag.c 置 selinux_state=false，
 *       直到 ksud 收尾才恢复）⇒ 零外部依赖、必然可执行。这是**主力**；</li>
 *   <li><b>应用侧这一半</b>（本类）：链返回后（进程还活着）立刻清一次；若链把进程
 *       软重启掉了，则落一枚持久化标记（[DfRootPrefs.markTmpCleanArmed]），
 *       由**下次打开 SRK** 接力 —— 正是用户描述的时序。这一半是**兜底**：
 *       它能覆盖链内那一半之后新产生的文件，也是链内那半万一没跑成时的第二次机会。</li>
 * </ul>
 *
 * <h2>通道选择：su → Shizuku（与既有实现完全一致，不另造轮子）</h2>
 * 与 [com.samsung.rootkit.exploit.RootExecutor] 的 `cleanDeviceTmpArtifacts()` 同序：
 * <ol>
 *   <li><b>su</b>（首选）：本应用已在 KSU allowlist 内时可用，root 身份删得最干净
 *       （`reboot_guard*` / `temp_su.sock` 这类 KSU 自有文件在 enforcing 下 shell 无
 *       unlink 权限）；</li>
 *   <li><b>Shizuku</b>（兜底）：shell(uid 2000) —— 而 `/data/local/tmp` 的**属主就是
 *       shell**（`drwxrwx--x shell shell`），删文件看的是目录写权限，故 shell 能删掉
 *       目录里任意属主的文件（KSU 自有标签的文件除外）。</li>
 * </ol>
 *
 * <p><b>成败判据是事实、不是命令退出码</b>：清理后用同一条通道跑
 * [PayloadManager.buildTmpArtifactCountCommand] 数一遍残留（只数本工具产物，
 * 不数他人文件），`remaining == 0` 才算成功。两条通道都不可用时 `remaining = -1`
 * （未知）并**如实上报**，不假装成功 —— 这正是本工程一贯的取舍
 * （原 DfManagerProvisioner 用的是同一条纪律，该类已在 round35 删除）。
 *
 * <h2>清理范围</h2>
 * 严格等于 [PayloadManager.TMP_ARTIFACTS]（**单一事实源**，链内 1a 段与它逐项一致，
 * 由 `_audit/round34/verify_round34.py` 断言）。两份刻意**不在**名单里的文件
 * （`srk-ksud-probe` 长期缓存、`dfroot_supported` 证据记录）见该常量的 KDoc。
 *
 * <p>本类是 **object + 阻塞方法**：调用方必须自备后台线程（IO）。
 */
object DfPostRootCleaner {

    private const val TAG = "DFROOT"

    /** 单条清理/统计命令的预算（`find` 扫一层目录，正常毫秒级；给足闪存抖动余量）。 */
    private const val CMD_TIMEOUT_MS = 20_000L

    /**
     * 一次清理的结果。
     *
     * @param ok        事实判据：清理后**本工具产物残留数为 0**（未知也算 false）。
     * @param remaining 清理后 /data/local/tmp 下本工具产物的剩余数量；-1 = 无法统计。
     * @param lines     逐步输出（调用方拼进 df 日志 / 运行记录）。
     */
    data class Result(
        val ok: Boolean,
        val remaining: Int,
        val lines: List<String>,
    )

    /**
     * 立刻清一次（**阻塞**；调用方自备 IO 线程）。
     *
     * @param shizuku 可空：拿不到实例的启动路径（如 [com.samsung.rootkit.RootKitApp]
     *                的极早期）只走 su 通道。
     */
    fun cleanNow(shizuku: ShizukuManager?): Result {
        val lines = mutableListOf<String>()
        val cleanCmd = PayloadManager.buildTmpArtifactCleanCommand()
        // 注意：统计命令不在这里建 —— 它只在 countRemaining() 里用一次，
        // 在此处再声明一个就成了死代码（Kotlin 会报「never used」）。

        // ── 通道①：su ──
        val su = try {
            RootExecutor.probeWorkingSu()
        } catch (t: Throwable) {
            Log.w(TAG, "probeWorkingSu failed", t)
            null
        }
        var suOut: String? = null
        if (su != null) {
            lines += "清理通道① su（$su）"
            val r = execSu(su, cleanCmd)
            suOut = when {
                r == null -> {
                    lines += "su 通道超时/异常"
                    null
                }
                else -> {
                    lines += "su 通道已执行（exit=${r.first}）"
                    r.second
                }
            }
        } else {
            lines += "无可用 su（未授权 / su 不在 allowlist），跳过通道①"
        }

        // ── 通道②：Shizuku（shell uid 2000）──
        // 把「可用性探针」的结果一次性收进本地 val：**可用 ⇒ 非空实例本身，否则 null**。
        // 为什么不写成 `val ok = shizuku != null && …` 再 `if (ok && shizuku != null)`：
        // 那样 Kotlin 会报 "Condition is always 'true'"（该布尔本地量本就蕴含非空），
        // 而若把后半句删掉又只能靠「布尔蕴含」型智能转换 —— 不如直接让**类型**承载
        // 非空性：`if (shizukuUsable != null)` 是经典的判空智能转换，零歧义、零告警。
        val shizukuUsable: ShizukuManager? = shizuku?.let { sz ->
            try {
                if (sz.isShizukuAvailable() && sz.hasPermission()) sz else null
            } catch (t: Throwable) {
                Log.w(TAG, "shizuku probe failed", t)
                null
            }
        }
        if (shizukuUsable != null) {
            lines += "清理通道② Shizuku（shell uid 2000，/data/local/tmp 属主即 shell）"
            try {
                val res = shizukuUsable.execShell(cleanCmd)
                lines += "Shizuku 通道已执行：${res.output.trim().take(200)}"
            } catch (t: Throwable) {
                lines += "Shizuku 通道异常：${t.javaClass.simpleName}: ${t.message}"
            }
        } else {
            lines += "Shizuku 不可用/未授权，跳过通道②"
        }

        // ── 事实核对（同一批通道，数残留）──
        val remaining = countRemaining(su, shizukuUsable)
        lines += when {
            remaining == 0 -> "已清理 /data/local/tmp 下本工具产物（残留 0）"
            remaining > 0 -> "仍未清干净：/data/local/tmp 下还有 $remaining 个本工具产物"
            else -> "无法核对残留数量（两条通道都不可用）——本次未能确认清理完成"
        }
        if (suOut.isNullOrBlank() && remaining != 0) {
            // 只在没拿到 su 输出时才补一句，避免日志噪音
            lines += "提示：本应用取得 KSU 授权后（在管理器的「超级用户」里放行），即可随时清干净。"
        }
        return Result(ok = remaining == 0, remaining = remaining, lines = lines)
    }

    // ─────────────────────────── 内部 ───────────────────────────

    /**
     * 统一用「同一批通道」数残留；两条都不可用返回 -1（未知）。
     *
     * @param su            可用的 su 路径（null = 无 su 通道）
     * @param shizukuUsable 可用的 Shizuku 实例（null = 无 Shizuku 通道）——
     *                      类型本身即承载了「可用性」，调用方无需再传一个 Boolean。
     */
    private fun countRemaining(su: String?, shizukuUsable: ShizukuManager?): Int {
        val cmd = PayloadManager.buildTmpArtifactCountCommand()
        if (su != null) {
            // su 优先；但若 su 通道超时/异常（execSu 返回 null），仍继续尝试 Shizuku，
            // 免得把一个本可得出的事实结论降级成「未知」。
            execSu(su, cmd)?.let { r ->
                return r.second.trim().lineSequence().lastOrNull { it.isNotBlank() }
                    ?.trim()?.toIntOrNull() ?: -1
            }
        }
        if (shizukuUsable != null) {
            return try {
                shizukuUsable.execShell(cmd).output.trim()
                    .lineSequence().lastOrNull { it.isNotBlank() }?.trim()?.toIntOrNull() ?: -1
            } catch (_: Throwable) {
                -1
            }
        }
        return -1
    }

    /** 经 su 跑一条命令：返回 (exitCode, stdout+stderr)；超时/异常 → null。 */
    private fun execSu(su: String, cmd: String): Pair<Int, String>? = try {
        val p = Runtime.getRuntime().exec(arrayOf(su, "-c", cmd))
        if (!p.waitFor(CMD_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            p.destroyForcibly()
            null
        } else {
            p.exitValue() to (
                p.inputStream.bufferedReader().use { it.readText() } +
                    p.errorStream.bufferedReader().use { it.readText() }
                )
        }
    } catch (t: Throwable) {
        Log.w(TAG, "execSu failed: $cmd", t)
        null
    }
}

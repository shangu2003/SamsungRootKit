package com.samsung.rootkit.df

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 一条 su 命令的执行结果。 */
data class DfCmdResult(
    val ok: Boolean,
    val output: String,
    val exitCode: Int,
    /**
     * 是否属于"没有可用的 su"（而不是"su 执行了但失败"）。
     *
     * 触发场景（2026-09-25 真机实测）：KernelSU 的 su 常位于 `/data/adb/ksu/bin/su`，
     * 该目录**不在** app 进程 PATH 里 ⇒ `Runtime.exec("su")` 抛
     * `IOException: Cannot run program "su": error=2, No such file or directory`。
     * 另一种是 su 存在但本应用尚未在管理器里被授予 root。
     * UI 需据此弹"去管理器授权"的引导，而不是只报一句失败。
     */
    val suMissing: Boolean = false,
) {
    /** 首个非空行，供 UI 一行摘要使用。 */
    val summary: String
        get() = output.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: "(无输出)"
}

/** 一步操作的结果（用于分步展示进度）。 */
data class DfStep(val name: String, val cmd: DfCmdResult) {
    val ok: Boolean get() = cmd.ok
}

/**
 * 卸载结果报告（2026-10-05 新增）。
 *
 * <h2>为什么不再让调用方用「每一步都 ok」当完成判据</h2>
 * 原实现在 UI 侧写的是 `steps.isNotEmpty() && steps.all { it.ok }`，它有两个
 * **已证伪的假阴性来源**：
 * <ol>
 *   <li><b>重试语义</b>：移密钥最多 3 轮，**被后续轮次成功覆盖的中间轮失败**会永久留在
 *       [steps] 里（`[X] 复核密钥已移除：密钥仍存在（疑似被 PMS 写回），准备重试`）——
 *       于是「第 2 轮修好了」照样被判成「没完成」；</li>
 *   <li><b>best-effort 步骤</b>：末尾的 `清理开机自启动残留` 在自己的 KDoc 里就写明
 *       「文件不存在也视为成功、**不影响卸载主流程的结论**」，可它的 `ok` 却照样把 UI
 *       卡在确认态 —— 与文档承诺自相矛盾。</li>
 * </ol>
 * 二者叠加的现场表现（真机）：**弹窗已经打印出「已卸载」的步骤输出、最下面那颗按钮却
 * 仍是「卸载」，且永远进不到完成态**（用户 2026-10-05 反馈；当时设备实测 DFReroot 已不在、
 * `df-boot-prefs` 也已被清掉 ⇒ 卸载其实已经完成）。
 *
 * <p>故本报告只回答**事实**，不回答「过程是否完美」：APK 走了没、密钥走了没。
 * 两者都是权威通道的实测结论（PackageManager / `DfInject --check`），
 * 与「哪一轮失败过」「best-effort 步骤成没成」彻底解耦。
 */
data class DfUninstallReport(
    val steps: List<DfStep>,
    /** DFReroot 是否**已不在设备上**（[DfBridge.isDfInstalled] 实测，零 su、零特权）。 */
    val dfGone: Boolean,
    /** 注入密钥是否**已从 packages.xml 移除**（`DfInject --check` 实测 `all_injected=false`）。 */
    val keyGone: Boolean,
) {
    /**
     * 卸载是否**真的完成** —— UI「完成态」（取消 / 软重启对称按钮）的唯一判据，
     * 同时也是运行记录结局与「清 pendingInstall 标记」的判据。
     */
    val done: Boolean get() = dfGone && keyGone

    /** 未完成时的**可读归因**（写运行记录 / 排障用）；已完成返回空串。 */
    val failureDetail: String
        get() = when {
            done -> ""
            !keyGone -> "注入密钥仍留在 packages.xml（移除未生效）"
            !dfGone -> "DFReroot 仍在设备上（卸载未生效）"
            // 理论不可达（两项事实都不成立时必然至少有一个失败步骤），留作兜底。
            else -> steps.firstOrNull { !it.ok }?.let { "${it.name}：${it.cmd.summary}" }.orEmpty()
        }
}

/**
 * DFReroot 集成的执行层 —— 所有 root 动作都在这里，UI 只负责调用与展示。
 *
 * 为什么必须走 `su`：
 *  本应用自身是 `untrusted_app`，既改不了 `/data/system/packages.xml`（SELinux 拦），
 *  也没有 `CAP_NET_ADMIN`。注入动作必须由 root 起一个 `app_process` 进程完成
 *  （`Abx.kt` 注释已写明：反射调 `android.util.Xml.resolvePullParser` 必须跑在 app_process，
 *   非 zygote 沙箱，否则撞 hidden-API 限制）。
 *
 * su 调用写法来源：本工程既有实现 `ui/RootKitScreen.kt:3363-3377` 的 `tryRootDelete()`。
 */
object DfRunner {

    // ─────────────────────────── 底层：跑 su ───────────────────────────

    /** 默认 su 命令名（走 PATH）。KSU 的 su 常在 `/data/adb/ksu/bin/su`，需调用方先探测。 */
    const val DEFAULT_SU = "su"

    /**
     * 执行 `<su> -c <command>`，带 stdout/stderr 并发读取（避免管道缓冲写满导致死锁）。
     *
     * @param suPath 可用的 su 路径。调用方应先用 `RootExecutor.resolveWorkingSu()` 探测，
     *               否则在"KSU 的 su 不在 PATH"时会立刻失败并抛 ENOENT（见 [DfCmdResult.suMissing]）。
     */
    fun runSu(command: String, suPath: String = DEFAULT_SU): DfCmdResult = try {
        val proc = Runtime.getRuntime().exec(arrayOf(suPath, "-c", command))
        val out = StringBuilder()
        val err = StringBuilder()
        val tOut = Thread { proc.inputStream.bufferedReader().forEachLine { out.appendLine(it) } }
        val tErr = Thread { proc.errorStream.bufferedReader().forEachLine { err.appendLine(it) } }
        tOut.start()
        tErr.start()
        val rc = proc.waitFor()
        tOut.join()
        tErr.join()
        val text = buildString {
            if (out.isNotBlank()) append(out.toString().trim())
            if (err.isNotBlank()) {
                if (isNotEmpty()) append('\n')
                append("[stderr] ").append(err.toString().trim())
            }
            if (isEmpty()) append("(无输出)")
        }
        DfCmdResult(rc == 0, text, rc)
    } catch (t: Throwable) {
        val msg = t.message.orEmpty()
        // 判据来自真机报错原文：Cannot run program "su": error=2, No such file or directory
        val missing = t is java.io.IOException && (
            msg.contains("No such file or directory") || msg.contains("error=2") ||
                msg.contains("Cannot run program") || msg.contains("Permission denied") ||
                msg.contains("error=13")
            )
        DfCmdResult(
            false,
            "su 执行失败（$suPath）：${t.javaClass.simpleName}: $msg",
            -1,
            suMissing = missing,
        )
    }

    suspend fun runSuAsync(command: String, suPath: String = DEFAULT_SU): DfCmdResult =
        withContext(Dispatchers.IO) { runSu(command, suPath) }

    // ─────────────────────────── 资产与安装 ───────────────────────────

    /**
     * 把内置的 df_reroot.apk 从 assets 解到 cache，返回文件。
     * 资产由构建脚本灌入（见工程根 `build.sh`），源码树里只有占位说明。
     */
    fun stageDfApk(ctx: Context): File {
        val dir = File(ctx.cacheDir, "dfreroot")
        if (!dir.exists() && !dir.mkdirs()) {
            throw IllegalStateException("无法创建缓存目录：${dir.absolutePath}")
        }
        val out = File(dir, "df_reroot.apk")
        ctx.assets.open(DfBridge.DF_APK_ASSET).use { input ->
            out.outputStream().use { input.copyTo(it) }
        }
        return out
    }

    /** 本应用 APK 的绝对路径（app_process 的 CLASSPATH）。 */
    fun ownSourceDir(ctx: Context): String = ctx.applicationInfo.sourceDir

    // ─────────────────────────── 首次配置 3 步 ───────────────────────────

    /**
     * 步骤 1：注入证书。用 root 起 app_process 跑 [DfInject]，改 `/data/system/packages.xml`。
     *
     * 判据：输出含 `injected=true`（DfInject 的 `--check` 分支输出格式，见 `DfInject.kt:86`）。
     *
     * @param suPath `RootExecutor.resolveWorkingSu()` 的结果。传 null 会退回 `"su"`，
     *               在 KSU 的 su 不在 PATH 时得到 [DfCmdResult.suMissing] = true。
     */
    suspend fun injectCert(ctx: Context, suPath: String? = null): DfCmdResult =
        withContext(Dispatchers.IO) {
            val keyHex = try {
                DfBridge.ownCertHex(ctx)
            } catch (t: Throwable) {
                return@withContext DfCmdResult(false, "读本应用证书失败：${t.message}", -1)
            }
            val cmd = DfBridge.buildAppProcessCommand(ownSourceDir(ctx), keyHex)
            runSu(cmd, suPath ?: DEFAULT_SU)
        }

    /** 步骤 2：软重启（唯一能让 PMS 重读 packages.xml 的手段）。 */
    suspend fun softReboot(suPath: String? = null): DfCmdResult =
        runSuAsync(DfBridge.buildSoftRebootCommand(), suPath ?: DEFAULT_SU)

    /**
     * 软重启（**带候选兜底**，2026-10-08 新增）。
     *
     * <h2>为什么需要它</h2>
     * 旧调用形态是「调用方先 `resolveWorkingSu()` 探一个，再把这一个交给 [softReboot]」。
     * 只要那一次探测落空（返回 null），就会退化成裸 `"su"`，进而在 su 不在 PATH 的设备上
     * 直接抛 ENOENT ⇒ `suMissing=true` ⇒ UI 把用户甩到「去管理器授权」弹窗，**即便设备此时
     * 明明还有可用的 su**（探测与真正执行之间环境可能已变，或在多候选路径里选错）。
     *
     * <p>本函数把「选路」与「执行」合成一步：先试 [preferred]（通常是刚被验证过的那条），
     * 失败且属「找不到 su」时再逐个试 [SU_CANDIDATES]；只要有一条能真正把命令跑起来
     * （`suMissing=false`）就立即采纳其结果，不再换路径。
     *
     * <p>候选表与 RootExecutor.SU_CANDIDATES **同源但有意各留一份**：本包
     * （`com.samsung.rootkit.df`）按 [DfBridge] 的定位应保持「纯常量与纯函数、不反向依赖
     * 上层（exploit/data/ui）」，故不 import RootExecutor。
     *
     * @return 任一路径成功即返回其成功结果；全部失败则返回**最后一次**结果（`suMissing`
     *         反映「所有候选都没有可用 su」）。
     */
    suspend fun softRebootRobust(preferred: String? = null): DfCmdResult {
        val order = LinkedHashSet<String>()
        if (!preferred.isNullOrBlank()) order += preferred
        order += SU_CANDIDATES
        var last: DfCmdResult? = null
        for (p in order) {
            val r = runSuAsync(DfBridge.buildSoftRebootCommand(), p)
            if (r.ok) return r
            last = r
            // su 跑起来了、只是命令本身失败 ⇒ 换路径也没用，直接收工（避免反复弹授权框）
            if (!r.suMissing) return r
        }
        return last ?: DfCmdResult(false, "(无可用的 su 候选)", -1, suMissing = true)
    }

    /**
     * su 候选路径（与 [com.samsung.rootkit.exploit.RootExecutor] 的 SU_CANDIDATES 同序）。
     * 「先走 PATH」在前是因为 KSU su-compat 就绪时 `/system/bin/su` 可命中。
     */
    private val SU_CANDIDATES = listOf(
        "su",
        "/system/bin/su",
        "/system/xbin/su",
        "/debug_ramdisk/su",
        "/sbin/su",
        "/su/bin/su",
        "/data/adb/ksu/bin/su",
    )

    /**
     * 步骤 3：安装 DFReroot。
     *
     * ⚠️ 必须在软重启**之后**执行 —— PMS 只在启动时读 packages.xml，
     *    注入后未重启就装，会拿到 `INSTALL_FAILED_SHARED_USER_INCOMPATIBLE`
     *    （上游 `InjectMain.kt:107-109` 原注释已写明）。
     */
    suspend fun installDf(ctx: Context, suPath: String? = null): List<DfStep> =
        withContext(Dispatchers.IO) {
            val su = suPath ?: DEFAULT_SU
            val steps = mutableListOf<DfStep>()
            val apk = try {
                stageDfApk(ctx)
            } catch (t: Throwable) {
                return@withContext listOf(
                    DfStep("解出内置 APK", DfCmdResult(false, "assets 缺失：${t.message}", -1)),
                )
            }
            val push = runSu(
                "cp '${apk.absolutePath}' ${DfBridge.DEV_DF_APK} && chmod 644 ${DfBridge.DEV_DF_APK}",
                su,
            )
            steps += DfStep("推送到 ${DfBridge.DEV_DF_APK}", push)
            if (!push.ok) return@withContext steps

            val install = runSu("pm install -r --user 0 ${DfBridge.DEV_DF_APK}", su)
            steps += DfStep("pm install", install)
            // 兜底清理中转文件，不给 /data/local/tmp 留垃圾
            runSu("rm -f ${DfBridge.DEV_DF_APK}", su)
            steps
        }

    // ─────────────────────────── 卸载（设置页） ───────────────────────────

    /**
     * 轮询 packages.xml 的 mtime，直到**连续两次采样不变**（判定 PMS 的异步
     * writeSettings 已落盘）。轮询间隔 1.2s，总超时 [timeoutMs]。
     *
     * 为什么必须等（v5.1.2 真机踩坑）：`pm uninstall` 会让 PMS 在后台线程调度一次
     * writeSettings——它把**内存里的完整状态**（仍带着我们注入的 pastSigs，因为 PMS
     * 只在下次开机才重新读盘）整文件重写回 packages.xml。不等它写完就删密钥，
     * 删完的文件会被这次写覆盖 → 密钥「复活」，再注入时报 already injected。
     *
     * @return true=已判稳；false=stat 不可用或超时（调用方应继续走复核+重试兜底）。
     */
    private fun waitForPmsSettled(su: String, timeoutMs: Long = 12000): Boolean {
        fun mtime(): Long? =
            runSu("stat -c %Y ${PackagesXml.PACKAGES_XML}", su).output.trim().toLongOrNull()
        var prev = mtime() ?: return false
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(1200)
            val cur = mtime() ?: return false
            if (cur == prev) return true
            prev = cur
        }
        return false
    }

    /**
     * 卸载 DFReroot 持久化。**顺序经确认：先卸 APK，再移密钥。**
     *
     * 顺序理由（失败模式不对称）：
     *  - 先移密钥后卸 APK：若第 2 步失败，会留下「APK 还在、密钥没了」的状态 —— 该包声明
     *    `sharedUserId="android.uid.system"` 但签名已不在 shared-user 签名集里，
     *    下次开机 PMS 扫描行为未验证（`[DEVICE-VERIFY]`），风险可能外溢到 system uid 本身。
     *  - 先卸 APK 后移密钥：若第 2 步失败，只留一条多余的 `<pastSigs>` 记录 —— 纯冗余、无害，
     *    顶多下次重装 DFReroot 无需再注入。
     *
     * v5.1.2 修复：卸载后密钥"复活"（真机实测：APK 卸了、packages.xml 里密钥还在）。
     * 根因是 PMS 异步 writeSettings 竞态（见 [waitForPmsSettled] 注释）。对策：
     *  ① pm uninstall 后先等 PMS 把这次卸载落盘（mtime 判稳），再动密钥；
     *  ② 移除后跑 `--check` 复核；若密钥又被写回（PMS 竞态），等它再次落盘后重试，
     *     最多 3 轮。
     *
     * @return 分步结果；调用方应在最后一步之后提示用户重启（密钥移除只在 PMS 下次读
     *         packages.xml 时完全生效）。
     */
    suspend fun uninstallDfFully(ctx: Context, suPath: String? = null): DfUninstallReport =
        withContext(Dispatchers.IO) {
            val su = suPath ?: DEFAULT_SU
            val steps = mutableListOf<DfStep>()
            // 「组装报告」的唯一出口（局部函数须先声明后引用，故放在最前）。
            // dfGone 每次现场重查：pm uninstall 之后 PMS 的内存态已更新，
            // 这一问拿到的是**当下事实**，不是对前面步骤记录的复述。
            fun report(keyGone: Boolean) =
                DfUninstallReport(steps = steps, dfGone = !DfBridge.isDfInstalled(ctx), keyGone = keyGone)

            // ① 先卸 APK（packages.xml 里的 <package> 条目随之消失）
            if (DfBridge.isDfInstalled(ctx)) {
                val r = runSu("pm uninstall --user 0 ${DfBridge.PKG_DF}", su)
                steps += DfStep("卸载 DFReroot APK", r)
                if (!r.ok) {
                    // 失败就停：绝不在「APK 还在」的情况下继续移密钥
                    return@withContext report(keyGone = false)
                }
                // ①b 等 PMS 把「卸载」落盘（见 waitForPmsSettled 注释）。
                // 判稳与否都不中止：真删除以 ②③ 的复核为准。
                val settled = waitForPmsSettled(su)
                steps += DfStep(
                    "等待 PMS 落盘",
                    DfCmdResult(true, if (settled) "packages.xml 已稳定" else "判稳超时，继续（由复核兜底）", 0),
                )
            } else {
                steps += DfStep("卸载 DFReroot APK", DfCmdResult(true, "未安装，跳过", 0))
            }

            // ②③ 移密钥 → 复核 → （被 PMS 写回则）重试，最多 3 轮
            val keyHex = try {
                DfBridge.ownCertHex(ctx)
            } catch (t: Throwable) {
                steps += DfStep("移除注入密钥", DfCmdResult(false, "读证书失败：${t.message}", -1))
                return@withContext report(keyGone = false)
            }
            var removed = false
            for (attempt in 1..3) {
                val cmd = DfBridge.buildAppProcessCommand(
                    ownSourceDir(ctx), keyHex, mode = "--uninstall",
                )
                val r = runSu(cmd, su)
                steps += DfStep("移除注入密钥（第 $attempt 次）", r)
                if (!r.ok) break
                // 复核：--check（零写入）确认真不在了——PMS 竞态可能在删除后立刻写回
                val c = checkInjected(ctx, su)
                val clean = c.ok && c.output.contains("all_injected=false")
                steps += DfStep(
                    "复核密钥已移除",
                    if (clean) c else DfCmdResult(false, "密钥仍存在（疑似被 PMS 写回），准备重试", c.exitCode),
                )
                if (clean) {
                    removed = true
                    break
                }
                // 等 PMS 把写回动作落盘再删，否则下一刀还会被覆盖
                waitForPmsSettled(su, timeoutMs = 8000)
            }
            if (!removed && steps.last().ok) {
                // 理论不可达（循环里复核失败必然置 ok=false），防御性兜底
                steps += DfStep("复核密钥已移除", DfCmdResult(false, "未确认移除，请重试或重启后再试", -1))
            }

            // ③' 清理「开机自启动」的持久化残留（2026-10-01 修复）。
            // 背景：DFReroot 以 uid=system / root 把开关状态写进 /data/system 与
            // /data/adb，这些**不属于 APK 数据目录**，`pm uninstall` 不会清。若不清，
            // 即便 APK 已卸、密钥已移，残留的 service.d shim 仍会在下次开机被 KernelSU
            // 的 ksud 自动执行 → `late-load` 重新加载 LKM，实现「开机自动 root」。
            steps += uninstallAutostartResidue(su)

            android.util.Log.d(
                "DFUNINSTALL",
                "uninstallDfFully done: " + steps.joinToString(" | ") {
                    "${it.name} ok=${it.ok} rc=${it.cmd.exitCode} :: ${it.cmd.summary}"
                },
            )
            // ⚠️ 这里必须是 report(...) 而不是裸 steps —— 本函数已改为返回 [DfUninstallReport]。
            //    keyGone 取 `removed`（= ②③ 循环里「--check 实测 all_injected=false」那一轮才置真），
            //    **不是** steps.all{ok}：中间轮失败被后续轮覆盖后仍然留在 steps 里，
            //    拿它当判据正是那个「已卸载却进不到完成态」bug 的根因（见 DfUninstallReport KDoc）。
            report(keyGone = removed)
        }

    /**
     * 清理 DFReroot「开机自启动」相关的持久化残留文件。
     *
     * 这些文件是 DFReroot 运行时**直接写盘**的（`android.uid.system` / root），落在
     * `/data/system` 与 `/data/adb` 下，**不在 APK 数据目录内**，故 `pm uninstall`
     * 不会触及。必须显式删除，否则开机自启可能在「APK 已卸载」的情况下仍被触发：
     *
     *   · `/data/system/df-boot-prefs`   —— autostart/d2fix/variant 开关（自启核心状态）
     *   · `/data/system/df-boot-disabled`—— 自启哨兵（无条件关闭标记）
     *   · `/data/system/df-boot-attempts`—— 尝试计数
     *   · `/data/system/df-boot-ok`      —— 「本次开机已挂钩」标记
     *   · `/data/adb/service.d/zz-df-r1.sh` —— service.d shim（KernelSU 开机自动执行，
     *        会 `logcat late-load` 重载 LKM；这是「不依赖 DFReroot APK 也能开机 root」的关键）
     *   · `/data/system/dfreroot-ksud`   —— stage2 bind-mount 到 logcat 的执行体
     *   · `/data/local/tmp/.ksud-stage`  —— 三星 ksud 的 staging 中转
     *   · 日志（df-boot.log / df-boot-chain.log / df-sr-lastboot）—— 审计残留，一并清
     *
     * 清理是 best-effort（文件不存在也视为成功），不影响卸载主流程的结论。
     *
     * <h2>⚠️ 2026-10-08 修复：`/data/adb/ksud` 从清理名单**移除**（曾是「卸载后软重启失效」的根因）</h2>
     * 旧实现把 `/data/adb/ksud` 也当残留删掉，依据是「它是 ksud 从 `.ksud-stage` rename 来的暂存体」
     * （该 rename 确实存在，见 [com.samsung.rootkit.exploit.RootExecutor] 注释）。但**落点选错了**：
     * `/data/adb/ksud` 正是 KernelSU late-load 完成后**接管**的规范执行体路径
     * （见 `DFRoot_v4.20_…md` §「memfd 无文件 → `/data/local/tmp/.ksud-stage` → `/data/adb/ksud` 接管」），
     * KernelSU 的 su 入口 `/data/adb/ksu/bin/su` 即经它路由。
     * <p>后果（真机现象）：卸载流程里 `pm uninstall` 之后**紧接着**执行本清理 ⇒ su 入口被一并删掉 ⇒
     * 之后「完成态」弹窗里点**软重启**时 `resolveWorkingSu()` 已探不到 su ⇒ 误判
     * `suMissing` ⇒ 被甩到「去管理器授权」弹窗（用户 2026-10-08 报障：卸载完成后的软重启"调用不了，
     * 又返回到打开管理器"）。
     * <p>**差分证据**：同一段软重启代码在 ③「安装」流程（注入后软重启）可正常执行，唯独 ④「卸载完成态」
     * 失败 —— 两者唯一差别就是卸载额外跑了本清理 ⇒ 元凶在本函数内。而在本名单里，唯一与 su 相关
     * 的就是 `/data/adb/ksud`。
     * <p>**为什么删它也不必要**：自启链的拦断面是 `service.d/zz-df-r1.sh` + `/data/system/df-*` 状态位，
     * 与 `/data/adb/ksud` 无关；且该文件在下一次跑链时会由 ksud 自身的 late-load 从 `.ksud-stage`
     * 重新 rename 生成（自愈），留它既不恢复自启、也不留隐患。
     */
    private fun uninstallAutostartResidue(su: String): DfStep {
        val targets = listOf(
            "/data/system/df-boot-prefs",
            "/data/system/df-boot-disabled",
            "/data/system/df-boot-attempts",
            "/data/system/df-boot-ok",
            "/data/system/df-boot.log",
            "/data/system/df-boot-chain.log",
            "/data/system/df-sr-lastboot",
            "/data/system/dfreroot-ksud",
            "/data/adb/service.d/zz-df-r1.sh",
            // ⚠️ 2026-10-08：**刻意不删** `/data/adb/ksud`。
            //    它是 KernelSU late-load 后接管的执行体、su 入口 `/data/adb/ksu/bin/su` 经它路由；
            //    卸载时删掉会让随后的「完成态软重启」拿不到 su（详见本函数 KDoc）。
            //    它不是自启链的一环（拦断面是 service.d shim + /data/system/df-* 状态位），
            //    且跑链时会由 ksud 自身从 .ksud-stage 重新 rename 生成。
            "/data/local/tmp/.ksud-stage",
        )
        // 历史存量：v26 已删除的 dmesg 采样器落盘（df-dmesg-*.txt），通配一并清。
        val cmd = "rm -f " + targets.joinToString(" ") + " /data/system/df-dmesg-*.txt"
        val r = runSu(cmd, su)
        // 复核：核心开关文件确已消失（df-boot-prefs 是自启状态的唯一权威落点）。
        val check = runSu(
            "test ! -e /data/system/df-boot-prefs && echo gone || echo still-present", su,
        )
        val clean = check.ok && check.output.contains("gone")
        return DfStep(
            "清理开机自启动残留",
            if (clean) r else DfCmdResult(false, "df-boot-prefs 仍存在（清理未生效）：${r.summary}", r.exitCode),
        )
    }

    /** 只查状态，不写盘（DfInject 的 `--check`：逐目标报告 injected=true/false）。 */
    suspend fun checkInjected(ctx: Context, suPath: String? = null): DfCmdResult =
        withContext(Dispatchers.IO) {
            val keyHex = try {
                DfBridge.ownCertHex(ctx)
            } catch (t: Throwable) {
                return@withContext DfCmdResult(false, "读证书失败：${t.message}", -1)
            }
            runSu(
                DfBridge.buildAppProcessCommand(ownSourceDir(ctx), keyHex, mode = "--check"),
                suPath ?: DEFAULT_SU,
            )
        }
}

package com.samsung.rootkit.data

import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 一次运行的**来源**（跑链的是哪套引擎）。
 *
 * ⚠️ 简称纪律（与全工程一致）：
 * - [SRK] = 本工程自己的 Root 向导（`RootExecutor.executeRoot` + payload ksud）
 * - [DF]  = df（DFRoot / `df.root`，round24 起内嵌本包，`df.root.DfChain`）
 * - [DFE] = dfe（DFReroot / `com.polygraphene.df.reroot`，独立 App，`DfRunner` 系列操作）
 */
enum class RunSource { SRK, DF, DFE }

/**
 * 一次运行的**结局**。
 *
 * [RUNNING] 只存在于「进行中」的暂存记录（`run_log_pending.json`）里，
 * 不会出现在 [RunLogStore.loadAttempts] 的结果中 —— 列表里只会看到最终态三选一。
 */
enum class RunOutcome { RUNNING, SUCCESS, FAILED, ABORTED }

/**
 * 一条运行记录。
 *
 * <p>相对 v6.0.2 的形态有两处扩展：
 * <ol>
 *   <li>多了 [source]：运行记录页要同时收纳 SRK / df / dfe 三条链的日志，
 *       没有来源就无法区分；</li>
 *   <li>`success: Boolean` 换成 [outcome] 三态：原先只有「成功 / 失败」，
 *       而「跑到一半进程就没了（被杀 / 软重启 / 崩溃）」既不是成功也不是失败，
 *       却恰恰是排查最需要看到的一类；混进「失败」会掩盖真实原因。</li>
 * </ol>
 * [detail] 是短原因（失败原因 / 中断原因 / 成功摘要），列表页在标题下直接显示一行。
 */
data class RunAttempt(
    /** 唯一标识 = [begin] 时的开始时间戳；[finish] 靠它认领暂存记录。 */
    val id: Long,
    /** 开始时间（列表排序与显示都用它）。 */
    val timestamp: Long,
    val source: RunSource,
    val outcome: RunOutcome,
    val model: String,
    val firmware: String,
    val detail: String,
    val logLines: List<String>,
)

/**
 * 运行记录存储（SRK / df / dfe 共用一份）。
 *
 * <h2>为什么要有「暂存记录」（pending）</h2>
 * 「运行一半就退出」的记录**不可能在退出时写**——那时进程已经没了。可行的做法是反着来：
 * 开始时先落一条 [RunOutcome.RUNNING] 的暂存记录，结束时再改写成最终态。
 * 于是下次进程启动时若发现暂存记录还在，就说明上次运行没走完 ⇒ 回落为 [RunOutcome.ABORTED]。
 * 这样「半途退出」的日志也能出现在列表里，而不是静默消失。
 *
 * <p>暂存记录单独放一个文件（而不是在主列表里改），是为了让跑链途中的
 * [checkpoint] 只重写这一条 —— 主列表可能有 50 条 × 上百行日志，
 * 每隔一秒重写整个列表在长链上是纯粹的浪费。
 *
 * <h2>进程为谁而死，结论就不一样</h2>
 * df 链的软重启是**成功路径的一部分**：链跑完 → 发起软重启 → 本进程被杀。
 * 若恰好在写最终态之前被杀，下次起来就会看到一条「中断」，与事实相反。
 * 故 [recoverPending] 接受 `dfArmedThisBoot`：df 来源的记录在「本次开机已注入」时
 * 回落为成功（注入确实生效了，只是没来得及写回执）。
 */
object RunLogStore {
    private const val FILE_NAME = "run_logs.json"
    private const val PENDING_FILE_NAME = "run_log_pending.json"
    private const val MAX_ENTRIES = 50

    /** 跑链途中写盘的节流间隔：日志行可能刷得非常密，不节流会把 IO 打满。 */
    private const val CHECKPOINT_MIN_INTERVAL_MS = 1500L

    /** 上次 checkpoint 落盘时刻（进程内，不持久化）。 */
    private var lastCheckpointAt = 0L

    /**
     * 记录文件的目录：**设备保护存储（DE）**，不是普通的 CE `filesDir`。
     *
     * <p>为什么不放 CE：df 的**开机自启**（[com.samsung.rootkit.dfroot.DfBootReceiver]）在
     * `LOCKED_BOOT_COMPLETED` 时就会跑链 —— 那一刻用户还没解锁，CE 存储根本没挂载，
     * 往 `context.filesDir` 写会直接失败。而「无人值守的那次开机 root」恰恰是最需要留痕的一类运行。
     * 放到 DE 后，锁屏期与解锁后读写的都是同一个文件，运行记录页也能正常看到它。
     *
     * <p>与本工程既有教训同源（见 `DfRootFailureRecordStore.file`、`DfRootPrefs.de`）：
     * 凡是「开机早期就要读写」的状态，一律 DE。
     *
     * <p>代价：v6.0.3 之前写在 CE 里的历史记录升级后不再可见（日志属诊断数据，可接受）。
     */
    private fun file(context: Context) = File(deDir(context), FILE_NAME)

    private fun pendingFile(context: Context) = File(deDir(context), PENDING_FILE_NAME)

    /** DE 目录；取不到时回退 CE（极少数 ROM 上 DE 未启用，宁可丢开机期记录也不能崩）。 */
    private fun deDir(context: Context): File = try {
        context.createDeviceProtectedStorageContext().filesDir
    } catch (_: Throwable) {
        context.filesDir
    }

    /** 无 payload 上下文（df / dfe 路径）时，运行记录用的设备描述。 */
    fun defaultModel(): String = Build.MODEL?.takeIf { it.isNotEmpty() } ?: "—"

    /** 无 payload 上下文（df / dfe 路径）时，运行记录用的固件描述。 */
    fun defaultFirmware(): String = Build.DISPLAY?.takeIf { it.isNotEmpty() } ?: "—"

    /**
     * 开始一次运行：写暂存记录并返回它（调用方在结束时把同一个对象交给 [finish]）。
     *
     * <p>返回整个对象而不是 id，是为了让 [finish] **不依赖**暂存文件还完好 ——
     * 即便暂存被别的东西覆盖/清掉，结束时仍能凭手里的对象落一条完整记录。
     */
    @Synchronized
    fun begin(
        context: Context,
        source: RunSource,
        model: String,
        firmware: String,
        detail: String = "",
    ): RunAttempt {
        val now = System.currentTimeMillis()
        val attempt = RunAttempt(
            id = now,
            timestamp = now,
            source = source,
            outcome = RunOutcome.RUNNING,
            model = model,
            firmware = firmware,
            detail = detail,
            logLines = emptyList(),
        )
        writePending(context, attempt)
        return attempt
    }

    /**
     * 跑链途中的日志快照（可选）。带节流：距上次落盘不足 [CHECKPOINT_MIN_INTERVAL_MS]
     * 直接返回，调用方可以每来一行就调一次。
     *
     * @param force 结束前最后一次快照用 true，跳过节流。
     */
    @Synchronized
    fun checkpoint(context: Context, attempt: RunAttempt, logLines: List<String>, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastCheckpointAt < CHECKPOINT_MIN_INTERVAL_MS) return
        lastCheckpointAt = now
        val pending = readPending(context) ?: return
        if (pending.id != attempt.id) return
        writePending(context, pending.copy(logLines = logLines))
    }

    /** 结束一次运行：落最终态并清掉暂存记录。 */
    @Synchronized
    fun finish(
        context: Context,
        attempt: RunAttempt,
        outcome: RunOutcome,
        detail: String,
        logLines: List<String>,
    ) {
        val done = attempt.copy(
            // RUNNING 不是合法的最终态：调用方漏传时按「失败」处理，绝不写回一条假的「进行中」。
            outcome = if (outcome == RunOutcome.RUNNING) RunOutcome.FAILED else outcome,
            detail = detail,
            logLines = logLines,
        )
        append(context, done)
        val pending = readPending(context)
        if (pending != null && pending.id == attempt.id) deletePending(context)
    }

    /**
     * 进程启动时调用一次：把上次没走完的暂存记录落成「中断」。
     *
     * @param dfArmedThisBoot df 来源的记录专用判据 —— `/dev/df` 存在说明本次开机
     *        其实已经注入成功，只是回执没写成（软重启抢先杀进程），此时记「成功」才对。
     * @param extraLogLines  收尾时**追加**到日志末尾的行。
     *        典型来源（round35 起只剩这一个）：df 的「提权成功后清理 /data/local/tmp」
     *        在开着自动软重启时会落到**下次进程启动**执行，它的分步输出只能在这里
     *        借道进日志 —— 否则那条记录会缺掉「清理做没做成」这一段。
     *        （原「管理器补齐」那一路已随方案 B 删除：管理器在跑链**之前**就装完，
     *        日志由跑链那次自己写，不再需要跨进程借道。）
     * @param finalOutcome   非 null ⇒ 用它替代按 [dfArmedThisBoot] 推出来的结局。
     *        用途是表达「注入成功、但流程并未真正走完」。round35 起 RootKitApp 传 null
     *        （管理器已在跑链前装好，注入成功即代表流程走完），本参数保留给将来的
     *        同类需求，行为不变。
     * @param finalDetail    与 [finalOutcome] 配套的短原因（null = 沿用原有推导）。
     */
    @Synchronized
    fun recoverPending(
        context: Context,
        dfArmedThisBoot: Boolean,
        extraLogLines: List<String> = emptyList(),
        finalOutcome: RunOutcome? = null,
        finalDetail: String? = null,
    ) {
        val pending = readPending(context) ?: return
        deletePending(context)
        if (pending.outcome != RunOutcome.RUNNING) return
        val armed = pending.source == RunSource.DF && dfArmedThisBoot
        append(
            context,
            pending.copy(
                outcome = finalOutcome ?: if (armed) RunOutcome.SUCCESS else RunOutcome.ABORTED,
                detail = finalDetail ?: if (armed) "已注入（跑链因软重启中断，本次开机已生效）"
                else "运行中断（进程退出 / 软重启）",
                logLines = pending.logLines + extraLogLines,
            ),
        )
    }

    @Synchronized
    fun loadAttempts(context: Context): List<RunAttempt> {
        val f = file(context)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            val out = mutableListOf<RunAttempt>()
            for (i in 0 until arr.length()) {
                out.add(fromJson(arr.getJSONObject(i)))
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** 清空全部记录（含未完成的暂存记录 —— 否则清完又冒出来一条）。 */
    @Synchronized
    fun clear(context: Context) {
        file(context).delete()
        deletePending(context)
    }

    // ─────────────────────────── 内部 ───────────────────────────

    /** 插入到列表头（newest first）并按上限截断；任何异常都吞掉（日志不该让功能崩）。 */
    private fun append(context: Context, attempt: RunAttempt) {
        val list = loadAttempts(context).toMutableList()
        list.add(0, attempt)
        while (list.size > MAX_ENTRIES) list.removeAt(list.size - 1)
        val arr = JSONArray()
        for (a in list) arr.put(toJson(a))
        try {
            file(context).writeText(arr.toString())
        } catch (e: Exception) {
            android.util.Log.e("RunLogStore", "failed to save run attempt log: ${e.message}")
        }
    }

    private fun writePending(context: Context, attempt: RunAttempt) {
        try {
            pendingFile(context).writeText(toJson(attempt).toString())
        } catch (e: Exception) {
            android.util.Log.e("RunLogStore", "failed to save pending run log: ${e.message}")
        }
    }

    private fun readPending(context: Context): RunAttempt? {
        val f = pendingFile(context)
        if (!f.exists()) return null
        return try {
            fromJson(JSONObject(f.readText()))
        } catch (_: Exception) {
            null
        }
    }

    private fun deletePending(context: Context) {
        try {
            pendingFile(context).delete()
        } catch (_: Exception) {
        }
    }

    private fun toJson(a: RunAttempt): JSONObject {
        val o = JSONObject()
        o.put("id", a.id)
        o.put("timestamp", a.timestamp)
        o.put("source", a.source.name)
        o.put("outcome", a.outcome.name)
        // 兼容旧版本的读取路径（v6.0.2 及以前只认 success 布尔）：多写一个字段不影响新读取。
        o.put("success", a.outcome == RunOutcome.SUCCESS)
        o.put("model", a.model)
        o.put("firmware", a.firmware)
        o.put("detail", a.detail)
        val lines = JSONArray()
        for (l in a.logLines) lines.put(l)
        o.put("logLines", lines)
        return o
    }

    private fun fromJson(o: JSONObject): RunAttempt {
        val lines = mutableListOf<String>()
        o.optJSONArray("logLines")?.let { la ->
            for (j in 0 until la.length()) lines.add(la.getString(j))
        }
        // 旧记录没有 source / outcome：source 缺省 SRK（v6.0.2 只记 SRK 向导），
        // outcome 由旧的 success 布尔推导 —— 保证升级后历史记录仍能正常显示。
        val source = runCatching {
            RunSource.valueOf(o.optString("source", RunSource.SRK.name))
        }.getOrDefault(RunSource.SRK)
        val rawOutcome = o.optString("outcome", "")
        val outcome = if (rawOutcome.isEmpty()) {
            if (o.optBoolean("success", false)) RunOutcome.SUCCESS else RunOutcome.FAILED
        } else {
            runCatching { RunOutcome.valueOf(rawOutcome) }.getOrDefault(RunOutcome.FAILED)
        }
        val ts = o.optLong("timestamp", 0L)
        return RunAttempt(
            id = o.optLong("id", ts),
            timestamp = ts,
            source = source,
            outcome = outcome,
            model = o.optString("model", ""),
            firmware = o.optString("firmware", ""),
            detail = o.optString("detail", ""),
            logLines = lines,
        )
    }
}

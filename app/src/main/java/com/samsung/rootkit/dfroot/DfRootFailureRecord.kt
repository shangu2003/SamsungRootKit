package com.samsung.rootkit.dfroot

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File

/**
 * df（DFRoot）的**失败版本记录** —— 只回答一个问题：
 * 「**当前这个固件**上，df 提权是否已经**连续失败到阈值**（[FAIL_THRESHOLD] 次）？」
 *
 * <h2>★ 连续 3 次才算失败（2026-10-08 用户修正 —— 本版本核心变更）</h2>
 * 旧规则是「失败 **1** 次即拉黑」。用户指出其缺陷，原话：
 * 「DF 链同样是用了漏洞，是有可能一次不成功的，不代表下次不成功，
 * 需要改为记录 3 次 root 不成功，自动关闭 df 总开关」。
 * 理由成立：漏洞利用本就存在**单次**失败（时序 / 内存布局 / 竞态 / 一次不中），
 * 这类失败**不代表该固件已被修补**；一次失败就锁死入口并关掉总开关，
 * 会把一台其实可用的设备误判为「不支持」，且用户没有任何自救途径
 * （开关打不开、天天弹窗），只能等 OTA —— 代价远大于收益。
 *
 * <p>故改为 **累计 [FAIL_THRESHOLD]（=3）次连续失败** 才拉黑：
 * <ul>
 *   <li><b>第 1、2 次失败</b>：**只计数**（写入本文件），总开关**保持打开**、
 *       入口**不拦**、设置页**不弹**「当前系统版本不支持」⇒ 用户可直接重试；</li>
 *   <li><b>第 3 次失败</b>（[DfRootFailureRecord.fails] ≥ [FAIL_THRESHOLD]）：
 *       才算 [DfRootFailureRecord.confirmed] —— 此时才写黑名单语义、关总开关、
 *       拦入口（即旧规则的全部行为）；</li>
 *   <li><b>中途成功一次即清零</b>（[DfRootFailureRecordStore.clear]）——
 *       判的是**连续**失败；成功本身已经证明该固件可用。</li>
 * </ul>
 * 计数与固件绑定：固件一变（OTA），计数从 0 重新开始。
 *
 * <p><b>计数口径（已知取舍，勿当作缺陷）</b>：按**失败次数**累计，而非按开机次数。
 * 同一开机内的重试同样计数 —— `/dev/df` 是 O_EXCL 互斥锁，同一开机重复跑链
 * 可能因「锁已被消费」而再失败一次（属误导性失败），也会占一格。这是刻意的：
 * 规则要可预测（用户数到 3 次就是 3 次），且应用内在失败后本就提示
 * 「先重启设备再跑」（见 `resultFailedDesc`），正常按提示操作时不会误占格。
 *
 * <h2>为什么只记失败（2026-10-05 用户定调）</h2>
 * 用户原话：「这个文件只记录 df 失败的系统版本号，不记录成功的。」
 * 于是本文件的语义从「实测能力档案」收敛为 **df 的版本黑名单**，答「是」时三处联动：
 * <ol>
 *   <li>启动判定 [DfRootLaunchResolver] 出口走回退（[DfRootLaunchState.UNSUPPORTED]）；</li>
 *   <li>设置页「DFRoot 总开关」**打不开**（UI 侧拦截 + 弹「当前系统版本不支持」）；</li>
 *   <li>跑链失败当场自动关闭总开关，并引导进入 SRK 的 Root 流程。</li>
 * </ol>
 * 成功的设备**不需要**任何记录：成功即已拿到 root，`/dev/df`（[DfRootBridge.DEV_DF_MUTEX]）
 * 与内核自报版本码是更硬的事实，读序上排在记录之前（见 [DfRootLaunchResolver]）。
 *
 * <h2>固件版本绑定 —— 系统升级即自动解锁</h2>
 * 记录带 [Build.VERSION.INCREMENTAL]（就是设置页「固件版本」那一行，OTA 必变）。
 * [DfRootFailureRecordStore.read] 发现与当前固件**不一致** ⇒ 返回 null（视同从未失败），
 * 于是：系统升级后 df 入口自动恢复、手动打开总开关也不再被拦截
 * （用户原话：「读取到文件内记录版本号跟当前系统版本号不一致，则自动打开df功能…
 * 手动到设置里打开df总开关也不拦截，直接放行」）。
 * 「自动打开」那一步由 df 状态刷新执行：它用 [DfRootFailureRecordStore.readRaw]
 * 看出「有记录但版本对不上」⇒ 清掉陈旧记录 + 把总开关写回 true。
 *
 * <h2>哪些结果码算失败</h2>
 * 只认「链真正跑过」的失败：rc=1（ksud 出错）/ 2（超时）/ 3（补丁失败）。
 * <b>rc=9（本次开机已注入）不记</b> —— 那是「无需重复」，不是失败；
 * <b>rc=-1 等链路级失败也不记</b> —— 链根本没执行（无 su / 调用异常），
 * 把它当「设备不支持」会把入口错误地锁死，而这属于 SRK 自身环境问题。
 */
data class DfRootFailureRecord(
    /** 写入时的固件标识（[Build.VERSION.INCREMENTAL]）。 */
    val firmware: String,
    /**
     * 本固件上的**连续失败次数**（2026-10-08 新增）。
     *
     * <p>固件变化时由写入侧归零；成功一次同样归零。≥ [FAIL_THRESHOLD] 即 [confirmed]。
     * 旧记录（本键不存在）按 **1** 次读入 —— 见 [DfRootFailureRecordStore.parse] 的说明。
     */
    val fails: Int,
    /** 产生该记录的跑链结果码（1/2/3；排障用）。 */
    val lastRc: Int,
    /** 写入时刻（epoch ms）。 */
    val ts: Long,
) {
    /** 固件是否与当前一致；不一致 ⇒ 记录作废（视同从未失败，系统升级即解锁）。 */
    val matchesCurrentFirmware: Boolean get() = firmware == currentFirmware()

    /**
     * 是否已达**确认失败**阈值 —— 只有它为 true，记录才具备「黑名单」语义
     * （见类注释：关总开关 / 入口回退 / 设置页拦下，全部只认这个判据）。
     *
     * <p>注意：它**不含**固件校验。要判「当前是否该拦」请用
     * [DfRootFailureRecordStore.read]（= 固件一致 **且** 已达阈值）。
     */
    val confirmed: Boolean get() = fails >= FAIL_THRESHOLD

    /** 还差几次才触发自动关闭（已达阈值 ⇒ 0）。只用于文案/日志。 */
    val remaining: Int get() = (FAIL_THRESHOLD - fails).coerceAtLeast(0)

    companion object {
        /**
         * 触发「确认失败」所需的**连续失败次数**（2026-10-08 用户定调为 3）。
         *
         * <p>改这一个常量即可调整策略，三处联动（关总开关 / 入口回退 / 设置页拦下）
         * 与全部文案都由它派生 —— 不要再在别处硬编码 3。
         */
        const val FAIL_THRESHOLD = 3

        /** 当前固件标识。 */
        fun currentFirmware(): String = Build.VERSION.INCREMENTAL ?: ""

        /**
         * 系统区「支持」记录路径（df 链内代写，2026-10-04 拍板②）。
         *
         * <p>与私有区的**失败**记录是两回事、两个文件：那个文件是「df 链在这台设备上
         * 成功过」的系统级证据，写入方（df 链内代写）**尚未落地**。读接口先行 ——
         * 文件不存在时恒 false，未来 df 侧落地后 SRK 零改动。
         */
        const val SYSTEM_ZONE_FILE = "/data/local/tmp/dfroot_supported"
    }
}

/**
 * [DfRootFailureRecord] 的存取。
 *
 * <p>文件：本应用**设备保护存储（DE）**的 `filesDir/dfroot_failure_record.txt`，
 * KV 空格分隔：`failed=1 firmware=<INCREMENTAL> fails=<int> last_rc=<int> ts=<ms>`
 *
 * <p>★ `fails`（连续失败次数）是 2026-10-08 新增的键。它的存在使本文件从
 * 「一次性事实」变成「可累计的计数器」：**每次失败都在既有值上加一**，
 * 达 [DfRootFailureRecord.FAIL_THRESHOLD] 才升级为黑名单。旧记录没有该键 ⇒
 * 读作 1（见 [parse]）。
 *
 * <p>⚠️ **必须走 DE 存储**（`createDeviceProtectedStorageContext()`）而不是普通 [Context.filesDir]。
 * 读写方有两处、且运行阶段不同：
 * <ul>
 *   <li>[DfBootReceiver] —— 开机自启跑链后写，可能发生在 **LOCKED_BOOT_COMPLETED**，
 *       此时 CE 存储尚未挂载，写 CE 会失败（或落到一个解锁后不可见的路径）；</li>
 *   <li>UI（[com.samsung.rootkit.ui] 的 df 状态刷新 / 设置页开关拦截）—— 解锁后读写。</li>
 * </ul>
 * 两者若各用一侧，会出现「开机跑链写了、UI 却读不到 ⇒ 明明失败过却仍放行」这种
 * 只在锁屏重启路径下复现的隐蔽缺陷。统一走 DE 后两侧看到同一份文件。
 *
 * <h2>为什么不复用 2026-10-04 那个 `dfroot_support_record.txt`</h2>
 * 旧文件是**双态**的（`supported=1` 成功 / `supported=0` 不支持），文件名与格式都服务于
 * 「实测能力档案」。本文件只记失败，若沿用旧名会误导后来者；若沿用旧**格式**去读，
 * 一条既有的 `supported=1`（用户设备上必然存在 —— 成功过才会用到今天）会被误判成失败、
 * 把 df 入口和总开关一起锁死。故换名换格式，旧文件**有意弃用**（不再读、不再写、
 * 留在磁盘上不删：它是历史证据，删除没有任何收益）。
 */
object DfRootFailureRecordStore {

    private const val TAG = "DFROOT"
    private const val FILE_NAME = "dfroot_failure_record.txt"

    private fun file(ctx: Context): File =
        File(ctx.createDeviceProtectedStorageContext().filesDir, FILE_NAME)

    /**
     * 读记录：**仅当固件与当前一致、且已达 [DfRootFailureRecord.FAIL_THRESHOLD] 次**时
     * 返回非 null。即本方法是「当前是否该拉黑」的**唯一判据**。
     *
     * <p>不存在 / 解析失败 / **固件已变** / **失败次数未达阈值** ⇒ null。
     * 返回非 null 即表示「当前固件上 df 已确认失败」，调用方无需再校验 ——
     * 于是三处联动（关总开关 / 入口回退 / 设置页拦下）**自动**只在第 3 次失败后生效，
     * 无需各自再判一次次数（这正是把阈值放在这一层的原因）。
     *
     * <p>只想看「失败过几次」（如失败提示文案要显示「第 N 次」）请用
     * [strikesOnCurrentFirmware]，**不要**用本方法 —— 未达阈值时它返回 null。
     */
    fun read(ctx: Context): DfRootFailureRecord? =
        readRaw(ctx)?.takeIf { it.matchesCurrentFirmware && it.confirmed }

    /**
     * 当前固件上的**连续失败次数**（0 = 无记录 / 固件已变 / 解析失败）。
     *
     * <p>与 [read] 的区别：本方法**不套阈值**，未达阈值也如实返回 1 / 2。
     * 用途仅限文案与日志（「已连续失败 2 次，共需 3 次才会自动关闭」），
     * 判「该不该拦」必须用 [read]。
     */
    fun strikesOnCurrentFirmware(ctx: Context): Int =
        readRaw(ctx)?.takeIf { it.matchesCurrentFirmware }?.fails ?: 0

    /** 记录文件当前是否存在（任一固件）；用于避免无谓的删写。 */
    fun exists(ctx: Context): Boolean = try {
        file(ctx).isFile
    } catch (_: Throwable) {
        false
    }

    /**
     * 读记录，**不做固件校验**（返回写入当时的固件标识）。
     *
     * <p>唯一用途：让调用方看出「存在一条记录，但它属于**旧固件**」——
     * 这正是「系统升级过、该给 df 解锁了」的判据（见 [read] 的 KDoc）。
     * 判断「该不该拦」请用 [read]，不要用本方法。
     */
    fun readRaw(ctx: Context): DfRootFailureRecord? = try {
        val f = file(ctx)
        if (!f.isFile) null else parse(f.readText())
    } catch (t: Throwable) {
        Log.w(TAG, "read failure record failed", t)
        null
    }

    private fun parse(text: String): DfRootFailureRecord? {
        val map = text.trim().split(' ').mapNotNull {
            val kv = it.split('=', limit = 2)
            if (kv.size == 2) kv[0] to kv[1] else null
        }.toMap()
        val firmware = map["firmware"] ?: return null
        // 必须显式带 failed=1 才算失败记录：缺字段（含旧格式 supported=…）一律当「无记录」，
        // 宁可多给用户一次尝试机会，也绝不把「成功过」误读成「失败过」。
        if (map["failed"] != "1") return null
        // fails 缺失 ⇒ 旧规则（一次即锁）写下的记录，**读作 1 次**。
        // 理由：那条记录承载的正是用户已否决的「一次失败就锁死」策略，不能让它继续
        // 以「已确认」的身份生效（否则用户装上新版仍被旧记录锁着，会以为修复没生效）。
        // 读作 1 之后，它只是「第一次失败」，用户还拿到 [FAIL_THRESHOLD]-1 次机会；
        // 若真是已修补的固件，再失败两次即重新锁定，不会无限重试。
        // coerceAtLeast(1)：解析出 0/负数一律按 1 处理（0 会让「有记录」与
        // 「没失败过」自相矛盾）。
        val fails = (map["fails"]?.toIntOrNull() ?: 1).coerceAtLeast(1)
        return DfRootFailureRecord(
            firmware = firmware,
            fails = fails,
            lastRc = map["last_rc"]?.toIntOrNull() ?: -1,
            ts = map["ts"]?.toLongOrNull() ?: 0L,
        )
    }

    /**
     * 记一次「本固件上 df 提权失败」：**在既有计数上加一**并整份重写（IO 线程调用）。
     *
     * <p>调用方必须已确认 rc 属于「链真正跑过且失败」（1/2/3）——
     * 见 [com.samsung.rootkit.dfroot.DfRootManager.handleChainFailure]，那是唯一入口。
     *
     * <p>两条归零规则（都在本方法内落实，调用方不必管）：
     * <ul>
     *   <li>既有记录的**固件与当前不同** ⇒ 从 0 起算（旧固件的失败与新固件无关）；</li>
     *   <li>既有记录**已达阈值**（理论上不该再失败；防御性）⇒ 仍按加一处理，
     *       使计数保持单调、不会因为再次失败而「变轻」。</li>
     * </ul>
     *
     * @return 写入后的记录（含新计数）；**写盘失败返回 null** ——
     *         此时调用方应**放弃关总开关**（计数都没落盘，谈何「连续 3 次」；
     *         宁可让它继续重试，也不能凭一次无法记录结果就锁死设备）。
     */
    fun recordFailure(ctx: Context, lastRc: Int): DfRootFailureRecord? = try {
        val prev = readRaw(ctx)?.takeIf { it.matchesCurrentFirmware }
        val rec = DfRootFailureRecord(
            firmware = DfRootFailureRecord.currentFirmware(),
            fails = (prev?.fails ?: 0) + 1,
            lastRc = lastRc,
            ts = System.currentTimeMillis(),
        )
        file(ctx).writeText(
            "failed=1 firmware=${rec.firmware} fails=${rec.fails} " +
                "last_rc=${rec.lastRc} ts=${rec.ts}",
        )
        rec
    } catch (t: Throwable) {
        Log.w(TAG, "recordFailure failed", t)
        null
    }

    /**
     * 清除记录（= 连续失败计数归零 + 解除黑名单）。
     *
     * <p>三个调用点：① 系统升级后（旧固件的结论作废）；
     * ② **df 链在本固件上成功一次**（见 `DfRootManager.handleChainSuccess`）——
     * 阈值判的是**连续**失败，成功已证明该固件可用，必须清零，
     * 否则「失败 2 次 → 成功 → 再失败 1 次」会被误判成 3 连败而锁死；
     * ③ 将来若有「重置 df 实测状态」入口。
     */
    fun clear(ctx: Context) {
        try {
            file(ctx).delete()
        } catch (_: Throwable) {
        }
    }

    /**
     * 读系统区「支持」记录（[DfRootFailureRecord.SYSTEM_ZONE_FILE]，df 链内代写）。
     *
     * <p>读取零权限（与兜底文件同目录同权限模型）。写入方未落地前文件不存在 ⇒ 恒 false。
     */
    fun readSystemZoneSupported(): Boolean = try {
        val f = File(DfRootFailureRecord.SYSTEM_ZONE_FILE)
        f.isFile && f.readText().contains("supported=true")
    } catch (_: Throwable) {
        false
    }
}

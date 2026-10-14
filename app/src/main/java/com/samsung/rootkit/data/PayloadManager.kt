package com.samsung.rootkit.data

import android.content.Context
import java.io.File
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

/**
 * Payload 管理器
 *
 * 完全复刻 run_root.sh / run_root.cmd 的 payload 逻辑：
 * 1. 从 assets/payload/<机型版本>/<profileId>__payload|__root 递归读取（不再使用加密 pack）
 * 2. 解析 payloads.list (JSON) 获取设备元数据与 aliasOf 复用关系
 * 3. 根据设备 fingerprint 自动匹配
 * 4. 根据 KMI 选择正确的 ksud
 *
 * 目录结构（assets/payload/）：
 *   assets/payload/
 *     ├── <firmwareVersion>/           例如 S9180ZHS8FZG1
 *     │   ├── <profileId>__payload     例如 dm3q-S9180ZHS8FZG1__payload
 *     │   └── <profileId>__root        例如 dm3q-S9180ZHS8FZG1__root
 *     └── …（每个机型版本一个子目录）
 * 无独立二进制的机型（aliasOf 非空）复用 aliasOf 指向的源 profile 文件，可跨子目录。
 */
class PayloadManager(private val context: Context) {

    // ---- 双变体：KSU（源版）/ KernelSU-Next ----
    // 必须声明在【类体】而不是 companion 内：companion 里的嵌套类无法用
    // `PayloadManager.KsuVariant` 这种限定形式引用（Kotlin 不支持），
    // 放到类体后外部可用 PayloadManager.KsuVariant，内部则直接写 KsuVariant。
    enum class KsuVariant { KSU, KSU_NEXT }

    /**
     * 系列 = 内核代际。KMI 唯一决定系列，系列决定 ksud 与 root 用哪一份：
     *   5.15 → S23 / 6.1 → S24 / 6.6 → S25 / 6.12 → S26
     */
    enum class KsuSeries(val kmiLabel: String) {
        S23("android13-5.15"),
        S24("android14-6.1"),
        S25("android15-6.6"),
        S26("android16-6.12"),
    }

    companion object {
        // 设备上的目标路径 (与 run_root.sh 一致)
        const val DEV_PAYLOAD = "/data/local/tmp/cve-2026-43499"
        const val DEV_ROOT = "/data/local/tmp/cve-2026-43499-root"
        const val DEV_KSUD = "/data/local/tmp/ksud-selected"

        /** 本工具在 /data/local/tmp 下的产物目录 */
        const val DEV_TMP_DIR = "/data/local/tmp"

        /**
         * 「本工具在 /data/local/tmp 下的产物」名单 —— 清理时**只删这些**，不动该目录下属于
         * 其他工具的文件（旧实现用 `-mindepth 1 -maxdepth 1` 无条件全清，会误删他人文件）。
         *
         * 每一项都有本工程内的源码出处，不是凭记忆罗列：
         *   cve-2026-43499        ← DEV_PAYLOAD（推送的 payload）
         *   cve-2026-43499-root   ← DEV_ROOT（推送的 root helper）
         *   ksud-selected         ← DEV_KSUD（推送的 ksud）
         *   .ksud-stage           ← RootExecutor 的 srk 分支补 staging
         *   ksud-s25u-kdp         ← RootExecutor 的 rms24 分支硬编码落地路径
         *   temp_su.sock          ← rms24 触发前清理 + KSU su 的临时 socket
         *   ksu_manager.apk       ← 安装管理器 APK 时的中转文件
         *   reboot_guard*         ← KSU su 守护进程在本工具提权后创建的 socket
         *
         * <h2>round34（2026-10-06）补齐的 7 项</h2>
         * 上面 8 项是 round22 的原始名单；下面这批同样是本工程写在 /data/local/tmp 的东西，
         * 此前**漏登记**（清不掉就一直是垃圾）：
         *   df_reroot.apk         ← DfBridge.DEV_DF_APK（dfe 首次配置安装时推送的中转包）
         *   df_sr.claim           ← ksud_next_wrap.sh 软重启通道的 once 锁（mkdir 目录）
         *   df_sr.log  df_wd.log  ← 同上，诊断日志
         *   df_mgr.log            ← ksud_wrap.sh 1b 管理器通道的追踪日志
         *   df_mgr.lock           ← 同上，mkdir 原子互斥锁
         *   .dfw.*                ← ksud_wrap.sh 的 /data/local/tmp 可写性探针临时文件
         *
         * <h2>★ 刻意**不**列入的两项（考古查证，别顺手加回来）</h2>
         *   · `srk-ksud-probe` —— [com.samsung.rootkit.exploit.RootExecutor.KSUD_PROBE_PATH]，
         *     它的声明处注释明写「刻意不列入清理名单」：那是「推一次、长期复用」的
         *     5.7 MB ksud 缓存，列入会让每次提权前都清掉、然后反复重推。
         *   · `dfroot_supported` —— [com.samsung.rootkit.dfroot.DfRootFailureRecord.SYSTEM_ZONE_FILE]，
         *     那是**系统级证据记录**（「df 链在这台设备上成功过」）而不是缓存，清掉等于抹证据。
         *
         * <h2>★ 与链内清理的一致性</h2>
         * `app/src/main/assets/dfroot/ksud_wrap.sh` 与 `ksud_next_wrap.sh` 的「1a 段」
         * 有一份**同样内容**的 shell 名单（在 UMH 的 uid0 + permissive 窗口里以 root 直接删，
         * 零外部依赖）。两处必须逐项一致，`_audit/round34/verify_round34.py` 会断言相等。
         */
        val TMP_ARTIFACTS: List<String> = listOf(
            "cve-2026-43499",
            "cve-2026-43499-root",
            "ksud-selected",
            ".ksud-stage",
            "ksud-s25u-kdp",
            "temp_su.sock",
            "ksu_manager.apk",
            "reboot_guard*",
            // ── round34 补齐 ──
            "df_reroot.apk",
            "df_sr.claim",
            "df_sr.log",
            "df_wd.log",
            "df_mgr.log",
            "df_mgr.lock",
            ".dfw.*",
        )

        /**
         * 生成「只删本工具产物」的 shell 片段。
         * shell(uid 2000) 与 su 两种身份复用同一条命令，避免两处名单漂移。
         */
        fun buildTmpArtifactCleanCommand(): String {
            val names = TMP_ARTIFACTS.joinToString(" -o ") { "-name '$it'" }
            return "find $DEV_TMP_DIR -mindepth 1 -maxdepth 1 \\( $names \\) -exec rm -rf {} + 2>/dev/null"
        }

        /** 统计 /data/local/tmp 下**仍存在的本工具产物**数量（用于清理结果上报，不含他人文件） */
        fun buildTmpArtifactCountCommand(): String {
            val names = TMP_ARTIFACTS.joinToString(" -o ") { "-name '$it'" }
            return "find $DEV_TMP_DIR -mindepth 1 -maxdepth 1 \\( $names \\) 2>/dev/null | wc -l"
        }

        // 资产目录
        const val TOOLS_KSU_DIR = "tools/ksu_tools"
        const val TOOLS_KSU_NEXT_DIR = "tools/ksu_next_tools"
        const val ROOT_SUBDIR = "root"

        // 管理器 APK 与包名（两者包名不同，安装/唤起都要按变体走）
        const val KSU_APK = "KernelSU_Manager_v3.3.0_32601.apk"
        const val KSU_NEXT_APK = "KernelSU_Next_v3.4.0_33294.apk"
        const val PKG_KSU = "me.weishu.kernelsu"
        const val PKG_KSU_NEXT = "com.rifsxd.ksunext"

        // ── 两个变体的「内核版本码」（2026-10-02 真机联调新增）──────────────────
        //
        // 与上面两个 APK 文件名里的数字**同源**：KernelSU 家族的构建约定是同一次编译
        // 产出的三件套共享同一个版本码 —— 内核 KSU_VERSION == ksud 编译期版本码 ==
        // 管理器 APK 的 versionCode。因此内核通过 `ksud debug version` 自报的版本码，
        // 可以直接映射回「内核里跑的是哪一支」。
        //
        // 实测依据（2026-10-02，SM-S9180 / dm3q / 内核 5.15.189-android13）：
        //   1) 本包资产 ksu_tools/ksud-samsung-multi-kdp       内嵌版本码 32601
        //      （＝本包管理器 KernelSU_Manager_v3.3.0_32601.apk 的 versionCode）
        //   2) 本包资产 ksu_next_tools/ksud-samsung-multi-kdp-next 内嵌版本码 33294
        //      （＝本包管理器 KernelSU_Next_v3.4.0_33294.apk 的 versionCode）
        //   3) 两者对**同一个正在运行的内核**执行 `debug version`，都回
        //      `Kernel Version: 33294` ⇒ 该数字来自内核、不是 ksud 自身版本
        //      （两支 ksud 的 `--version` 分别为 783d3d87 / 3.4.0，并不相同）。
        //
        // 用途：见 [com.samsung.rootkit.exploit.RootExecutor.detectKernelVariant]。
        // 维护：升级载荷时，这三处（APK 文件名、本常量、SHA256SUMS.txt）必须同步改。
        const val KSU_KERNEL_VERSION = 32601L
        const val KSU_NEXT_KERNEL_VERSION = 33294L

        // ── DFReroot 持久化集成（v5.1.0）──
        // 包名与 DFReroot 的 applicationId 逐字一致（D2 决策：不改包名，JNI 类名
        // org.lsposed.lspromise.DirtyFrag 亦不可改，见其 exp.c:45 的 FindClass）。
        // 必须同时在 AndroidManifest 的 <queries> 里声明，否则包可见性过滤会让
        // getApplicationInfo 抛异常，两处状态检测全废。
        const val PKG_DF = "com.polygraphene.df.reroot"

        /** 内置的 DFReroot 集成 APK（构建脚本灌入，见 assets/dfreroot/README.txt） */
        const val DF_APK_ASSET = "dfreroot/df_reroot.apk"

        /**
         * 主线 KernelSU 的 ksud（v6.0.2 起）：单一二进制内嵌全部受支持的 KMI
         * （android12-5.10 / android13-5.10 / android13-5.15 / android14-5.15 /
         *  android14-6.1 / android15-6.6 / android16-6.12 / android17-6.18），
         * 由 ksud 自身在运行时按内核 KMI 挑选对应的 kernelsu.ko，
         * 因此主线变体不再按系列分文件，统一用这一份。
         */
        const val KSUD_UNIFIED = "ksud-samsung-multi-kdp"

        /**
         * KernelSU-Next 的 ksud（2026-10-02 起）：与主线同策略——单一二进制内嵌
         * 全部 8 个受支持 KMI 的 kernelsu.ko（含 android16-6.12 / S26），
         * 由 ksud 自身在运行时按内核 KMI 挑选，故 Next 变体也不再按系列分文件。
         * 这是 Next 变体获得 S26 支持的关键：此前 Next 按系列分文件且缺 6.12 资产。
         * 已随本次改动移除：ksud-samsung-android{13-5.15,14-6.1,15-6.6}-kdp-next。
         */
        const val KSUD_NEXT_UNIFIED = "ksud-samsung-multi-kdp-next"

        /**
         * ksud 资产文件名（按变体）。
         * 两个变体都改用各自「统一多 KMI 版」，不再随系列变化：
         *   主线（KSU）→ [KSUD_UNIFIED]（ksud-samsung-multi-kdp）
         *   Next       → [KSUD_NEXT_UNIFIED]（ksud-samsung-multi-kdp-next）
         */
        fun ksudFileName(variant: KsuVariant): String =
            if (variant == KsuVariant.KSU_NEXT) KSUD_NEXT_UNIFIED
            else KSUD_UNIFIED

        /** root 资产文件名（按系列 + 变体；Next 的 S24/S25/S26 与 KSU 版同哈希，但按变体独立命名存放） */
        fun rootFileName(series: KsuSeries, variant: KsuVariant): String =
            if (variant == KsuVariant.KSU_NEXT) "root-samsung-${series.kmiLabel}-next"
            else "root-samsung-${series.kmiLabel}"

        /** KMI 字符串 → 系列。必须先判 6.12，否则 "android16-6.12" 会被 "6.1" 抢先命中 */
        fun seriesOfKmi(kmi: String): KsuSeries? = when {
            kmi.contains("6.12") -> KsuSeries.S26
            kmi.contains("5.15") -> KsuSeries.S23
            kmi.contains("6.1") -> KsuSeries.S24
            kmi.contains("6.6") -> KsuSeries.S25
            else -> null
        }

        /** 内核版本（uname -r，如 5.15.189-android13-...）→ 系列 */
        fun seriesOfKernel(kernelVersion: String): KsuSeries? = seriesOfKmi(kernelVersion)

        // 2026-10-02：原 `variantSupports(variant, series)`（KSU-Next × S26 门禁）
        // 已整条移除。Next 变体自本版起改用统一多-KMI ksud（内嵌 android16-6.12
        // 的 kernelsu.ko），S26 与 KSU 变体同等支持，故不再需要任何系列级拦截。

        // 展示名与首页按钮文案保持一致（红=KernelSU / 蓝=KernelSU-Next），
        // 用于日志与用户可见提示；仅作显示，不参与任何字符串比较或分支判断。
        fun variantDisplayName(variant: KsuVariant): String =
            if (variant == KsuVariant.KSU_NEXT) "KernelSU-Next" else "KernelSU"

        fun managerPackage(variant: KsuVariant): String =
            if (variant == KsuVariant.KSU_NEXT) PKG_KSU_NEXT else PKG_KSU

        /**
         * 内核自报版本码 → 变体（依据见 [KSU_KERNEL_VERSION]）。
         *
         * 只认本包载荷登记过的两个版本码；**未登记一律返回 null（不猜）** ——
         * 调用方宁可回退到旧的用户态判据，也不能凭"数字更大就是 Next"这类臆断下结论。
         */
        fun variantOfKernelVersion(code: Long): KsuVariant? = when (code) {
            KSU_KERNEL_VERSION -> KsuVariant.KSU
            KSU_NEXT_KERNEL_VERSION -> KsuVariant.KSU_NEXT
            else -> null
        }

        fun getModelDisplayName(board: String): String = when (board) {
            "dm1q" -> "Galaxy S23 (SM-S9110)"
            "dm2q" -> "Galaxy S23+ (SM-S9160)"
            "dm3q" -> "Galaxy S23 Ultra (SM-S9180)"
            "e1q" -> "Galaxy S24 (SM-S9210)"
            "e2q" -> "Galaxy S24+ (SM-S9260)"
            "e3q" -> "Galaxy S24 Ultra (SM-S9280)"
            "pa1q" -> "Galaxy S25 (SM-S9310)"
            "pa2q" -> "Galaxy S25+ (SM-S9360)"
            "pa3q" -> "Galaxy S25 Ultra (SM-S9380)"
            "psq" -> "Galaxy S25 FE (SM-S9370)"
            "q5q" -> "Galaxy Z Fold5 (SM-F9460)"
            "b5q" -> "Galaxy Z Flip5 (SM-F7310)"
            "b6q" -> "Galaxy Z Flip6 (SM-F7410)"
            "b7q" -> "Galaxy Z Flip7 (SM-F7660)"
            "q6q" -> "Galaxy Z Fold6 (SM-F9560)"
            // 修正：SM-F9660 实为 Z Fold7（原误标 Fold6），与「关于」页 modelVersions 及上游命名对齐
            "q7q" -> "Galaxy Z Fold7 (SM-F9660)"
            "w9025" -> "Galaxy W25 (SM-W9025)"
            "w9026" -> "Galaxy W26 (SM-W9026)"
            "gts11wifi" -> "Galaxy Tab S11 (SM-X730)"
            "m1q" -> "Galaxy S26 (SM-S942)"
            "m2q" -> "Galaxy S26+ (SM-S9470)"
            "m3q" -> "Galaxy S26 Ultra (SM-S948)"
            else -> board
        }

        /**
         * 仅展示用：根据固件版本号中的 CSC 区域码推导地区（国行/港版/美版/韩版/台版/国际版）。
         * 说明：这是启发式推导，并非 PC 端“联网核实”的权威结果；无法识别时返回“未知”。
         * 仅用于 UI 展示，绝不影响 Root 运行逻辑。
         */
        fun regionFromFirmware(firmware: String): String {
            val u = firmware.uppercase()
            return when {
                u.contains("ZHS") -> "港版"
                u.contains("ZRI") || u.contains("BRI") -> "台版"
                u.contains("ZCS") || u.contains("ZCU") -> "国行"
                u.contains("XX") -> "国际版"
                u.contains("US") || u.contains("UES") -> "美版"
                u.contains("NK") -> "韩版"
                else -> "未知"
            }
        }

        /** 构造「外部自选文件」条目（不走 SHA256SUMS.txt 校验） */
        fun customEntry(
            key: String,
            board: String,
            firmware: String,
            payloadFile: File,
            rootFile: File,
            kmi: String = "",
            displayName: String = "自定义 ($board-$firmware)"
        ): PayloadEntry = PayloadEntry(
            key = key,
            board = board,
            firmware = firmware,
            displayName = displayName,
            payloadFile = payloadFile,
            rootFile = rootFile,
            status = "自定义",
            region = "未知",
            kmi = kmi,
            hasBinary = true,
            isCustom = true
        )
    }

    private val cacheDir = File(context.cacheDir, "payloads_cache")

    /** APK 签名保护清单本身；这里再用清单校验运行前实际读取的二进制，防止损坏或缓存污染。 */
    private val trustedSha256: Map<String, String> by lazy {
        context.assets.open("SHA256SUMS.txt").bufferedReader().useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull { line ->
                    val parts = line.split(Regex("\\s+"), limit = 2)
                    if (parts.size == 2 && parts[0].matches(Regex("[0-9a-fA-F]{64}"))) {
                        parts[1].trim() to parts[0].lowercase()
                    } else null
                }
                .toMap()
        }
    }

    private fun sha256(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(data)
            .joinToString("") { "%02x".format(it) }

    private fun verifyBytes(manifestPath: String, data: ByteArray) {
        val expected = trustedSha256[manifestPath]
            ?: throw SecurityException("missing trusted checksum: $manifestPath")
        val actual = sha256(data)
        if (actual != expected) {
            throw SecurityException("checksum mismatch: $manifestPath")
        }
    }

    private fun verifyFile(manifestPath: String, file: File) {
        if (!file.isFile) throw SecurityException("required payload missing: $manifestPath")
        verifyBytes(manifestPath, file.readBytes())
    }

    /**
     * 在任何 payload 被推送或执行前校验最终选中的文件。
     *
     * 注意：payload 仍与 profile 同目录（键为 `payloads/<文件名>`），
     * 而 root 已改为按「系列 + 变体」共享存放（键为 `tools/ksu_tools/root/...` 或 `tools/ksu_next_tools/root/...`），
     * 所以这里必须用 [rootAssetPathFor] 取键，不能再硬编码 `payloads/` 前缀。
     */
    fun verifyPayloadEntry(entry: PayloadEntry, variant: KsuVariant = KsuVariant.KSU) {
        if (entry.isCustom) return  // 自定义外部文件无 SHA256SUMS 条目，由用户自负责任
        verifyFile("payloads/${entry.payloadFile.name}", entry.payloadFile)
        verifyFile(rootAssetPathFor(entry, variant), rootFileFor(entry, variant))
    }

    /**
     * 在 tools/ksu_tools 与 tools/ksu_next_tools 之间择优解析资源路径。
     *
     * 用 [trustedSha256] 的键做判定：既避免真实 open 大文件，也保证「路径确实是清单里登记的那条」。
     * 文件名带 `-next` 的只会落在 ksu_next_tools，其余落在 ksu_tools，因此不会歧义。
     */
    private fun resolveUnderTools(fileName: String, sub: String? = null): String {
        val rel = if (sub.isNullOrEmpty()) fileName else "$sub/$fileName"
        val a = "$TOOLS_KSU_DIR/$rel"
        val b = "$TOOLS_KSU_NEXT_DIR/$rel"
        return when {
            trustedSha256.containsKey(a) -> a
            trustedSha256.containsKey(b) -> b
            else -> a
        }
    }

    /** ksud 的资源相对路径（清单键） */
    fun ksudAssetPathFor(fileName: String): String = resolveUnderTools(fileName)

    /** 管理器 APK 的资源相对路径（清单键） */
    fun managerApkPath(variant: KsuVariant): String =
        if (variant == KsuVariant.KSU_NEXT) "$TOOLS_KSU_NEXT_DIR/$KSU_NEXT_APK"
        else "$TOOLS_KSU_DIR/$KSU_APK"

    /** 本条目所属系列：优先 payloads.list 的 kmi，其次 board 推断 */
    fun seriesOf(entry: PayloadEntry): KsuSeries? {
        seriesOfKmi(entry.kmi)?.let { return it }
        return when (entry.board) {
            "dm1q", "dm2q", "dm3q", "q5q", "b5q", "gts9u" -> KsuSeries.S23
            "e1q", "e2q", "e3q", "b6q", "q6q", "w9025" -> KsuSeries.S24
            // 注：gts11wifi（Galaxy Tab S11 / SM-X730）曾使用专用 ksud
            // `ksud-samsung-android15-6.6-kdp-x730`，该文件已随 v5.0.7 删除；当前 payloads.list
            // 中没有任何 profile 使用该 board，故这里不再映射（避免日后被静默当成通用 6.6 机型）。
            // 若日后恢复该机型，必须同时恢复专用 ksud 文件与下面的映射。
            "pa1q", "pa2q", "pa3q", "psq", "b7q", "q7q", "w9026" -> KsuSeries.S25
            "m1q", "m2q", "m3q" -> KsuSeries.S26
            else -> null
        }
    }

    /** root 的资源相对路径（清单键） */
    fun rootAssetPathFor(entry: PayloadEntry, variant: KsuVariant): String {
        if (entry.isCustom) return "payloads/${entry.rootFile.name}"
        val series = seriesOf(entry) ?: return "payloads/${entry.rootFile.name}"
        return resolveUnderTools(rootFileName(series, variant), ROOT_SUBDIR)
    }

    /**
     * 解析出可推送到设备的 root 文件（从 assets 提取到缓存）。
     *
     * 缓存复用前先比对 SHA-256（清单里的值），哈希不符或文件缺失才重新提取，
     * 避免换了 root 之后仍用旧缓存。
     */
    fun rootFileFor(entry: PayloadEntry, variant: KsuVariant): File {
        if (entry.isCustom) return entry.rootFile
        val series = seriesOf(entry)
        if (series == null) {
            if (entry.rootFile.isFile) return entry.rootFile
            throw IllegalStateException("cannot resolve KSU series for ${entry.key}")
        }
        val assetPath = resolveUnderTools(rootFileName(series, variant), ROOT_SUBDIR)
        val dirName = if (variant == KsuVariant.KSU_NEXT) "ksu_next_tools" else "ksu_tools"
        val out = File(cacheDir, "tools/$dirName/$ROOT_SUBDIR/${assetPath.substringAfterLast('/')}")
        val expected = trustedSha256[assetPath]
        if (out.isFile && expected != null) {
            try {
                if (sha256(out.readBytes()) == expected) return out
            } catch (_: Exception) { /* 读失败则重新提取 */ }
        }
        out.parentFile?.mkdirs()
        val data = context.assets.open(assetPath).use { it.readBytes() }
        verifyBytes(assetPath, data)
        out.writeBytes(data)
        return out
    }

    /**
     * 计算源资产签名：APK 内 payloads.list 内容 + assets/payload/ 下所有相对路径清单
     * （文件与目录的相对路径），用于让旧缓存在新增机型 / 文件变动 / 改名后失效，强制重新提取。
     * 不再依赖已废弃的 payloads.pack。
     */
    private fun computeSourceSignature(): String {
        val md = MessageDigest.getInstance("SHA-256")
        try {
            context.assets.open("payloads.list").use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
        } catch (_: Exception) {}
        // assets/payload/ 与 assets/tools/ 的递归路径清单（目录 + 文件相对路径），足以检测新增/删除/改名
        val tree = mutableListOf<String>()
        collectAssetPaths("payload", tree)
        collectAssetPaths("tools", tree)
        tree.sorted().forEach { md.update(it.toByteArray(Charsets.UTF_8)) }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** 递归收集 assets 下某路径的所有条目相对路径（文件与目录），用于源签名 */
    private fun collectAssetPaths(assetPath: String, out: MutableList<String>) {
        val names = context.assets.list(assetPath) ?: return
        if (names.isEmpty()) {
            out.add(assetPath) // list 返回空：通常是文件
            return
        }
        for (name in names) {
            val child = if (assetPath.isEmpty()) name else "$assetPath/$name"
            val sub = context.assets.list(child) ?: emptyArray()
            if (sub.isEmpty()) out.add(child) else collectAssetPaths(child, out)
        }
    }

    /**
     * 提取 payload 到缓存目录，返回缓存目录。
     * 新方案：递归复制 assets/payload/ 下所有机型版本子目录与文件（保留 <机型版本>/<文件> 结构），
     * 不再解密 payloads.pack。payloads.list 一并复制到缓存目录供 getPayloadList 解析。
     */
    @Synchronized
    fun extractPayloads(): File {
        val sig = computeSourceSignature()
        val sigFile = File(cacheDir, ".sig")
        if (cacheDir.exists()) {
            // 完整性校验：递归统计 __payload 文件数足够 + payloads.list 存在 + 源签名一致，才复用旧缓存；
            // 否则（数量不足 / 缺少 / 新增机型导致签名变化）强制重建。
            val payloadCount = countPayloadFiles(cacheDir)
            val sigMatches = sigFile.exists() && sigFile.readText() == sig
            if (payloadCount >= 1 && File(cacheDir, "payloads.list").exists() && sigMatches) {
                return cacheDir
            }
            cacheDir.deleteRecursively()
        }
        cacheDir.mkdirs()

        // 递归复制 assets/payload/ 整个目录树到缓存目录（保留 <机型版本>/<文件> 结构）
        try {
            copyAssetTree("payload", cacheDir)
        } catch (e: Exception) {
            throw IllegalStateException("failed to extract assets/payload/: ${e.message}", e)
        }

        // 复制 payloads.list 到缓存目录
        context.assets.open("payloads.list").use { input ->
            File(cacheDir, "payloads.list").outputStream().use { output ->
                input.copyTo(output)
            }
        }

        sigFile.writeText(sig)
        return cacheDir
    }

    /** 递归复制 assets 目录树到目标目录（保留相对结构）。 */
    private fun copyAssetTree(assetPath: String, destDir: File) {
        val names = context.assets.list(assetPath) ?: emptyArray()
        if (names.isEmpty()) {
            // list 返回空：可能是文件（尝试复制），也可能是空目录（忽略）
            try { copyAssetFile(assetPath, destDir) } catch (_: Exception) {}
            return
        }
        for (name in names) {
            val childAsset = if (assetPath.isEmpty()) name else "$assetPath/$name"
            val childDest = File(destDir, name)
            val sub = context.assets.list(childAsset) ?: emptyArray()
            if (sub.isEmpty()) {
                try { copyAssetFile(childAsset, destDir) } catch (_: Exception) {}
            } else {
                childDest.mkdirs()
                copyAssetTree(childAsset, childDest)
            }
        }
    }

    /** 复制单个 assets 文件到目标目录，文件名保持不变 */
    private fun copyAssetFile(assetPath: String, destDir: File) {
        val name = assetPath.substringAfterLast('/')
        context.assets.open(assetPath).use { input ->
            File(destDir, name).outputStream().use { output -> input.copyTo(output) }
        }
    }

    /** 递归统计目录树下 __payload 文件数量 */
    private fun countPayloadFiles(root: File): Int {
        var n = 0
        fun walk(d: File?) {
            d?.listFiles()?.forEach { f ->
                if (f.isDirectory) walk(f) else if (f.name.endsWith("__payload")) n++
            }
        }
        walk(root)
        return n
    }

    /** 递归查找目录树下所有 __payload 文件（忽略 .orig 备份），按文件名排序 */
    private fun findPayloadFiles(root: File): List<File> {
        val result = mutableListOf<File>()
        fun walk(d: File) {
            d.listFiles()?.forEach { f ->
                if (f.isDirectory) walk(f)
                else if (f.name.endsWith("__payload") && !f.name.contains(".orig")) result.add(f)
            }
        }
        walk(root)
        return result.sortedBy { it.name }
    }

    /**
     * 获取所有可用 payload 列表
     *
     * 策略：先解析 payloads.list (JSON) 获取元数据，再扫描缓存目录中的实际 __payload 文件，
     * 双向合并 —— JSON 中有而目录中没有的标为 hasBinary=false，目录中有而 JSON 中没有的用 fallback 元数据。
     * 这确保 pack 中新增的 profile（如 e3q-S928BXXS6DZG1）和 payloads-新增 目录中的额外文件都能被列出。
     */
    fun getPayloadList(): List<PayloadEntry> {
        val dir = extractPayloads()
        val listFile = File(dir, "payloads.list")
        val entries = mutableListOf<PayloadEntry>()
        val seen = mutableSetOf<String>()

        // Step 1: 解析 JSON 获取元数据
        val jsonMeta = mutableMapOf<String, JSONMeta>()
        if (listFile.exists()) {
            try {
                val json = JSONObject(listFile.readText())
                val targets = json.getJSONArray("targets")
                for (i in 0 until targets.length()) {
                    val t = targets.getJSONObject(i)
                    val pid = t.optString("profileId", "")
                    if (pid.isEmpty()) continue
                    val compat = mutableListOf<String>()
                    t.optJSONArray("compatible")?.let { arr ->
                        for (j in 0 until arr.length()) compat.add(arr.getString(j))
                    }
                    jsonMeta[pid] = JSONMeta(
                        modelName = t.optString("modelName", ""),
                        model = t.optString("model", ""),
                        device = t.optString("device", ""),
                        firmware = t.optString("firmwareVersion", ""),
                        status = t.optString("status", ""),
                        driver = t.optString("driver", "srk"),
                        kmi = t.optJSONObject("kernelsu")?.optString("kmi", "") ?: "",
                        compatible = compat,
                        aliasOf = t.optString("aliasOf", "")
                    )
                }
            } catch (_: Exception) { /* JSON 损坏时走 pure directory scan */ }
        }

        // Step 2: 递归扫描缓存目录中所有 __payload 文件（主源，支持机型版本子目录）
        val payloadFiles = findPayloadFiles(dir)

        for (file in payloadFiles) {
            val key = file.name.removeSuffix("__payload")
            seen.add(key)
            val meta = jsonMeta[key]
            val board = meta?.device ?: key.substringBefore("-")
            val fw = meta?.firmware ?: key.substringAfter("-", "")
            val displayName = if (meta != null && meta.modelName.isNotEmpty()) {
                "${meta.modelName} (${meta.model})"
            } else {
                getModelDisplayName(board)
            }
            entries.add(PayloadEntry(
                key = key,
                board = board,
                firmware = fw,
                displayName = displayName,
                // payload/root 位于同一机型版本子目录内，按实际文件父目录解析 root 路径
                payloadFile = file,
                rootFile = File(file.parentFile ?: dir, "${key}__root"),
                status = meta?.status ?: "已测试",
                region = regionFromFirmware(fw),
                kmi = meta?.kmi ?: "",
                compatible = meta?.compatible ?: emptyList(),
                driver = meta?.driver ?: "srk",
                hasBinary = true,
                unsupported = meta == null
            ))
        }

        // Step 3: 补充 JSON 中有但目录中没有的条目
        //   - 有 aliasOf 的条目：复用源 profile 的 payload/root 文件（源文件可能在另一机型版本子目录），
        //     hasBinary 取决于源文件是否存在；isAlias=true 供 UI 标注"兼容适配"
        //   - 无 aliasOf 的条目：标记为 hasBinary=false（JSON 声明但 assets/payload/ 中无该二进制）
        val entryByKey = entries.associateBy { it.key }
        for ((pid, meta) in jsonMeta) {
            if (pid in seen) continue

            val displayName = if (meta.modelName.isNotEmpty()) "${meta.modelName} (${meta.model})" else getModelDisplayName(meta.device)

            if (meta.aliasOf.isNotEmpty()) {
                val source = entryByKey[meta.aliasOf]
                if (source != null && source.hasBinary) {
                    // 复用源文件：payloadFile/rootFile 指向源条目对应的实际文件
                    entries.add(PayloadEntry(
                        key = pid,
                        board = meta.device,
                        firmware = meta.firmware,
                        displayName = displayName,
                        payloadFile = source.payloadFile,
                        rootFile = source.rootFile,
                        status = meta.status,
                        region = regionFromFirmware(meta.firmware),
                        kmi = meta.kmi,
                        compatible = meta.compatible,
                        driver = meta.driver,
                        hasBinary = true,
                        isAlias = true,
                    ))
                    continue
                }
                // 源条目缺失或无二进制：降级为 hasBinary=false
                entries.add(PayloadEntry(
                    key = pid,
                    board = meta.device,
                    firmware = meta.firmware,
                    displayName = displayName,
                    payloadFile = File(dir, "${meta.aliasOf}__payload"),
                    rootFile = File(dir, "${meta.aliasOf}__root"),
                    status = meta.status,
                    region = regionFromFirmware(meta.firmware),
                    kmi = meta.kmi,
                    compatible = meta.compatible,
                    driver = meta.driver,
                    hasBinary = false,
                    isAlias = true,
                ))
            } else {
                entries.add(PayloadEntry(
                    key = pid,
                    board = meta.device,
                    firmware = meta.firmware,
                    displayName = displayName,
                    payloadFile = File(dir, "${pid}__payload"),
                    rootFile = File(dir, "${pid}__root"),
                    status = meta.status,
                    region = regionFromFirmware(meta.firmware),
                    kmi = meta.kmi,
                    compatible = meta.compatible,
                    driver = meta.driver,
                    hasBinary = false
                ))
            }
        }

        return entries.sortedWith(compareBy({ it.board }, { it.firmware }))
    }

    /** JSON 元数据缓存结构 */
    private data class JSONMeta(
        val modelName: String,
        val model: String,
        val device: String,
        val firmware: String,
        val status: String,
        val kmi: String,
        val compatible: List<String>,
        val driver: String = "srk",  // 驱动风味：srk=本App原编排；rms24=root-my-s24 移植载荷编排
        val aliasOf: String = ""  // 非空表示本 profile 无独立二进制，复用 aliasOf 指向的源 profile 的 payload/root 文件
    )

    /**
     * 自动匹配设备 —— 与 run_root.sh 的自动模式逻辑完全一致
     * 读取 ro.build.fingerprint，优先精确匹配 firmwareVersion，再匹配 compatible 列表
     */
    fun autoMatch(fingerprint: String): PayloadEntry? {
        val entries = getPayloadList()
        val f = fingerprint.lowercase()
        // 1. 精确匹配 firmwareVersion；多个命中时取 firmwareVersion 最长（最具体）者，
        //    避免较短固件串（如合并基线短标签 CZG1）抢在更具体的专用固件载荷前面。
        entries.filter { it.firmware.isNotEmpty() && f.contains(it.firmware.lowercase()) }
            .maxByOrNull { it.firmware.length }
            ?.let { return it }
        // 2. 匹配 compatible 列表中的任一固件；多个命中时取标签最长（最具体）者
        entries.flatMap { e ->
                e.compatible.filter { c -> f.contains(c.lowercase()) }.map { it to e }
            }
            .maxByOrNull { it.first.length }
            ?.let { return it.second }
        return null
    }

    /**
     * 选择 ksud 文件名（按变体）。
     *
     * 系列仍要解析——未知 KMI 必须直接抛错（说明设备不在内置覆盖范围内）；
     * 但 ksud 本身两个变体都已是「统一多 KMI 版」，不再随系列变化。
     */
    fun ksudNameFor(
        entry: PayloadEntry,
        kernelVersion: String?,
        variant: KsuVariant,
    ): String {
        val series = seriesOf(entry) ?: seriesOfKernel(kernelVersion.orEmpty())
        if (series == null) {
            throw IllegalArgumentException(
                "unsupported or unknown kernel KMI: ${kernelVersion.orEmpty().ifBlank { "<empty>" }}"
            )
        }
        return ksudFileName(variant)
    }

    /** 从 assets 读取 ksud 二进制文件；用户自带时通过精确路径 customKsudFile 读取 */
    fun getKsudAsset(ksudName: String, customKsudFile: File? = null): ByteArray {
        // 用户自带的 ksud：直接读传入的精确文件路径（走 SAF 导入路径，跳过 SHA256 校验）。
        // 不再扫描 custom_payloads/ 目录，避免残留旧文件导致 firstOrNull 选错。
        if (ksudName == "ksud-custom-user") {
            val f = customKsudFile
                ?: throw IllegalStateException("custom ksud file not provided")
            if (!f.isFile) throw IllegalStateException("custom ksud not found: ${f.absolutePath}")
            return f.readBytes()
        }
        val path = resolveUnderTools(ksudName)
        // 异常包装：AssetManager.open 文件不存在时抛 FileNotFoundException，
        // 不同 Android 版本 message 不统一，统一包成 "ksud not found: <path>" 让 UI 层能稳定识别。
        val data = try {
            context.assets.open(path).use { it.readBytes() }
        } catch (e: java.io.FileNotFoundException) {
            throw IllegalStateException("ksud not found: $path", e)
        }
        verifyBytes(path, data)
        return data
    }

    /**
     * 全部内置 ksud 文件名。两个变体各一份统一多-KMI ksud，共 2 个（2026-10-02 起）。
     * 保留 distinct() 以防未来又出现同一文件被登记两次。
     */
    fun builtinKsudNames(): List<String> =
        listOf(
            ksudFileName(KsuVariant.KSU),
            ksudFileName(KsuVariant.KSU_NEXT),
        ).distinct()

    /** 检测单个内置 ksud 文件是否存在 */
    fun hasBuiltinKsud(name: String): Boolean =
        try {
            context.assets.open(resolveUnderTools(name)).use { true }
        } catch (_: Exception) { false }

    /** 任何一个内置 ksud 文件存在即返回 true；用于 UI 层预检测（用户删除内置 ksud 后提示） */
    fun hasAnyBuiltinKsud(): Boolean = builtinKsudNames().any { hasBuiltinKsud(it) }

    /** 从 assets 读取 KernelSU / KernelSU-Next Manager APK */
    fun getKsuApk(variant: KsuVariant = KsuVariant.KSU): ByteArray {
        val path = managerApkPath(variant)
        val data = context.assets.open(path).use { it.readBytes() }
        verifyBytes(path, data)
        return data
    }

    /** 清理解密缓存 */
    fun clearCache() {
        cacheDir.deleteRecursively()
    }
}

data class PayloadEntry(
    val key: String,
    val board: String,
    val firmware: String,
    val displayName: String,
    val payloadFile: File,
    val rootFile: File,
    val status: String,
    val region: String = "",
    val kmi: String = "",
    val compatible: List<String> = emptyList(),
    val hasBinary: Boolean = true,
    val unsupported: Boolean = false,
    val isCustom: Boolean = false,  // 标记：来自外部用户自选文件，无 SHA256SUMS.txt 校验条目
    val isAlias: Boolean = false,   // 标记：本 profile 复用 aliasOf 源 profile 的二进制文件，pack 中无独立文件
    // 驱动风味：srk=本App原编排(temporary-root-ready/root -c/S25U_KSUD_PATH)；
    // rms24=root-my-s24 移植载荷(retval/socket 判据、硬编码 ksud 路径、--late-load 退出码)
    val driver: String = "srk"
)

package com.samsung.rootkit.data

/**
 * 机型支持判定（2026-09-26；同日修复：字面量匹配 -> 解析式 KMI）。
 *
 * 判定依据是内核 release（System.getProperty("os.version")），
 * 而非 Build.MODEL —— 三星机型的 MODEL（SM-S9xxB）虽可区分系列，但工程机、
 * 刷机改型号等场景下型号串不可靠；内核 release 是内核自身携带的信息，与
 * DFReroot 侧 KsudStage.detectKmi() 的解析逻辑同源。
 *
 * ⚠️ 三星内核 release 是自家格式，KMI 字段不是标准 GKI 的 "androidNN-x.y"：
 *   S23U 实测：5.15.189-android13-8-3251900-abS9180ZCS8FZG1
 *   S24  实测：6.1.145-android14-11-33419968-abS928BXXS6AZH2（One UI 8 / API 36）
 * 即 S24 串里是 "android14-11"，不包含 "android14-6"。
 * 因此旧版 isS24() 的 contains("android14-6") 在三星真机上恒为 false，
 * 主页三处 DFReroot 门禁（自动装 / root 弹窗 / 点卡片）全部失效 —— 已改为解析式。
 *
 *   S23 系列 -> 5.15 内核（解析 KMI: android13-5.15）  DFReroot 支持
 *   S24 系列 -> 6.1  内核（解析 KMI: android14-6.1）   DFReroot **不支持**（无 6.1 ksud 资产）
 *   S25 系列 -> 6.6  内核（解析 KMI: android15-6.6）   支持（统一 ksud，KSU / Next 均可）
 *   S26 系列 -> 6.12 内核（解析 KMI: android16-6.12）  支持（统一 ksud 内嵌 6.12，KSU / Next 均可）
 */
object DeviceSupport {

    /** S24 系列解析出的 KMI 标记（与 DF 侧 DeviceSupport.KMI_S24 一致） */
    const val KMI_S24 = "android14-6.1"

    /** 当前内核 release 串，取不到时为空串。 */
    fun kernelRelease(): String = System.getProperty("os.version") ?: ""

    /**
     * 解析 KMI（与 DF 侧 KsudStage.detectKmi() 完全同源）：
     * 取内核 release 首个 "<主>.<次>" 版本号 + "android" 后的数字，
     * 拼成 "android<NN>-<主>.<次>"。
     *   "6.1.145-android14-11-33419968..." -> "android14-6.1"
     *   "5.15.189-android13-8-3251900..."  -> "android13-5.15"
     * 解析不出返回 null。
     */
    fun detectKmi(): String? {
        val rel = kernelRelease()
        if (rel.isEmpty()) return null
        val v = Regex("""\b(\d+)\.(\d+)\b""").find(rel) ?: return null
        val a = rel.indexOf("android")
        if (a < 0) return null
        val digits = rel.substring(a + 7).takeWhile { it.isDigit() }
        if (digits.isEmpty()) return null
        return "android$digits-${v.groupValues[1]}.${v.groupValues[2]}"
    }

    /**
     * S24 全系列（6.1 内核）。
     * 主判定：解析 KMI == android14-6.1；
     * 兜底：解析失败的极端情况下按「6.1 内核 + android14 标记」判（覆盖三星
     * 未来调整 release 格式导致的解析失效），宁可误拦不漏放。
     */
    fun isS24(): Boolean {
        val rel = kernelRelease()
        if (detectKmi() == KMI_S24) return true
        return rel.startsWith("6.1.") && rel.contains("android14")
    }

    /** dfe（DFReroot / `com.polygraphene.df.reroot`）持久化在当前设备上是否可用。 */
    fun dfSupported(): Boolean = !isS24()

    /**
     * df（DFRoot / `df.root`，DirtyFrag 主动 root 引擎）在当前设备上是否可用。
     *
     * <p>⚠️ **这是 df 自己的判据，不是 [dfSupported] 的别名** —— 两者判的是不同的东西：
     * <ul>
     *   <li>[dfSupported]（dfe）看的是 **ksud 资产覆盖**（无 6.1 ksud 资产）；</li>
     *   <li>本函数（df）看的是 **DirtyFrag 漏洞利用面** —— CVE-2026-43284 在目标内核
     *       是否可利用 + df 包内预编译 `dirtyfrag.ko` 是否覆盖该内核。</li>
     * </ul>
     *
     * <p><b>当前实现 = `!isS24()`</b>，与 dfe 相同 —— 这是**拍板④**（2026-10-04：
     * android14-6.1 预检直判「不支持」→回退向导）的沿用，**不是**两者技术上等价。
     * 沿用的理由：S24/android14-6.1 真机实测 df root 不成功，负面判据只有这一条有实证。
     *
     * <p><b>未来扩展点（改这里，别动 [dfSupported]）</b>：
     * <ul>
     *   <li>S24 大版本升级换内核（6.1→6.6）→ `isS24()` 自动判 false，**无需改本函数**；</li>
     *   <li>6.1 内核**补丁级更新** + DFRoot 上游 payload 覆盖了该内核 → KMI 不变，
     *       `isS24()` 仍拦 ⇒ **必须改本函数放行**，且须先逐内核版本实测可利用性
     *       （漏洞利用与补丁级别相关，不能按 KMI 粒度自动放行）；</li>
     *   <li>届时若 dfe 仍不支持 6.1，两函数各自独立、互不牵连。</li>
     * </ul>
     */
    fun dfRootSupported(): Boolean = !isS24()
}

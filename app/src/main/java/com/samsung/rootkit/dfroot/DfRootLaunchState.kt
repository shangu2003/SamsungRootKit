package com.samsung.rootkit.dfroot

import com.samsung.rootkit.data.DeviceSupport
import com.samsung.rootkit.data.PayloadManager

/**
 * S8 —— df（DFRoot / `df.root`）的**启动判定**与出口选择。
 *
 * <p>方案原文（`DFRoot整合_A路线_内嵌关联具体方案_20261004.md` §S8）：
 * <pre>
 *   读序：私有「不支持」记录 → 系统区「支持」记录 → /dev/df* + 内核版本码（32601/33294）
 *   出口：回退向导 / DFRoot 已 root / 首次待命
 * </pre>
 *
 * <p><b>P4（round15）—— S7 记录已接入</b>：读序前三级的完整语义见
 * [DfRootLaunchResolver.resolve] 的 KDoc。要点：
 * <ol>
 *   <li>**事实态最优先**（`/dev/df` → 内核版本码）—— 与方案原文读序（记录在前）的
 *       **偏差及理由**见 resolve 注释；</li>
 *   <li>失败记录（固件已校验、**且已连续失败到阈值**）第二 —— 实测压过静态预判：
 *       当前固件上 df 连续失败 ≥ [DfRootFailureRecord.FAIL_THRESHOLD] 次 ⇒
 *       [DfRootLaunchState.UNSUPPORTED]（出口回退向导），**且设置页总开关会被拦下**；</li>
 *   <li>静态机型判据 [DeviceSupport.dfRootSupported]（= `!isS24()`，拍板④）**保持不变**、
 *       沦为兜底 —— 与记录互不混合（2026-10-04 用户定调）。</li>
 * </ol>
 *
 * <p><b>2026-10-05 语义收窄</b>：记录从「支持 / 不支持双态」收敛为
 * [DfRootFailureRecord]「只记失败」—— 成功的设备靠 `/dev/df` 与内核版本码即可说明，
 * 不再需要档案；见该类 KDoc。
 *
 * <p><b>为什么必须用「内核自报版本码」而不是任何用户态记录</b>：见 [com.samsung.rootkit.exploit.RootExecutor]
 * 的真机记录 —— `/data/system/df-boot-ok` 第三字段写的是「请求的变体」而非「内核实际跑的变体」，
 * 用户态记录会撒谎，只有内核自报的版本码不会。（S7 记录不参与「变体」判定，只参与
 * 「支不支持」判定，故不与此冲突。）
 */
enum class DfRootLaunchState {
    /**
     * 出口①：回退向导 —— 本机型不提供 df 入口。
     *
     * <p>两个来源：静态机型判据（= S24，拍板④），或**当前固件上 df 已连续失败到阈值**
     * （[DfRootFailureRecord]，2026-10-08 起为连续 3 次；系统升级后自动解除）。
     * 只失败过 1、2 次**不**落到本态 —— 阈值做在 [DfRootFailureRecordStore.read] 里。
     */
    UNSUPPORTED,

    /**
     * 出口②：DFRoot 已 root —— 本次开机 df 已注入（`/dev/df` 存在）。
     *
     * <p>`/dev/df` 由 df 的 shellcode 在 **init（uid 0 / tid 1）** 里以 `O_CREAT|O_EXCL`
     * 创建（见 df `jni/libcxx.S` 的 `mutex_file`），创建点在 insmod 之前、hook 成功之后；
     * 位于 tmpfs 的 `/dev` 上，重启即消失 ⇒ 是「本次开机已注入」的可靠标记。
     */
    DF_ROOTED,

    /**
     * 出口③：已 root，但**不是本次 df 注入的** —— 内核已跑着某一支 KernelSU
     * （自报版本码 32601=主线 / 33294=Next），来源可能是 SRK payload 向导、dfe 或 df，
     * 内核不会自报来源，故单列为一种状态。
     */
    KSU_ROOTED,

    /** 出口④：首次待命 —— 无 root 迹象，可触发 df，也可继续走回退向导。 */
    IDLE,

    ;

    /** 是否「已经 root」（DF_ROOTED 与 KSU_ROOTED 都算）。 */
    val isRooted: Boolean get() = this == DF_ROOTED || this == KSU_ROOTED

    /** 是否允许触发 df 跑链（未 root 且机型支持）。 */
    val canTriggerDf: Boolean get() = this == IDLE
}

object DfRootLaunchResolver {

    /**
     * 判定当前启动态（S8 完整读序，S7 记录已接入）。
     *
     * <p><b>读序与方案原文的偏差（有意为之，务必知悉）</b>：方案 §S8 原文是
     * `私有「不支持」记录 → 系统区「支持」记录 → /dev/df* + 内核版本码`，若机械执行，
     * 「记录不支持」会压过「`/dev/df` 存在」—— 但后者是**当前事实态**：设想
     * 上次跑链失败写入「不支持」→ 用户重启 → df 自启重放**成功** → `/dev/df` 建立，
     * 此时把已 root 的用户推向回退向导是错的。故实现取
     * **事实态 > 记录态 > 静态预判**：
     * <ol>
     *   <li>`/dev/df` 存在（本次开机已注入）→ [DfRootLaunchState.DF_ROOTED]；</li>
     *   <li>内核自报版本码命中 → [DfRootLaunchState.KSU_ROOTED]；</li>
     *   <li>失败记录（固件一致）→ [DfRootLaunchState.UNSUPPORTED]
     *       （出口回退向导；实测明确失败，防重复失败挫败感）；</li>
     *   <li>系统区「支持」记录 → [DfRootLaunchState.IDLE]（成功过 ⇒ 给入口）；</li>
     *   <li>静态机型判据兜底 → IDLE / UNSUPPORTED（拍板④ `!isS24()`，保持不变）。</li>
     * </ol>
     * 方案原文「私有不支持连 `/dev/df` 都不必看」的意图（防重复失败）通过第 3 级
     * 依然完整保留 —— 只是让位给「已经 root」这一更硬的事实。
     *
     * @param deviceSupported 机型静态预判（[DeviceSupport.dfRootSupported]）；
     *                        由调用方传入而非本类直接调用，是为了让 UI 层可以复用父级
     *                        已 remember 的结果（内核串解析不必每次重组重跑）。
     * @param armedThisBoot   `/dev/df` 是否存在（[DfRootBridge.isArmedThisBoot]）
     * @param kernelVariant   内核自报版本码判出的当前生效变体；null = 内核里没有 KSU
     * @param failureRecord   df 失败记录（**须已过固件校验** —— [DfRootFailureRecordStore.read]
     *                        返回非 null 即已校验）；null = 没失败过或固件已变视同首次
     * @param systemZoneSupported 系统区「支持」记录（df 链内代写，拍板②；
     *                        写入方未落地前恒 false）
     */
    fun resolve(
        deviceSupported: Boolean,
        armedThisBoot: Boolean,
        kernelVariant: PayloadManager.KsuVariant?,
        failureRecord: DfRootFailureRecord? = null,
        systemZoneSupported: Boolean = false,
    ): DfRootLaunchState = when {
        armedThisBoot -> DfRootLaunchState.DF_ROOTED
        kernelVariant != null -> DfRootLaunchState.KSU_ROOTED
        failureRecord != null -> DfRootLaunchState.UNSUPPORTED
        systemZoneSupported -> DfRootLaunchState.IDLE
        deviceSupported -> DfRootLaunchState.IDLE
        else -> DfRootLaunchState.UNSUPPORTED
    }

    /**
     * 一次算完证据并判定（IO 线程调用）。
     *
     * <p>[variantProbe] 会跑 `<ksud> debug version`，涉及进程启动（最坏 ~4 s），
     * 必须在 IO 线程调用；UI 层应把结果 remember 住，不要每次重组都跑。
     *
     * <p>注意：本便捷入口**不带记录**（适用于无 Context 的纯探测场景）；
     * UI 主流程请直接调 [resolve] 并传入 [DfRootFailureRecordStore.read] 的结果。
     */
    fun resolveNow(
        variantProbe: () -> PayloadManager.KsuVariant?,
        deviceSupported: Boolean = DeviceSupport.dfRootSupported(),
        armedThisBoot: Boolean = DfRootBridge.isArmedThisBoot(),
    ): DfRootLaunchState = resolve(
        deviceSupported = deviceSupported,
        armedThisBoot = armedThisBoot,
        kernelVariant = try {
            variantProbe()
        } catch (_: Throwable) {
            null
        },
    )
}

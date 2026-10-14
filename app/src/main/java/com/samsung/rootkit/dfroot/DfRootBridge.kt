package com.samsung.rootkit.dfroot

import java.io.File

/**
 * df（DFRoot）—— 现已是**本应用内嵌的能力**，不再是独立 App。
 *
 * <h2>为什么这层还在（历史与现状）</h2>
 * 2026-10-04 的「A 路线」把 df 当作**独立 APK**（`df.root`）内嵌关联：链在 df 自己的进程里跑，
 * SRK 只能 `startActivity(TriggerActivity)` 跨 App 触发、再等广播/兜底文件收结果；自启是
 * df 自己的 `BootReceiver` 组件态；安装要 `pm install`。
 *
 * <p>2026-10-05（round24）改成**真单包**：df 的漏洞引擎（`libexp.so` + 7 版 `dirtyfrag-*.ko`）
 * 与执行层（`df.root.ExploitRunner`）整体编进 SRK，于是：
 * <ul>
 *   <li>触发退化为**进程内函数调用**（[df.root.DfChain]）⇒ 无跨 App 通道、无广播、无兜底文件；</li>
 *   <li>自启 = 本应用自己的 `DfBootReceiver` 组件态 + 本应用自己的 DE prefs；</li>
 *   <li>没有独立包 ⇒ 无安装 / 无卸载 / 无包可见性 `<queries>` 声明。</li>
 * </ul>
 *
 * <p>于是本类只剩两类东西：**结果码**与**内核互斥标记**——两者都与「链」本身同寿，
 * 不随集成方式改变。其余常量（包名 / TriggerActivity / 广播 / 安装资产）已随单包整合删除；
 * 若要做历史对照，见 `_audit/round23_end/` 前一轮的版本。
 *
 * <p>⚠️ 本文件里**不得**再出现跨 App 常量。加常量前先问：它是不是只在「df 是独立 App」的
 * 前提下才有意义？若是，说明你正在把已经删掉的耦合加回来。
 */
object DfRootBridge {

    // ─────────────────────────── 结果码（与 df jni 链的语义一一对应，不可改） ───────────────────────────

    /** 成功获取 Root（内核侧建立 `/dev/df`）。 */
    const val RC_OK = 0

    /** ksud 执行出错退出。 */
    const val RC_KSUD_ERROR = 1

    /** 执行超时。 */
    const val RC_TIMEOUT = 2

    /** 文件补丁失败 / 链内异常。 */
    const val RC_PATCH_FAIL = 3

    /**
     * 本次开机已注入（`/dev/df` 已存在）。
     *
     * <p>不是「成功」也不是「失败」——是「无需重复」。
     */
    const val RC_ALREADY_ARMED = 9

    // ─────────────────────────── 内核互斥标记 ───────────────────────────

    /**
     * `/dev/df` —— 由 df 的 shellcode 在 init（uid 0 / tid 1）里以 `O_CREAT|O_EXCL` 创建，
     * 位于 tmpfs 的 `/dev` 上，**重启即清、且从不删除**。
     *
     * <p>因此它同时承担两个语义：
     * <ol>
     *   <li>「本次开机已注入」的**事实标记**（[isArmedThisBoot]）；</li>
     *   <li>「同一开机内不可能重复注入」的**互斥锁**（第二次跑链必然拿不到 O_EXCL）。</li>
     * </ol>
     */
    const val DEV_DF_MUTEX = "/dev/df"

    /**
     * 本次开机是否已注入（零副作用、零权限 —— 只 stat 一个 tmpfs 节点）。
     *
     * <p>与 [df.root.DfChain.isArmedThisBoot] 是同一判据的两个入口：本方法供 UI/状态层用，
     * 后者供原生侧执行层用；之所以不去掉其中一个，是因为 UI 层不应依赖 `df.root` 包
     * （那是被 R8 keep 住、语义上属于「引擎」的包）。
     */
    fun isArmedThisBoot(): Boolean = try {
        File(DEV_DF_MUTEX).exists()
    } catch (_: Throwable) {
        // File.exists() 在极端沙箱配置下可能抛 SecurityException；此时按「未注入」处理，
        // 让链自己去撞 O_EXCL —— 误判为「已注入」会直接吞掉一次真实可用的 root 机会。
        false
    }
}

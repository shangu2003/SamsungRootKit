package com.samsung.rootkit.dfroot

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log

/**
 * df（DFRoot，已内嵌）的**持久化状态**——全部落在本应用自己的
 * <b>设备保护存储（DE / direct-boot storage）</b> 的 SharedPreferences `dfroot` 里。
 *
 * <h2>为什么必须放 DE 而不是普通 storage</h2>
 * 开机自启要在 [android.content.Intent.ACTION_LOCKED_BOOT_COMPLETED] 就开跑 ——
 * 那一刻用户可能**尚未解锁**，普通（CE）存储不可读。DE 存储在 Direct Boot 之前即可读，
 * 这正是 [DfBootReceiver] 能工作的前提。若把状态放 CE：
 * <ul>
 *   <li>在 FBE 设备上，LOCKED_BOOT_COMPLETED 时读到的是**空的** SharedPreferences
 *       ⇒ 所有开关看起来都是「默认值」（自启关、变体错），链路静默走错分支；</li>
 *   <li>更糟的是它**不报错**：只有真的锁屏重启过一次才会暴露。</li>
 * </ul>
 *
 * <h2>键名故意与 df 原实现逐字相同</h2>
 * `auto_soft_reboot` / `ksu_next` 这两个键在原 df（独立包）里就存在，语义一字不差。
 * 保持同名有两个好处：① 迁移到单包后语义可直接对照旧版排查；
 * ② 万一有历史文档/脚本按这些键名描述行为，仍然对得上。
 * 新增的 `enabled` / `autostart` 是单包整合引入的（原版没有「总开关」，
 * 且「自启」原本直接由 **组件态** 承载，见 [syncBootReceiverComponent]）。
 *
 * <h2>「自启」到底存哪：prefs 还是组件态？</h2>
 * 原 df 实现里「自启」= `BootReceiver` 的**组件启用态**本身，没有第二份记录。
 * 单包后多了一个「总开关」，两者必须叠加（总开关关 ⇒ 自启无论如何都不该跑），
 * 若继续让组件态兼任唯一事实源，就出现一个无法表达的状态：
 * 「总开关关、但用户想把自启留着，下次开总开关时恢复」。
 * <br>故本实现取 **prefs 为唯一事实源、组件态为派生值**：
 * <pre>
 *   componentEnabled == (enabled && autostart)
 * </pre>
 * 每次任一开关变化后调 [syncBootReceiverComponent] 重新派生。这样：
 * <ul>
 *   <li>总开关关 → 组件被停用 → 系统根本不会投递开机广播（比「投递后自己忽略」更省电、
 *       也避免 receiver 在锁屏期被唤醒）；</li>
 *   <li>总开关再开 → 组件恢复到用户此前选的自启值，不会「丢了」。</li>
 * </ul>
 */
object DfRootPrefs {

    private const val TAG = "DFROOT"

    /** prefs 文件名（与 df 原实现同名）。 */
    const val NAME = "dfroot"

    /** 总开关：是否启用 DFRoot（关 ⇒ 主页按钮不再走 df 路，自启同时停用）。 */
    const val KEY_ENABLED = "enabled"

    /** 开机自启：开机后是否自动跑一次链。 */
    const val KEY_AUTOSTART = "autostart"

    /** 自动软重启：跑链成功后是否自动软重启一次（Next 路必需）。键名沿用 df 原实现。 */
    const val KEY_AUTO_SOFT_REBOOT = "auto_soft_reboot"

    /** 自启 / 跑链所用的 KSU 变体：true = KernelSU-Next，false = 官方 KernelSU。键名沿用 df 原实现。 */
    const val KEY_KSU_NEXT = "ksu_next"

    /**
     * **本次开机 df 链实际注入成功**时所用的变体（true = KSU-Next）。
     *
     * <h2>为什么需要它（2026-10-07 用户报障，df 链主页红/蓝按钮）</h2>
     * df 链模式下「当前工作的是哪一支」**没有任何可靠来源** ——
     * [com.samsung.rootkit.exploit.RootExecutor.resolveWorkingVariant] 的四级判据在这种情形下**全部落空**：
     * <ul>
     *   <li>内核自报版本码：需 su / Shizuku，而 df 链的 late-load **不注册 sucompat**、
     *       本应用不在 allowlist、`/data/adb` 对普通身份 0700 不可达（见 DfManagerInstaller 注释）⇒ 拿不到；</li>
     *   <li>`/data/system/df-boot-ok` 与 `df-boot.log`：都是 **DFReroot（dfe）** 的产物，纯 df 链从不写；</li>
     *   <li>「已安装唯一性」：两支管理器都装时无区分力 ⇒ null。</li>
     * </ul>
     * ⇒ `resolveWorkingVariant()` 恒为 null ⇒ 主页红/蓝按钮的「切换下次开机管理器」确认窗**永不弹出**
     * （该分支要求 `current != null`）；已 root 时点异变体按钮只会落到「当前系统已 Root，请勿重复操作」。
     *
     * <p>修法：df 链是**本进程内、由本应用驱动**的，它自己就知道用了哪一支 —— 于是在真正注入成功
     * （rc = RC_OK）时把该变体落盘，作为「本次开机工作变体」的可靠来源。
     *
     * <p>**有效性门控**：[com.samsung.rootkit.dfroot.DfRootManager.armedVariantThisBoot] 只在
     * `isArmedThisBoot()`（`/dev/df` 存在）为真时才采信本键 —— 冷重启后 `/dev/df` 消失即自动作废，
     * 无需显式清除。
     */
    const val KEY_ARMED_VARIANT = "armed_variant"

    // ── round35：「待补装管理器」标记（round30）已整段删除 ──────────────────────
    //   原设计是「跑链**后**补装」：链会软重启杀进程 ⇒ 落持久化标记 ⇒ 下次启动接力。
    //   用户 2026-10-06 改走**方案 B**：安装提到跑链**之前**，走 ③ PackageInstaller
    //   （前台、一次系统确认）⇒ 装不装得成当场就知道，不再需要跨进程记忆。
    //   （旧键 manager_pending_* 即使残留在老用户磁盘上也无人读取，无需迁移。）

    // ─────────────────────────── 存取 ───────────────────────────

    /** DE（设备保护）视角的 prefs —— **所有读写都走这里**，不要用 CE context。 */
    fun de(ctx: Context): SharedPreferences =
        ctx.createDeviceProtectedStorageContext().getSharedPreferences(NAME, Context.MODE_PRIVATE)

    private fun putBoolean(ctx: Context, key: String, value: Boolean): Boolean = try {
        de(ctx).edit().putBoolean(key, value).commit()
    } catch (t: Throwable) {
        Log.e(TAG, "putBoolean($key) failed", t)
        false
    }

    // 总开关 ------------------------------------------------------------------

    /** 总开关是否打开；**默认 true**（内嵌即具备能力，用户显式关才关）。 */
    fun isEnabled(ctx: Context): Boolean = readBool(ctx, KEY_ENABLED, default = true)

    /** 写总开关，并重新派生组件态。@return 是否写入成功 */
    fun setEnabled(ctx: Context, enable: Boolean): Boolean {
        val ok = putBoolean(ctx, KEY_ENABLED, enable)
        syncBootReceiverComponent(ctx)
        return ok
    }

    // 自启 --------------------------------------------------------------------

    /** 自启是否打开（**读 prefs，不读组件态** —— 理由见类注释）。默认 false。 */
    fun isAutostart(ctx: Context): Boolean = readBool(ctx, KEY_AUTOSTART, default = false)

    /** 写自启，并重新派生组件态。@return 是否写入成功 */
    fun setAutostart(ctx: Context, enable: Boolean): Boolean {
        val ok = putBoolean(ctx, KEY_AUTOSTART, enable)
        syncBootReceiverComponent(ctx)
        return ok
    }

    // 自动软重启 --------------------------------------------------------------

    /** 自动软重启是否打开；**默认 true**（与 df 原实现 `prefs.getBoolean("auto_soft_reboot", true)` 一致）。 */
    fun isAutoSoftReboot(ctx: Context): Boolean = readBool(ctx, KEY_AUTO_SOFT_REBOOT, default = true)

    /** 写自动软重启。@return 是否写入成功 */
    fun setAutoSoftReboot(ctx: Context, enable: Boolean): Boolean =
        putBoolean(ctx, KEY_AUTO_SOFT_REBOOT, enable)

    // 变体 --------------------------------------------------------------------

    /** 自启 / 跑链所用的变体；true = KSU-Next。默认 false（官方 KSU）。 */
    fun isKsuNext(ctx: Context): Boolean = readBool(ctx, KEY_KSU_NEXT, default = false)

    /** 写变体。@return 是否写入成功 */
    fun setKsuNext(ctx: Context, ksuNext: Boolean): Boolean = putBoolean(ctx, KEY_KSU_NEXT, ksuNext)

    // 「本次开机实际注入所用变体」 ------------------------------------------------

    /**
     * 记下**本次开机实际注入成功**所用的变体（**仅 `rc == RC_OK` 时调用**）。
     *
     * <p>⚠️ 只记「真的注入了」那一次：`RC_ALREADY_ARMED`（本次开机早已注入）**不得**调用 ——
     * 那会用「用户这次点的变体」覆盖「本次真正注入的变体」，与 dfe 侧
     * 「df-boot-ok 写的是请求的变体、不是生效的变体」的旧坑同源（见 RootExecutor 注释）。
     *
     * <p>同步 commit（非 apply）：链成功后可能**主动软重启**并杀掉本进程，异步写来不及。
     * 与 [markTmpCleanArmed] / [com.samsung.rootkit.data.SettingsStore.markRootedThisBoot] 同理。
     *
     * @return 是否写入成功
     */
    fun setArmedVariant(ctx: Context, ksuNext: Boolean): Boolean =
        putBoolean(ctx, KEY_ARMED_VARIANT, ksuNext)

    /** [KEY_ARMED_VARIANT] 的值；**键不存在 ⇒ null**（不猜，交调用方决定兜底）。 */
    fun armedVariantNextOrNull(ctx: Context): Boolean? = try {
        if (de(ctx).contains(KEY_ARMED_VARIANT)) de(ctx).getBoolean(KEY_ARMED_VARIANT, false) else null
    } catch (t: Throwable) {
        Log.w(TAG, "read $KEY_ARMED_VARIANT failed", t)
        null
    }

    // 待清理 /data/local/tmp ---------------------------------------------------

    /** 「待清理 /data/local/tmp」标记（round34 需求②）。 */
    const val KEY_TMP_CLEAN_ARMED = "tmp_clean_armed"

    /** 落标记的时刻（排障用）。 */
    const val KEY_TMP_CLEAN_AT = "tmp_clean_at"

    /**
     * 落一条「待清理 /data/local/tmp」标记。
     *
     * <p>用户原话（2026-10-06）：「DF链路提权成功也需要清理tmp下缓存文件，
     * 如果打开自动软重启，则下次打开srk时自动清理tmp下缓存文件
     * （自动软重启后肯定已经是root状态）」。
     *
     * <p>即：链成功、**且**开着自动软重启 ⇒ 本进程随即被软重启杀掉，清理动作只能由
     * **下次打开 SRK** 接力；而上一次之所以能做，是因为那时系统已经 root。
     * 与 round30 的待补装标记一样，必须**同步落盘**——写它的时刻紧邻那条会软重启的链。
     *
     * <p>链内已有一半（ksud_wrap.sh / ksud_next_wrap.sh 的「1a 段」，UMH 的 uid0 +
     * permissive 窗口，零外部依赖）；本标记对应的是**应用侧**那一半，两处都不是可选项。
     *
     * @return 是否写入成功
     */
    fun markTmpCleanArmed(ctx: Context): Boolean = try {
        de(ctx).edit()
            .putBoolean(KEY_TMP_CLEAN_ARMED, true)
            .putLong(KEY_TMP_CLEAN_AT, System.currentTimeMillis())
            .commit()
    } catch (t: Throwable) {
        Log.e(TAG, "markTmpCleanArmed failed", t)
        false
    }

    /** 是否存在待清理标记。 */
    fun isTmpCleanArmed(ctx: Context): Boolean = readBool(ctx, KEY_TMP_CLEAN_ARMED, default = false)

    /** 清掉待清理标记（清过了 / 判定无需再清时调用）。 */
    fun clearTmpCleanArmed(ctx: Context): Boolean = try {
        de(ctx).edit()
            .remove(KEY_TMP_CLEAN_ARMED)
            .remove(KEY_TMP_CLEAN_AT)
            .commit()
    } catch (t: Throwable) {
        Log.w(TAG, "clearTmpCleanArmed failed", t)
        false
    }

    // ─────────────────────────── 组件态派生 ───────────────────────────

    /**
     * 把 [DfBootReceiver] 的组件启用态重新派生为 `enabled && autostart`。
     *
     * <p>**为什么是 DONT_KILL_APP**：切组件态默认会杀掉本进程，而调用点常常就在本进程的
     * UI 线程上（用户在设置页拨开关）。加上该 flag 后只改 package manager 里的状态，
     * 不重启进程 —— 对本用途完全够用（receiver 是下次开机才投递的）。
     *
     * <p>失败只记日志不抛：组件态是**派生值**，prefs 才是事实源；某次同步没成功
     * 不影响状态正确性，下次任一开关变化都会重试。
     */
    fun syncBootReceiverComponent(ctx: Context) {
        val want = isEnabled(ctx) && isAutostart(ctx)
        try {
            // 幂等短路：状态未变就不写 PackageManager。
            // 本方法会被 UI 的每次 onResume 顺带调用（自愈：万一上次同步失败），
            // 不短路的话每次页面可见都要走一次 binder 写 —— 没必要。
            if (isBootReceiverEnabled(ctx) == want) return
            ctx.packageManager.setComponentEnabledSetting(
                bootReceiverComponent(ctx),
                if (want) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        } catch (t: Throwable) {
            Log.w(TAG, "syncBootReceiverComponent(want=$want) failed", t)
        }
    }

    /**
     * 组件态**实际**是否启用（排障用）。
     *
     * <p>注意语义陷阱：manifest 里声明的是 `android:enabled="false"`，此时
     * [PackageManager.getComponentEnabledSetting] 返回
     * [PackageManager.COMPONENT_ENABLED_STATE_DEFAULT]（**不是 DISABLED**）。
     * 判定「启用」必须用 `== ENABLED`（我们从不主动写 DEFAULT），不要写成 `!= DISABLED`。
     */
    fun isBootReceiverEnabled(ctx: Context): Boolean = try {
        ctx.packageManager.getComponentEnabledSetting(bootReceiverComponent(ctx)) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } catch (_: Throwable) {
        false
    }

    /**
     * 本应用自己的开机接收器组件（改自己的组件零权限、零 su）。
     *
     * <p>⚠️ **必须**用 `ComponentName(ctx, Class)` 而不是拼字符串：
     * 本工程 `applicationId = "com.samsung.root.kit"` 而 `namespace = "com.samsung.rootkit"`
     * （见 app/build.gradle.kts），二者**不同**。类实际在 `com.samsung.rootkit.dfroot.DfBootReceiver`，
     * 若拼 `"${ctx.packageName}.dfroot.DfBootReceiver"` 会得到
     * `com.samsung.root.kit.dfroot.DfBootReceiver` —— 一个不存在的组件，
     * 而 [PackageManager.setComponentEnabledSetting] 遇到未知组件**不抛异常**，
     * 只静默失败 ⇒ 自启开关看起来能拨、实际永不生效。
     */
    fun bootReceiverComponent(ctx: Context): ComponentName =
        ComponentName(ctx, DfBootReceiver::class.java)

    // ─────────────────────────── 内部 ───────────────────────────

    private fun readBool(ctx: Context, key: String, default: Boolean): Boolean = try {
        de(ctx).getBoolean(key, default)
    } catch (t: Throwable) {
        Log.w(TAG, "read $key failed, fallback to $default", t)
        default
    }
}

package com.samsung.rootkit.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.samsung.rootkit.ui.theme.AppThemeMode

object SettingsStore {
    private const val NAME = "rootkit_settings"
    private const val KEY_THEME = "theme_mode"      // 0 system, 1 light, 2 dark
    private const val KEY_DYNAMIC = "dynamic_color" // bool
    private const val KEY_LANG = "language"         // "system","zh","en"
    private const val KEY_DISCLAIMER_VERSION = "disclaimer_version"
    private const val CURRENT_DISCLAIMER_VERSION = 1

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getThemeMode(context: Context): AppThemeMode {
        return when (prefs(context).getInt(KEY_THEME, 0)) {
            1 -> AppThemeMode.Light
            2 -> AppThemeMode.Dark
            else -> AppThemeMode.System
        }
    }

    fun setThemeMode(context: Context, mode: AppThemeMode) {
        val v = when (mode) {
            AppThemeMode.Light -> 1
            AppThemeMode.Dark -> 2
            else -> 0
        }
        prefs(context).edit { putInt(KEY_THEME, v) }
    }

    fun getDynamicColor(context: Context): Boolean = prefs(context).getBoolean(KEY_DYNAMIC, true)

    fun setDynamicColor(context: Context, on: Boolean) {
        prefs(context).edit { putBoolean(KEY_DYNAMIC, on) }
    }

    fun getLanguage(context: Context): String = prefs(context).getString(KEY_LANG, "system") ?: "system"

    fun setLanguage(context: Context, lang: String) {
        prefs(context).edit { putString(KEY_LANG, lang) }
    }

    fun hasAcceptedCurrentDisclaimer(context: Context): Boolean =
        prefs(context).getInt(KEY_DISCLAIMER_VERSION, 0) >= CURRENT_DISCLAIMER_VERSION

    fun acceptCurrentDisclaimer(context: Context) {
        prefs(context).edit { putInt(KEY_DISCLAIMER_VERSION, CURRENT_DISCLAIMER_VERSION) }
    }

    // ── boot 作用域的“本次开机已提权”标记（内存态 root：冷重启即失效）──
    //
    // 语义 = 「**同一次开机**内成功提权过」。KSU LKM 是内存态：冷重启即失效；
    // 而软重启（`kill $(pidof system_server)`）只重启用户态、内核未重启，LKM 仍在 ⇒ 必须判「是」。
    // 因此判据的唯一正确形态是：**比对「哪一次开机」的权威标识是否全等**，
    // 而不是任何「当前已开机多久」的单调性推断（后者在冷重启后必然失效，见 isRootedThisBoot）。
    private const val KEY_ROOTED_BOOT = "rooted_boot_id"       // 权威：内核 boot_id 原文
    private const val KEY_ROOTED_BOOTWALL = "rooted_boot_wall"  // 兜底：本次开机挂钟起点
    private const val KEY_ROOTED_ELAPSED_LEGACY = "rooted_elapsed_ms" // 旧版遗留键，仅用于自愈清理

    private fun currentBootId(): String? = try {
        java.io.File("/proc/sys/kernel/random/boot_id").readText().trim().ifBlank { null }
    } catch (_: Throwable) { null }

    /**
     * 本次开机的**挂钟起点** = `当前挂钟 - 开机至今(含休眠)`，同一次开机内为常量：
     *   · 冷重启：内核计时归零、挂钟前进 ⇒ 值必变；
     *   · 软重启（kill system_server）：内核计时与挂钟都不重置 ⇒ 值不变。
     * 只用两个 App 可直读的时钟，零系统文件 / 零权限依赖，
     * 用于给「untrusted_app 读不到 `/proc/sys/kernel/random/boot_id`」的固件兜底。
     */
    private fun currentBootStartWall(): Long =
        System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()

    /**
     * 提权成功后调用：记录「本次开机已提权」。
     *
     * <p>同时写两条**同为「本次开机标识」**的证据（见 [isRootedThisBoot]）：内核 boot_id +
     * 开机挂钟起点 —— 后者零系统文件依赖，兜住读不到 boot_id 的固件。
     *
     * <p>⚠️ **必须同步落盘（commit）**：调用点（df 链成功、传统 Root 向导成功）紧接着
     * 就可能软重启（`kill $(pidof system_server)`）把本进程杀掉，异步 apply 极可能来不及
     * 写盘 —— 结果就是「明明提权成功，回来后主页仍显示未ROOT」。一次同步写盘的开销可忽略。
     */
    fun markRootedThisBoot(context: Context) {
        prefs(context).edit(commit = true) {
            currentBootId()?.let { putString(KEY_ROOTED_BOOT, it) }
            putLong(KEY_ROOTED_BOOTWALL, currentBootStartWall())
        }
    }

    /**
     * 是否「本次开机已提权」（KSU LKM 是内存态，冷重启即失效）。
     *
     * <p><b>判据 = 「哪一次开机」标识全等；不做任何单调性推断</b>：
     * <ol>
     *   <li><b>权威</b>：内核 `boot_id` 与记录全等。boot_id 只随**冷重启**变化、软重启不变，
     *       正是所需语义；某些固件下 untrusted_app 读不到该节点（此时为 null）。</li>
     *   <li><b>兜底</b>：开机挂钟起点全等（[currentBootStartWall]）。boot_id 读不到、
     *       或记录里没有 boot_id 时使用；同样只随冷重启变化。</li>
     * </ol>
     * 两者都不成立 ⇒ 判否（**绝不因为「当前 uptime 已超过历史记录值」而误判为已提权**）。
     *
     * <p>⚠️ <b>历史缺陷（已修，2026-10-07）</b>：旧实现把兜底写成
     * `记录值 ≤ elapsedRealtime()` 的**单调性**判据，并声称「冷重启后计时归零 ⇒ 判否」。
     * 该推断不成立 —— 冷重启后 uptime 从 0 重新增长，**一旦新开机时长超过旧记录值**，
     * `记录值 ≤ 当前值` 便重新成立 ⇒ 未 root 设备被误判成「本次已提权」，
     * 主页状态卡因此把「未 root + 管理器残留」显示成「KernelSU 已加载 / 当前系统已ROOT」。
     * （真机复现：SM-S9180 dm3q，冷重启约 60 分钟后误报。）现已收敛为标识全等。
     *
     * <p>另：判否时**自愈清除**陈旧标记 —— 旧实现全项目没有任何清除点，
     * 一旦误真就长期为真（直到下一次真正提权覆盖它）。
     */
    fun isRootedThisBoot(context: Context): Boolean {
        val p = prefs(context)
        val curId = currentBootId()
        val storedId = p.getString(KEY_ROOTED_BOOT, null)
        val match = if (curId != null && storedId != null) {
            // 双方都可读 ⇒ 以 boot_id 为准（唯一权威），不再参考挂钟兜底。
            storedId == curId
        } else {
            val storedWall = p.getLong(KEY_ROOTED_BOOTWALL, Long.MIN_VALUE)
            storedWall != Long.MIN_VALUE && storedWall == currentBootStartWall()
        }
        if (!match && (storedId != null || p.contains(KEY_ROOTED_BOOTWALL) || p.contains(KEY_ROOTED_ELAPSED_LEGACY))) {
            // 自愈：换过一次开机（或记录不再可信）⇒ 清掉陈旧标记，避免后续任何读取再次误判。
            p.edit {
                remove(KEY_ROOTED_BOOT)
                remove(KEY_ROOTED_BOOTWALL)
                remove(KEY_ROOTED_ELAPSED_LEGACY)
            }
        }
        return match
    }

    // ── DFReroot 集成（v5.1.0）：证书已注入、待安装 DFReroot 的跨重启标记 ──
    // 为什么必须持久化：注入成功后要软重启（`kill $(pidof system_server)`），**本应用进程会被
    // 一并杀掉**，内存态全部丢失。软重启回来时若不靠这个标记，就无从知道"证书已注入，
    // 只差安装 APK"这一步 —— 而 PMS 只在启动时读 packages.xml，安装必须发生在软重启之后。
    //
    // 不用 DfRunner.checkInjected() 走 su 复查的理由：那要依赖 KernelSU 授权与 root 可用性，
    // 而这里只需一个布尔量。软重启后 KernelSU LKM 仍在内存（内核未重启）故 su 通常仍可用，
    // 但把"要不要提示安装"建立在这种时序假设上是不必要的耦合。
    private const val KEY_DF_PENDING_INSTALL = "df_pending_install"

    fun setDfPendingInstall(context: Context, on: Boolean) {
        prefs(context).edit { putBoolean(KEY_DF_PENDING_INSTALL, on) }
    }

    fun isDfPendingInstall(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DF_PENDING_INSTALL, false)

    // ── DFReroot（dfe）**总开关**（2026-10-05）──
    // 与 df 的 DFRoot 总开关（DfRootPrefs.KEY_ENABLED）对称，但**存储位置刻意不同**：
    //   · df 存 DE（createDeviceProtectedStorageContext）—— 它的开机自启接收器在锁屏期跑，
    //     CE 未挂载，读 CE 只会拿到默认值且不报错；
    //   · dfe 存 CE —— dfe 侧**没有任何开机期消费方**（DFReroot 是独立 App，自启由它自己负责），
    //     全部读写都发生在 UI 进程（用户解锁后），CE 完全够用。
    //
    // 关闭语义 = 「不再要持久化 root」：主页两个按钮不再走 dfe 分支（回退 Root 向导）、
    // 设置页「配置 DFReroot」卡不渲染、SRK root 成功后不再弹持久化引导。
    // 默认 true：与 df 总开关一致（机型支持时默认走对应路径），避免首次渲染先显示
    // 「未配置」再被刷新纠正的可见闪烁。
    private const val KEY_DF_ENTRY_ENABLED = "df_entry_enabled"

    fun isDfEntryEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DF_ENTRY_ENABLED, true)

    fun setDfEntryEnabled(context: Context, on: Boolean) {
        prefs(context).edit { putBoolean(KEY_DF_ENTRY_ENABLED, on) }
    }

    // ── 最后一次使用的 KSU 变体（2026-10-01）──
    // 首页两个 root 按钮（KSU 红 / KSU-Next 蓝）点击进入向导时记录；
    // 设置页「打开 DFReroot」据此把变体传给 DFReroot，实现「SRK 选哪个、DFReroot 就用哪个」。
    // 0 = KSU，1 = KSU_NEXT。
    private const val KEY_KSU_VARIANT = "ksu_variant"

    fun getKsuVariant(context: Context): Int = prefs(context).getInt(KEY_KSU_VARIANT, 0)

    fun setKsuVariant(context: Context, variant: Int) {
        prefs(context).edit { putInt(KEY_KSU_VARIANT, variant) }
    }
}

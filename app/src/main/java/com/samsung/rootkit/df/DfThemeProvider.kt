package com.samsung.rootkit.df

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process

/**
 * 把 SamsungRootKit **当前生效**的 Material3 配色发布给 DFReroot，实现
 * "SamsungRootKit 改了配色，DFReroot 跟着自动改"。
 *
 * 为什么走 ContentProvider 而不是读文件/SharedPreferences：
 *   DFReroot 与 SamsungRootKit 是两个不同 UID 的应用（前者最终以 uid 1000 运行），
 *   跨应用直接读 `shared_prefs` 既不被 API 允许、也过不了 SELinux。
 *   ContentProvider 是唯一干净且可审计的通道。
 *
 * 权限：provider 声明 `android:readPermission` 为一个 **signature 级** 自定义权限。
 *   两个 APK 用同一把密钥签名（`srk-release.jks`），所以权限自动授予；
 *   其它应用即使知道 authority 也读不到。
 *
 * 快照由 [com.samsung.rootkit.ui.theme.RootKitTheme] 在每次主题参数变化时刷新
 * （取的是 `MaterialTheme.colorScheme` 的实际值，所以无论配色来自
 * 系统动态取色还是 material-kolor 生成，DFReroot 拿到的都是同一套）。
 */
object DfThemeSnapshot {
    /** 由 RootKitTheme 写入；未写入过为 null（DFReroot 侧据此使用自带回退配色）。 */
    @Volatile
    var values: Bundle? = null

    /** 只在真正变化时写，避免每次重组都新建 Bundle。 */
    fun publish(newValues: Bundle) {
        val cur = values
        if (cur != null && cur.getInt(DfThemeColumns.DARK) == newValues.getInt(DfThemeColumns.DARK)) {
            var same = true
            for (k in DfThemeColumns.ALL) {
                if (cur.getInt(k, Int.MIN_VALUE) != newValues.getInt(k, Int.MIN_VALUE)) {
                    same = false
                    break
                }
            }
            if (same) return
        }
        values = newValues
    }
}

/** 快照的列定义（DFReroot 侧按同名列读取，名字改动要两边一起改）。 */
object DfThemeColumns {
    const val DARK = "dark"
    const val BACKGROUND = "background"
    const val ON_BACKGROUND = "on_background"
    const val SURFACE = "surface"
    const val ON_SURFACE = "on_surface"
    const val SURFACE_CONTAINER = "surface_container"
    const val ON_SURFACE_VARIANT = "on_surface_variant"
    const val OUTLINE = "outline"
    const val PRIMARY = "primary"
    const val ON_PRIMARY = "on_primary"
    const val PRIMARY_CONTAINER = "primary_container"
    const val ON_PRIMARY_CONTAINER = "on_primary_container"
    const val ERROR = "error"

    val ALL = arrayOf(
        DARK, BACKGROUND, ON_BACKGROUND, SURFACE, ON_SURFACE, SURFACE_CONTAINER,
        ON_SURFACE_VARIANT, OUTLINE, PRIMARY, ON_PRIMARY, PRIMARY_CONTAINER,
        ON_PRIMARY_CONTAINER, ERROR,
    )
}

/**
 * 只读 provider。`query()` 返回 0 或 1 行；0 行表示"SRK 尚未生成快照"，
 * 此时 DFReroot 使用自己内置的回退配色（不会崩）。
 *
 * 访问控制放在**代码里**而不是 manifest 的 signature 级权限：因为 DFReroot 最终以
 * uid 1000（system）运行，而 uid 1000 被平台多个包共享，"按调用包签名"的权限判定在这种
 * 情况下不可靠（可能先被系统权限检查拦掉，根本进不到我们的代码）。
 */
class DfThemeProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    /**
     * 允许两类调用方：
     *  - uid 1000（system）：DFReroot 的最终运行身份；也允许本进程自身；
     *  - 其它 UID：必须与本应用**签名一致**（同一把 srk-release.jks）。
     * 配色不是敏感数据，但没必要对任意应用开放。
     */
    private fun enforceCallerAllowed() {
        val uid = Binder.getCallingUid()
        if (uid == Process.SYSTEM_UID || uid == Process.myUid()) return
        val ctx = context ?: throw SecurityException("provider has no context")
        val pm = ctx.packageManager
        for (pkg in pm.getPackagesForUid(uid).orEmpty()) {
            if (pm.checkSignatures(pkg, ctx.packageName) == PackageManager.SIGNATURE_MATCH) return
        }
        throw SecurityException("caller uid=$uid is not allowed to read DF theme")
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        enforceCallerAllowed()
        val cols = DfThemeColumns.ALL
        val cursor = MatrixCursor(cols)
        val snap = DfThemeSnapshot.values ?: return cursor
        cursor.addRow(cols.map { snap.get(it) }.toTypedArray())
        return cursor
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.item/vnd.samsung.rootkit.dftheme"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}

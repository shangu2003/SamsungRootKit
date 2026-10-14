package com.samsung.rootkit.ui

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.samsung.rootkit.data.SettingsStore
import com.samsung.rootkit.ui.glass.BackdropProvider
import com.samsung.rootkit.ui.theme.AccentColor
import com.samsung.rootkit.ui.theme.AppThemeMode
import com.samsung.rootkit.ui.theme.RootKitTheme

class MainActivity : AppCompatActivity() {
    // 可变状态用于在 onConfigurationChanged 时触发 Compose 重组
    private var _configVersion = 0
    val configVersion: Int get() = _configVersion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── 语言切换黑屏闪根治：预加热（Pre-warm）──
        // 根因（StackOverflow/Android开发者确认）：setApplicationLocales() 的**首次调用**
        // 必触发 Activity 重建，无视 configChanges="locale|layoutDirection" 标志。
        // 第二次及之后的调用才会尊重该标志不重建。
        // 方案：在 setContent 之前用当前保存的语言预调用一次，让"首次"代价在 UI 不可见时支付。
        val savedLang = SettingsStore.getLanguage(applicationContext)
        val warmupTag = if (savedLang == "system") "" else savedLang
        try {
            AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags(warmupTag)
            )
        } catch (_: Exception) { /* 预加热失败不影响功能 */ }

        // OdinBox 同款：setDecorFitsSystemWindows(false) 让内容延伸到系统栏后面
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
        }
        // 禁用系统导航栏对比遮罩（Android 10+ 默认会给透明导航栏添加半透明背景，导致底栏下方出现矩形）
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
        }
        // 系统栏全透明
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        // 窗口背景透明（内容层自己提供背景）
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        // 旧版本兼容
        if (Build.VERSION.SDK_INT < 30) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
        setContent {
            // key(configVersion): 当 onConfigurationChanged 递增 configVersion 时，
            // 整个 Compose 树销毁重建，读取新 locale 的字符串资源
            val ver = configVersion
            val themeMode = remember { mutableStateOf(SettingsStore.getThemeMode(applicationContext)) }
            val dynamicColor = remember { mutableStateOf(SettingsStore.getDynamicColor(applicationContext)) }
            RootKitTheme(
                themeMode = themeMode.value,
                accentColor = if (dynamicColor.value) AccentColor.Dynamic else AccentColor.Green,
            ) {
                // 全局 backdrop 源（Phase 3 弹窗玻璃模糊）：包裹在 RootKitScreen 外层，
                //  (仅 UI 层，不触碰功能代码)
                BackdropProvider {
                    key(ver) {
                        RootKitScreen(themeModeState = themeMode, dynamicColorState = dynamicColor)
                    }
                }
            }
        }
    }

    // 拦截配置变更（语言切换等），告诉系统"我自己处理，不要重建 Activity"。
    // 配合 AndroidManifest 的 configChanges="locale|layoutDirection" 使用。
    // 当 setApplicationLocales 触发 locale 变更时，系统调用此方法而非 destroy+recreate，
    // 从根源消除黑屏闪（没有 Activity 重建间隙）。不调用任何动画抑制 API，
    // 以保留系统默认的启动/返回图标缩放转场。
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 递增版本号触发 Compose 重组（读取新 locale 的字符串资源）
        _configVersion++
    }
}

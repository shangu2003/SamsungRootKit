package com.samsung.rootkit.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
// DFReroot 配色联动：把当前生效的 colorScheme 发布出去，供 DFReroot 通过
// DfThemeProvider 读取（实现"SRK 改配色 → DFReroot 跟着变"）。
import com.samsung.rootkit.df.DfThemeColumns
import com.samsung.rootkit.df.DfThemeSnapshot

// ---- Accent color options (matches Root-My-Galaxy) ----
enum class AccentColor {
    Dynamic, Blue, Violet, Green, Orange
}

// 品牌亮绿（对齐 F7310 / KernelSU 风格的行动按钮色）
val AppGreen = Color(0xFF4CAF50)
val AppGreenDark = Color(0xFF81C784)

enum class AppThemeMode {
    System, Light, Dark
}

// ---- Typography (ported from Root-My-Galaxy) ----
private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 38.sp, lineHeight = 44.sp, fontWeight = FontWeight.Light),
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Normal),
    headlineSmall = TextStyle(fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.Normal),
    titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium),
)

private fun accentSeed(context: Context, accentColor: AccentColor): Color = when (accentColor) {
    AccentColor.Dynamic -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Color(context.getColor(android.R.color.system_accent1_500))
        } else {
            Color(0xFF415F91) // Blue fallback
        }
    }
    AccentColor.Blue -> Color(0xFF415F91)
    AccentColor.Violet -> Color(0xFF6750A4)
    AccentColor.Green -> Color(0xFF356A35)
    AccentColor.Orange -> Color(0xFF8B4F23)
}

@Composable
fun RootKitTheme(
    accentColor: AccentColor = AccentColor.Blue,
    themeMode: AppThemeMode = AppThemeMode.System,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppThemeMode.System -> systemDarkTheme
        AppThemeMode.Light -> false
        AppThemeMode.Dark -> true
    }
    val colors = if (accentColor == AccentColor.Dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val generatedColors = rememberDynamicColorScheme(
            seedColor = accentSeed(context, accentColor),
            isDark = darkTheme,
            style = PaletteStyle.TonalSpot,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
        )
        if (darkTheme) {
            generatedColors
        } else {
            generatedColors.copy(
                onSurfaceVariant = lerp(generatedColors.surface, generatedColors.onSurface, 0.8f),
            )
        }
    }

    SideEffect {
        val window = (context as Activity).window
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    // ── DFReroot 配色联动（v5.1.1）──
    // 把**实际生效**的 colorScheme 令牌发布出去（不管它来自系统动态取色还是
    // material-kolor 从种子色生成，DFReroot 拿到的都是同一套值）。
    // DfThemeSnapshot.publish 内部做了"值没变就不写"的短路，重组不会反复新建 Bundle。
    SideEffect {
        DfThemeSnapshot.publish(
            Bundle().apply {
                putInt(DfThemeColumns.DARK, if (darkTheme) 1 else 0)
                putInt(DfThemeColumns.BACKGROUND, colors.background.toArgb())
                putInt(DfThemeColumns.ON_BACKGROUND, colors.onBackground.toArgb())
                putInt(DfThemeColumns.SURFACE, colors.surface.toArgb())
                putInt(DfThemeColumns.ON_SURFACE, colors.onSurface.toArgb())
                putInt(DfThemeColumns.SURFACE_CONTAINER, colors.surfaceContainer.toArgb())
                putInt(DfThemeColumns.ON_SURFACE_VARIANT, colors.onSurfaceVariant.toArgb())
                putInt(DfThemeColumns.OUTLINE, colors.outline.toArgb())
                putInt(DfThemeColumns.PRIMARY, colors.primary.toArgb())
                putInt(DfThemeColumns.ON_PRIMARY, colors.onPrimary.toArgb())
                putInt(DfThemeColumns.PRIMARY_CONTAINER, colors.primaryContainer.toArgb())
                putInt(DfThemeColumns.ON_PRIMARY_CONTAINER, colors.onPrimaryContainer.toArgb())
                putInt(DfThemeColumns.ERROR, colors.error.toArgb())
            }
        )
    }

    MaterialExpressiveTheme(
        colorScheme = colors,
        typography = AppTypography,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}

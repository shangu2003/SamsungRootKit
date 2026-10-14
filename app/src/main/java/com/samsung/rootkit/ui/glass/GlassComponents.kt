package com.samsung.rootkit.ui.glass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samsung.rootkit.ui.glass.blur.Sky
import com.samsung.rootkit.ui.glass.blur.rememberSky
import com.samsung.rootkit.ui.glass.blur.sky
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens

/**
 * Glass UI building blocks for SamsungRootKit, implementing the OdinBox v0.4.19
 * "titanium / glass" design system in a clean-room fashion (see [GlassTokens] for provenance).
 *
 * Architecture mirrors OdinBox:
 *  - [GlassSkyProvider] captures a blurred backdrop via cloudy's `sky()` (the analogue of
 *    OdinBox's BlurBackgroundView / RealtimeBackdropDrawable).
 *  - [GlassCard] / [GlassSurface] are the *translucent layered surfaces* (the analogue of
 *    glass_card_background.xml / titanium_card_bg.xml layer-lists): a semi-transparent fill
 *    + a white hairline stroke + a faint top-down sheen.
 *  - [GlassBottomBarContainer] is the floating nav bar: cloudy backdrop blur + double hairline.
 *  - [GlassDialogSurface] reproduces dialog_bg.xml / dialog_bg_glass.xml.
 */

val LocalGlassSky = compositionLocalOf<Sky> {
    error("LocalGlassSky not provided. Wrap your content with GlassSkyProvider.")
}

/**
 * 全局玻璃渲染开关（CompositionLocal）。
 *
 * 默认 true（玻璃正常渲染）。当设为 false 时，所有 GlassCard / GlassSurface 的
 * blurBehind 分支降级为简单玻璃（纯色填充 + 高光，无 backdrop 捕获/模糊），
 * 从而消除执行页面的实时模糊 GPU 开销。
 *
 * 用途：进入 Root 执行页时停用玻璃 → 锁定 60Hz 降低功耗与发热；
 *       进入完成页时恢复玻璃渲染。
 */
val LocalGlassEnabled = compositionLocalOf { true }

@Composable
private fun isDarkTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun GlassSkyProvider(
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    val sky = rememberSky()
    val dark = isDarkTheme()
    val backgroundBrush = if (dark) {
        Brush.verticalGradient(
            colors = listOf(
                TitaniumDark.BackgroundTop,
                TitaniumDark.BackgroundMid,
                TitaniumDark.BackgroundBottom,
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                GlassLight.BackgroundTop,
                GlassLight.BackgroundMid,
                GlassLight.BackgroundBottom,
            )
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        // LocalGlassSky must be provided outermost so both content and overlay can read it.
        CompositionLocalProvider(LocalGlassSky provides sky) {
            // Blurred backdrop source layer (analogue of OdinBox's backdropContentLayer).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .sky(sky)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundBrush)
                )
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ) {
                    content()
                }
            }
            // Overlay layer (e.g. floating bottom bar) sits outside the blurred source.
            overlay()
        }
    }
}

/**
 * Frosted glass card.
 *
 * Dark  → titanium_card_bg.xml : fill [TitaniumDark.Card] (#2A384850, 42%) + white hairline
 *                              [TitaniumDark.Hairline] (#26FFFFFF, 15%) @1dp.
 * Light → glass_card_background.xml : fill [GlassLight.Card] (#59FFFFFF, 35%) + white hairline
 *                              [GlassLight.Hairline] (#CCFFFFFF, 80%) @1dp.
 *
 * A faint top-down white sheen reproduces the layer-list's inner shading items, giving the
 * surface its characteristic "lit glass" edge.
 *
 * @param blurBehind when true, additionally blurs the live backdrop behind the card (cloudy),
 *   matching OdinBox's per-view RealtimeBackdropDrawable frost. Default false: the app backdrop
 *   is a uniform gradient, so the translucent fill alone already yields the glass look and this
 *   avoids the per-card capture cost. Set true for cards floating over live/scrolled content.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = GlassRadii.Card,
    blurBehind: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val dark = isDarkTheme()
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }

    val fill = if (dark) TitaniumDark.Card else GlassLight.Card
    val hairline = if (dark) TitaniumDark.Hairline else GlassLight.Hairline
    val sheenAlpha = if (dark) 0.04f else 0.10f

    val base = Modifier
        .then(
            if (onClick != null) Modifier.clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            ) else Modifier
        )
        .clip(shape)

    Surface(
        modifier = modifier.then(base),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, hairline),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        if (blurBehind && LocalGlassEnabled.current) {
            // 可选真·玻璃模糊（Kyant0/Backdrop）——仅 blurBehind=true 时启用（弹窗浮层等）。
            //
            // ⚠️ 关键红线（Round86 真机铁证）：此处的两个 Box 绝不能用 .fillMaxSize()！
            //
            // 原因：GlassCard 的 Surface(color=Transparent) 是 wrap content。
            // 当 GlassCard 用于 wrap-height 卡片（如 Step List Card，modifier=fillMaxWidth 无 weight）
            // 时，父 Column 给它的垂直约束是 "max = 剩余空间"（一个很大的值，如 1500px）。
            // 如果内部 Box 用 fillMaxSize()，Box 会取这个 max 约束值 → Surface 被撑到 1500px
            // → wrap卡片异常膨胀 → 后面的 weight(1f) Log Card 被挤出屏幕。
            //
            // 简单玻璃分支的 Box 不用 fillMaxSize，所以不会膨胀（v4.5.3 旧版稳定行为）。
            //
            // layerBackdrop / drawBackdrop 的 GraphicsLayer 由各自 Node 的 onGloballyPositioned
            // 回调设置坐标和大小（DrawBackdropNode/LayerBackdropNode 反编译验证），不依赖
            // Box 的 fillMaxSize 来确定覆盖范围。所以去掉 fillMaxSize 不影响模糊效果。
            val cardBackdrop = rememberLayerBackdrop()
            Box(
                modifier = Modifier
                    .layerBackdrop(cardBackdrop)
            )
            Box(
                modifier = Modifier
                    .drawBackdrop(
                        backdrop = cardBackdrop,
                        shape = { shape },
                        effects = {
                            blur(16.dp.toPx())
                            lens(24.dp.toPx(), 24.dp.toPx())
                        },
                        onDrawSurface = { drawRect(color = fill) },
                    )
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.White.copy(alpha = sheenAlpha),
                                0.5f to Color.Transparent,
                            )
                        )
                    )
            ) {
                content()
            }
        } else {
            // 默认简单玻璃（对齐 v4.5.3 旧版行为，wrap 卡片不膨胀）。
            //
            // ⚠️ 此 Box 禁用 fillMaxSize()！
            // 原因：GlassCard 的 Surface(color=Transparent) 由外部 modifier 决定尺寸。
            //   - wrap 卡片（Modifier.fillMaxWidth() 无 weight）：Surface 高度 = content wrap
            //     若 Box 用 fillMaxSize，在父 Column 给出大 max 约束时会取 max 值 → 膨胀
            //   - fill 卡片（Modifier.weight(1f)）：Surface 高度 = weight 分配值
            //     content() 内部自有 Column(fillMaxSize)/LazyColumn(weight(1f)) 撑满即可，
            //     不需要此 Box fillMaxSize 传递约束（Compose 子→父尺寸传播自动处理）。
            Box(
                modifier = Modifier
                    .background(fill)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.White.copy(alpha = sheenAlpha),
                                0.5f to Color.Transparent,
                            )
                        )
                    )
            ) {
                content()
            }
        }
    }
}

/**
 * Full-bleed glass pane (zero corner radius). Same translucent surface as [GlassCard].
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    blurBehind: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 0.dp,
        blurBehind = blurBehind,
        onClick = onClick,
        content = content,
    )
}

/**
 * Floating bottom navigation container.
 *
 * Uses cloudy's backdrop blur over the shared sky, with a double white hairline
 * (outer 1dp + inner 1dp) — the standard OdinBox floating-nav treatment.
 *
 * Now with frosted glass fill (~20% opacity) for visibility over scrolled content,
 * fixed 70dp height, and content vertically centered.
 */
@Composable
fun GlassBottomBarContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dark = isDarkTheme()
    val shape = RoundedCornerShape(999.dp)

    // Round67: 底栏磨砂玻璃 — 基于 Kyant0/Backdrop 渲染管线精确调校。
    //
    // 渲染层级（已反编译 DrawBackdropNode.draw 确认）：
    //   ① blurred backdrop layer（drawLayer）→ 背景模糊内容
    //   ② onDrawSurface（drawRect tint，alpha 叠加）→ 半透明染色层盖在模糊之上
    //   ③ lens 高光 → 表面镜面微高光（毛玻璃质感来源）
    //   ④ drawContent → 图标/文字
    // 即：磨砂玻璃 = 模糊背景透出 + 半透明染色层 + 镜面高光，三者叠加，不是单纯颜色叠透明度。
    //
    // 调校原则（对齐 GlassTokens 设计体系 + iOS frosted 观感）：
    //   - tint 用库既定材质色（GlassLight.Card / TitaniumDark.Card），保证色彩一致；
    //   - alpha 取 ~0.55–0.6：模糊仍可见（磨砂感），但表面已是有质感的玻璃而非透窗；
    //   - lens 提供顶部微高光（frosted 关键），blur 给足 24dp 形成明显柔化。
    val frostedFill = if (dark) TitaniumDark.Card else GlassLight.Card
    val outerBorder = if (dark) TitaniumDark.Hairline else GlassLight.Hairline
    val innerBorder = if (dark) Color.Transparent else GlassLight.Hairline

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        // Round69: 底栏改为居中短胶囊形（非全宽），参考 iOS Dock / 截图2-3 效果。
        // 内层 Surface 约束最大宽度为屏幕的 ~87%，水平居中，
        // 形成紧凑胶囊容器，pill 在内部四边等距。
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .clip(shape),
            shape = shape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, outerBorder),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
        ) {
            // 玻璃总开关：LocalGlassEnabled=false（执行页自动停用）时不做 backdrop 捕获与
            // GPU blur/lens，降级为静态磨砂纯色，彻底消除底栏实时渲染开销。
            val glassOn = LocalGlassEnabled.current
            if (glassOn) {
                // Phase 3 扩展：下栏接入真·玻璃模糊（Kyant0/Backdrop）。
                // 用独立的局部 layerBackdrop，只捕获「下栏背后的主页内容」一小块，
                // 不能引用全局 LocalBackdrop，否则捕获→模糊→再捕获递归致 RenderThread SIGSEGV。
                val barBackdrop = rememberLayerBackdrop()
                // 捕获层：放在模糊层之前（绘制顺序在下），捕获其下方兄弟（主页内容）作模糊源。
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(barBackdrop)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBackdrop(
                            backdrop = barBackdrop,
                            shape = { shape },
                            effects = {
                                blur(24.dp.toPx())
                                lens(28.dp.toPx(), 28.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            frostedFill.copy(alpha = 0.72f),
                                            frostedFill.copy(alpha = 0.52f),
                                        ),
                                    ),
                                )
                            },
                        )
                        .padding(1.dp)
                        .clip(shape)
                        .border(
                            width = 1.dp,
                            color = innerBorder,
                            shape = shape,
                        )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        content()
                    }
                }
            } else {
                // 关闭动态玻璃：静态磨砂纯色胶囊（无 backdrop 捕获 / 无 GPU 模糊）。
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    frostedFill.copy(alpha = 0.92f),
                                    frostedFill.copy(alpha = 0.86f),
                                ),
                            ),
                        )
                        .padding(1.dp)
                        .clip(shape)
                        .border(width = 1.dp, color = innerBorder, shape = shape),
                    contentAlignment = Alignment.Center,
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Glass dialog surface — wraps dialog content with OdinBox's dialog styling:
 *   Dark  → dialog_bg.xml        : #F0161B22 + hairline #33FFFFFF (20%) @1dp, 24dp radius.
 *   Light → dialog_bg_glass.xml  : #E6FFFDFB + hairline #FFFFFFFF @1.5dp, 32dp radius.
 *
 * Use this as the container for custom dialogs. (Compose Material3 AlertDialog does not
 * support a hairline border directly, so it is left on the theme; see integration report.)
 */
@Composable
fun GlassDialogSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = GlassRadii.Dialog,
    content: @Composable () -> Unit,
) {
    val dark = isDarkTheme()
    val shape = RoundedCornerShape(cornerRadius)
    val bg = if (dark) TitaniumDark.DialogBg else GlassLight.DialogBg
    val stroke = if (dark) TitaniumDark.Edge else GlassLight.DialogHairline
    val strokeWidth = if (dark) 1.dp else 1.5.dp

    Surface(
        modifier = modifier,
        shape = shape,
        color = bg,
        border = BorderStroke(strokeWidth, stroke),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        content()
    }
}

private fun Color.luminance(): Float {
    val r = red * 0.2126f
    val g = green * 0.7152f
    val b = blue * 0.0722f
    return r + g + b
}

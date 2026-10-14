package com.samsung.rootkit.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * 全局 backdrop 源（Phase 3 弹窗玻璃模糊接入点）。
 *
 * 机制（源自 Kyant0/Backdrop 2.0.0 源码验证）：
 *  - [rememberLayerBackdrop] 创建一个坐标相关的 backdrop 源，捕获「在其下方、之前绘制」的内容。
 *  - [Modifier.layerBackdrop] 挂在本 Provider 的最外层 Box 上，因此捕获的是整个 App 页面内容
 *    （即弹窗背后的真实内容），供任意 [CustomAlertDialog] 通过 [LocalBackdrop] 读取并做实时模糊。
 *
 * 这是一条**新增的并行玻璃通道**，与既有 [GlassSkyProvider] / 自研 `GlassBlur` 体系互不干扰：
 * 现有主页大卡片（GlassCard）维持原样，本源仅服务于自定义弹窗。
 *
 * 兼容性：blur 依赖 RenderEffect(API31+)，lens 依赖 RuntimeShader(API33+)。库内部已做能力探测，
 * API 不足时对应 effect 自动 no-op（源码验证：Blur.kt / Lens.kt），故不会因设备版本导致崩溃。
 */
val LocalBackdrop = compositionLocalOf<Backdrop> { emptyBackdrop() }

@Composable
fun BackdropProvider(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    // 最外层 Box 挂 layerBackdrop：捕获其下方绘制的全部页面内容，作为弹窗模糊源。
    Box(modifier = modifier.fillMaxSize().layerBackdrop(backdrop)) {
        CompositionLocalProvider(LocalBackdrop provides backdrop) {
            content()
        }
    }
}

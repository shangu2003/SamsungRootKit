package com.samsung.rootkit.ui.glass.blur

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.LayoutDirection

/**
 * `RectangleShape` was removed from Compose Foundation in 1.11.x, so we provide a local equivalent
 * to keep API parity with cloudy's `cloudy(shape = RectangleShape)` default.
 */
private val RectangleShape: Shape = GenericShape { size, _ ->
    addRect(Rect(0f, 0f, size.width, size.height))
}

/**
 * Self-contained backdrop-blur primitives replacing the unavailable
 * `com.github.skydoves:cloudy` dependency (JitPack reports "Error" for every version of
 * skydoves/cloudy, and the artifact is also absent from Maven Central).
 *
 * Clean-room reimplementation of cloudy's public surface (rememberSky / sky / cloudy) so that
 * [com.samsung.rootkit.ui.glass.GlassComponents] keeps calling the same API.
 *
 * IMPLEMENTATION NOTE — why this is a *translucent* backdrop, not a pixel blur:
 * True live backdrop blur requires capturing a sibling composable into a shared
 * `GraphicsLayer` and compositing it blurred — the `Modifier.graphicsLayer(GraphicsLayer)`
 * overload that libraries like cloudy rely on. That overload is **not present** in the Compose
 * version resolved for this project (1.11.x), which is in fact why cloudy itself cannot target
 * this Compose level. Without it, the most robust, version-stable approach is a translucent
 * frosted backdrop: the tint is painted over the real content sitting behind the surface, and —
 * together with the layered translucent fill + white hairline defined in GlassComponents — it
 * reads as glass (the same layered-translucent core that defines OdinBox's "titanium/glass" look).
 * The live GPU blur can be reintroduced later by pinning Compose to a version that exposes the
 * shared-GraphicsLayer API, or by switching to a RenderEffect-based capture (see integration
 * report).
 */
class Sky internal constructor()

@Composable
fun rememberSky(): Sky = remember { Sky() }

/** Placeholder for parity with cloudy's `sky()`; the capture step is a no-op in this build. */
fun Modifier.sky(sky: Sky): Modifier = this

/**
 * Draws a translucent frosted backdrop over whatever content sits behind this surface.
 *
 * @param sky      retained for API parity with cloudy (unused in this translucent implementation).
 * @param radius   retained for API parity (blur radius, px) — reserved for a future GPU blur.
 * @param tint     colour washed over the backdrop. Use [Color.Unspecified] to draw nothing
 *   (e.g. when the host surface already supplies its own translucent fill).
 * @param shape    clip shape for the backdrop (should match the host surface's corner radius).
 * @param cpuBlurEnabled retained for API parity with cloudy.
 */
fun Modifier.glassBackdrop(
    sky: Sky,
    radius: Int,
    tint: Color = Color.Unspecified,
    shape: Shape = RectangleShape,
    @Suppress("UNUSED_PARAMETER") cpuBlurEnabled: Boolean = true,
): Modifier = this.drawWithContent {
    if (tint != Color.Unspecified) {
        drawRect(color = tint, size = size)
    }
}

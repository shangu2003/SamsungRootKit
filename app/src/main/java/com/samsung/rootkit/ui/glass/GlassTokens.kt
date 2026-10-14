package com.samsung.rootkit.ui.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glass / "titanium" design tokens — CLEAN-ROOM port of OdinBox v0.4.19's glass UI.
 *
 * Provenance: every value below is reverse-engineered from the decoded resources of
 * OdinBox v0.4.19 (package com.samsung.firmwarehelper), stored in:
 *   analysis/odinbox_decoded/glass_drawables.txt   (layer-list decode, authoritative for fills/strokes)
 *   analysis/odinbox_decoded/resources_dump.txt    (color table: titanium_*, colorAccent, windowBackground)
 *   analysis/odinbox_decoded/blur_usage.txt         (RenderEffect.createBlurEffect(30f,30f,CLAMP) + SDK guard v1=31,
 *                                                     RealtimeBackdropDrawable software fallback)
 *
 * We do NOT copy OdinBox source code or assets — only the *design parameters* (colors, alphas,
 * radii, stroke widths, blur radius) are reproduced, then re-expressed in Compose.
 *
 * Color encoding note: Compose [Color] long literal is 0xAARRGGBB.
 */

// ── Corner radii (dp) ──────────────────────────────────────────────────────────────
object GlassRadii {
    val Card = 28.dp           // glass_card_background.xml / card_background.xml / card_background_dark.xml
    val Dialog = 24.dp         // dialog_bg.xml (dark dialog)
    val GlassDialog = 32.dp    // dialog_bg_glass.xml (light glass dialog)
    val Input = 999.dp         // glass_input_background.xml (pill)
    val Chip = 28.dp           // bg_history_chip.xml
    val Button = 24.dp         // btn_primary.xml / btn_outlined.xml
    val TitaniumCard = 16.dp   // titanium_card_bg.xml (compact / inner card)
}

// ── Blur radii (px, Int) ──────────────────────────────────────────────────────────
// cloudy's `radius` is in pixels (not dp), which matches OdinBox's
// RenderEffect.createBlurEffect(30f, 30f, CLAMP) — also pixels.
object GlassBlur {
    val Backdrop = 30   // OdinBox BlurBackgroundView — RenderEffect SDK guard v1 = 31
    val Card = 16       // per-card frost when GlassCard(blurBehind = true)
    val BottomBar = 30  // floating navigation bar frost
}

// ── Dark theme ("titanium") ───────────────────────────────────────────────────────
object TitaniumDark {
    val BackgroundTop = Color(0xFF161B22)     // R.color.titanium_bg / windowBackground / colorPrimaryDark
    val BackgroundMid = Color(0xFF12131F)
    val BackgroundBottom = Color(0xFF0D0E18)
    val Card = Color(0x2A384850)             // titanium_card_bg.xml main fill (AA=2A≈42% cool dark blue-grey)
    val CardInnerShadow = Color(0x0A000000)   // titanium_card_bg.xml layer 1 (subtle top shade)
    val Hairline = Color(0x26FFFFFF)         // titanium_card_bg.xml stroke (AA=26≈15% white) @1dp
    val Edge = Color(0x33FFFFFF)             // R.color.titanium_edge / dialog_bg.xml stroke (AA=33≈20% white)
    val Accent = Color(0xFF00E5FF)           // R.color.titanium_accent (cyan)
    val AccentPressed = Color(0x4D00E5FF)    // R.color.titanium_pressed
    val DialogBg = Color(0xF0161B22)         // dialog_bg.xml solid (AA=F0≈94% titanium dark)
    val ChipBg = Color(0xFF262A34)           // bg_history_chip.xml
    val ChipHairline = Color(0x1AFFFFFF)     // bg_history_chip.xml stroke (AA=1A≈10% white) @0.5dp
}

// ── Light theme ("glass") ─────────────────────────────────────────────────────────
object GlassLight {
    val BackgroundTop = Color(0xFFF5F3EE)
    val BackgroundMid = Color(0xFFF0EDE6)
    val BackgroundBottom = Color(0xFFEDEAE2)
    val Card = Color(0x59FFFFFF)             // glass_card_background.xml main fill (AA=59≈35% white)
    val Hairline = Color(0xCCFFFFFF)         // glass_card_background.xml stroke (AA=CC≈80% white) @1dp
    val DialogBg = Color(0xE6FFFDFB)         // dialog_bg_glass.xml solid (AA=E6≈90% warm white)
    val DialogHairline = Color(0xFFFFFFFF)   // dialog_bg_glass.xml stroke (100% white) @1.5dp
    val Accent = Color(0xFF10B981)           // GlassDialogTheme colorAccent (green)
    val InputBg = Color(0xE6FFFDFB)          // glass_input_background.xml main (AA=E6≈90% white)
    val InputHairline = Color(0xFFFFFFFF)     // glass_input_background.xml stroke (100% white) @1.5dp
}

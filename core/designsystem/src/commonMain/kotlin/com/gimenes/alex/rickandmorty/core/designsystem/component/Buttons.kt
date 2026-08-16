package com.gimenes.alex.rickandmorty.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import com.gimenes.alex.rickandmorty.core.designsystem.theme.PortalRingShape
import com.gimenes.alex.rickandmorty.core.designsystem.theme.RickAndMortyExtendedTheme

/** Minimum interactive touch target per the a11y spec (>=48dp) - shared by every button here. */
private val MIN_TOUCH_TARGET = 48.dp

/**
 * Extra headroom (above/below/around the pill) reserved so [PrimaryGradientButton]'s glow can
 * bleed past the pill's own edges instead of being cropped flush to it.
 */
private val GLOW_BLEED = 10.dp

/**
 * Tier 1 of the two-tier button system (R3, issue #39): the primary, committed/scored action.
 * Diagonal `primary` -> `accent` gradient fill with a soft `accent`-tinted glow, pill-shaped
 * (fully rounded, distinct from card corners). Label renders in `onPrimary` (dark, per R1's
 * tokens) since it must stay legible across the whole gradient, not just its `primary` end.
 *
 * The glow is a manually-drawn [Canvas] radial gradient (see [PortalRingGlyph]'s kdoc for the
 * same cross-platform-consistency rationale), not `Modifier.shadow`.
 *
 * Use for every committed/scored primary action across both game features: Start Quiz, Next/See
 * Results, Replay, Play Again, Retry.
 */
@Composable
fun PrimaryGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val accent = RickAndMortyExtendedTheme.extendedColors.accent
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val disabledColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET + GLOW_BLEED * 2),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (enabled) {
                val glowRadius = this.size.minDimension * 2.4f
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to accent.copy(alpha = 0.5f),
                        0.5f to accent.copy(alpha = 0.2f),
                        1f to accent.copy(alpha = 0f),
                        center = center,
                        radius = glowRadius,
                    ),
                    radius = glowRadius,
                    center = center,
                )
            }
        }

        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = PortalRingShape,
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth().heightIn(min = MIN_TOUCH_TARGET),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = if (enabled) {
                            Brush.linearGradient(colors = listOf(primary, accent))
                        } else {
                            Brush.linearGradient(colors = listOf(disabledColor, disabledColor))
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = onPrimary,
                )
            }
        }
    }
}

/**
 * Tier 2 of the two-tier button system (R3, issue #39): a text-only, `accent`-colored, bold
 * link-style CTA with an arrow suffix baked into the caller-supplied [text] (e.g. `"Play now ->"`)
 * rather than appended automatically, so callers control exact wording/arrow glyph per context.
 *
 * Reserved for exploratory/low-commitment actions - never a committed/scored one (use
 * [PrimaryGradientButton] for those).
 *
 * Two usage modes, selected via [onClick]:
 * - `onClick = null` (the default): renders as **plain, non-interactive text** with no semantics
 *   node of its own. This is the mode [ModeCard] uses for its embedded "Play now" affordance -
 *   critical so the CTA stays merged into the card's single clickable accessibility node instead
 *   of becoming a second, separately-focusable one. See
 *   `core/designsystem/src/androidInstrumentedTest/.../ModeCardAccessibilityInstrumentedTest.kt`
 *   for the semantics-tree verification of that requirement.
 * - `onClick` provided: renders as its **own standalone clickable text node** (`Role.Button`,
 *   >=48dp touch target) - the mode for a truly standalone link-style CTA, e.g. R5's Home usage
 *   (entering a mode directly from Home, outside of a ModeCard).
 */
@Composable
fun LinkCtaText(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val accent = RickAndMortyExtendedTheme.extendedColors.accent
    if (onClick == null) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier
                .heightIn(min = MIN_TOUCH_TARGET)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
    }
}

/**
 * Tier 3 (secondary, non-scored actions - Home/Back-style): the existing outlined pattern,
 * refreshed to explicitly border in R1's new `outline` token rather than relying on
 * `OutlinedButton`'s Material3 default border color/alpha.
 */
@Composable
fun SecondaryOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = MIN_TOUCH_TARGET),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(text)
    }
}

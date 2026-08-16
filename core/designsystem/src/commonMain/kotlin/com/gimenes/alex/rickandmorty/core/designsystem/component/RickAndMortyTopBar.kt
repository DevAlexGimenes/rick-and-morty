package com.gimenes.alex.rickandmorty.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gimenes.alex.rickandmorty.core.designsystem.theme.RickAndMortyExtendedTheme

/**
 * Shared, pinned exit/back top bar for feature flows that need one (issue #55): Trivia Quiz and
 * Guess the Character. Deliberately NOT Material3's [androidx.compose.material3.TopAppBar] /
 * [androidx.compose.material3.Scaffold] `topBar` slot - both screens already drive their own
 * [androidx.compose.material3.Scaffold] wrapping a single full-screen `when` over their UI state,
 * with no collapsing/scrolling app-bar behavior in their spec. A plain fixed-height [Row] placed
 * as the first child above that state content (see `QuizScreen`/`GuessCharacterScreen`) is the
 * least invasive integration and needs none of `TopAppBar`'s scroll-behavior machinery.
 *
 * Visual language deliberately flush with the page - `background`-colored container, no shadow or
 * tonal elevation - with only a 1dp `outline`-at-30%-alpha hairline bottom border for separation.
 * Mirrors [ModeCard]'s existing hairline-border treatment (same color/alpha) rather than
 * introducing a new elevated-surface look. The back arrow is hand-drawn via [Canvas], mirroring
 * [ModeCard]'s `ChevronGlyph` geometry mirrored to point left instead of right - no Material icon
 * library is pulled in, consistent with every other glyph in this design system.
 *
 * @param exitContentDescription a per-caller, per-screen description of what the arrow actually
 *  does (e.g. "Exit Trivia Quiz") - never a generic "Back", so screen-reader users hear what
 *  leaving this screen means rather than an ambiguous navigation verb.
 * @param isTitleHeading whether [title] is exposed to accessibility services as a heading.
 *  Defaults to `true` (a top bar title is ordinarily a page's heading). Screens that already
 *  expose their own distinct, dynamic in-page heading carrying the identical title text (e.g.
 *  Guess the Character's Round screen, whose in-page heading doubles as the per-round
 *  accessibility refocus target) should pass `false` here to avoid two headings announcing the
 *  same static text back to back.
 */
@Composable
fun RickAndMortyTopBar(
    title: String,
    onExitClick: () -> Unit,
    exitContentDescription: String,
    modifier: Modifier = Modifier,
    isTitleHeading: Boolean = true,
) {
    val spacing = RickAndMortyExtendedTheme.spacing
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = HAIRLINE_BORDER_ALPHA)
    val borderWidthPx = with(LocalDensity.current) { HAIRLINE_BORDER_WIDTH.toPx() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, size.height - borderWidthPx / 2f),
                    end = Offset(size.width, size.height - borderWidthPx / 2f),
                    strokeWidth = borderWidthPx,
                )
            }
            .heightIn(min = TOP_BAR_HEIGHT)
            .padding(horizontal = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onExitClick,
            modifier = Modifier
                .size(MIN_TOUCH_TARGET)
                .semantics { contentDescription = exitContentDescription },
        ) {
            BackArrowGlyph(color = MaterialTheme.colorScheme.onBackground)
        }
        val titleModifier = if (isTitleHeading) {
            Modifier.padding(start = spacing.xs).semantics { heading() }
        } else {
            Modifier.padding(start = spacing.xs)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = titleModifier,
        )
    }
}

/**
 * Hand-drawn left-pointing arrow/chevron. Geometrically this is exactly
 * [ModeCard]'s private `ChevronGlyph` (same stroke-width ratio, same two-segment "V" construction)
 * with its x-coordinates mirrored so the apex points left instead of right.
 */
@Composable
private fun BackArrowGlyph(color: Color, iconSize: Dp = 20.dp) {
    Canvas(modifier = Modifier.size(iconSize)) {
        val strokeWidth = size.minDimension * 0.16f
        drawLine(
            color = color,
            start = Offset(size.width * 0.7f, size.height * 0.15f),
            end = Offset(size.width * 0.3f, size.height * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.3f, size.height * 0.5f),
            end = Offset(size.width * 0.7f, size.height * 0.85f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

/** Minimum interactive touch target per the a11y spec (>=48dp). */
private val MIN_TOUCH_TARGET = 48.dp

/** Standard fixed top bar height - roomy enough that the 48dp icon button sits comfortably inset. */
private val TOP_BAR_HEIGHT = 56.dp

/** Hairline bottom border width, matching [ModeCard]'s own border width. */
private val HAIRLINE_BORDER_WIDTH = 1.dp

/** Hairline bottom border alpha, matching [ModeCard]'s `outline`-at-30%-alpha border treatment. */
private const val HAIRLINE_BORDER_ALPHA = 0.3f

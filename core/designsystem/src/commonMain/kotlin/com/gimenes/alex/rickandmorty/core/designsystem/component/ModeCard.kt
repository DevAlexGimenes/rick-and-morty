package com.gimenes.alex.rickandmorty.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import com.gimenes.alex.rickandmorty.core.designsystem.theme.RickAndMortyExtendedTheme
import com.gimenes.alex.rickandmorty.core.designsystem.theme.RickAndMortyShapes

/**
 * A large tappable card used on Home to present a game mode (Trivia Quiz / Guess the Character).
 *
 * Redesigned in R3 (issue #39) from a full-width banner-style card to a compact header layout:
 * a [PortalRingGlyph]-style [icon] + stacked [title]/[description] + a chevron affordance, in one
 * row, followed by a "Play now"-style [LinkCtaText] CTA. `RickAndMortyShapes.medium` (20dp, down
 * from `large`/28dp), a 1dp hairline `outline`-at-30%-alpha border, and a subtle vertical
 * `surface` -> `surfaceVariant` gradient fill (an alpha-blended solid gradient, not true
 * translucency/blur - LUMA explicitly deferred blur pending cross-platform feasibility
 * verification) replace the previous flat `surface` fill.
 *
 * **Accessibility**: the whole card is a single [Surface] with `onClick`, so its default semantics
 * merging picks up every descendant's text/click affordance - including the embedded "Play now"
 * CTA - into one accessibility node. This must NOT regress into two separately-focusable nodes;
 * see `core/designsystem/src/androidInstrumentedTest/.../ModeCardAccessibilityInstrumentedTest.kt`
 * for the semantics-tree verification (not just an assumption that `clickable`'s default merging
 * holds).
 *
 * @param icon the header glyph, e.g. `{ PortalRingGlyph(size = 44.dp, content = { QuestionMarkGlyph() }) }`
 *  for Trivia or `{ PortalRingGlyph(size = 44.dp, content = { CharacterSilhouetteGlyph() }) }` for
 *  Guess the Character. Purely decorative by convention here - the adjacent [title] already
 *  conveys the same information, matching [PortalRingGlyph]'s own decorative-by-default guidance.
 */
@Composable
fun ModeCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {},
) {
    val spacing = RickAndMortyExtendedTheme.spacing
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant,
        ),
    )

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RickAndMortyShapes.medium,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundBrush)
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                icon()
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ChevronGlyph()
            }
            LinkCtaText(text = "Play now →")
        }
    }
}

/** Small ">" chevron affordance at the far right of [ModeCard]'s header row. Purely decorative. */
@Composable
private fun ChevronGlyph() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = Modifier.size(20.dp)) {
        val strokeWidth = size.minDimension * 0.16f
        drawLine(
            color = color,
            start = Offset(size.width * 0.3f, size.height * 0.15f),
            end = Offset(size.width * 0.7f, size.height * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.7f, size.height * 0.5f),
            end = Offset(size.width * 0.3f, size.height * 0.85f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

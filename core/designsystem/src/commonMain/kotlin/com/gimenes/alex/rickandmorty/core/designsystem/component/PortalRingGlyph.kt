package com.gimenes.alex.rickandmorty.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gimenes.alex.rickandmorty.core.designsystem.theme.RickAndMortyExtendedTheme

/**
 * Shared "portal ring" glyph primitive (R3, issue #39): two concentric ring/arc strokes in
 * [ringColor] with a soft radial glow in [glowColor] behind them - a manually-drawn [Canvas]
 * gradient rather than [androidx.compose.ui.draw.shadow]/`Modifier.shadow`, per LUMA's
 * cross-platform-consistency reasoning (native shadow rendering differs visibly across the
 * Android/Desktop/iOS Compose targets this app ships to; a hand-drawn radial gradient renders
 * identically everywhere).
 *
 * This is the single building block behind every "portal ring" glyph in the app: ModeCard's
 * compact header icon here in R3, and - without any change needed to this composable - Home's
 * hero treatment (R5) and the app icon/splash source art (R8/R9) later, by supplying a different
 * [size], [content], or omitting [content] entirely for a plain ring.
 *
 * @param size outer diameter of the glyph; the glow is allowed to extend slightly beyond this
 *  box (Compose does not clip a composable's own [Canvas] drawing to its layout bounds by
 *  default), which is what gives the glow its "bleeding past the ring" look.
 * @param contentDescription accessibility text for the glyph. Pass `null` (the default) when the
 *  glyph is purely decorative in context - e.g. inside [ModeCard], where the adjacent title/
 *  subtitle already convey the same information. When `null`, the glyph is explicitly marked
 *  [hideFromAccessibility] rather than left to whatever the caller's default semantics merging happens
 *  to produce; when non-null, it's exposed as a single accessible node with that description.
 * @param ringColor color of the ring/arc strokes. Defaults to [MaterialTheme.colorScheme.primary].
 * @param glowColor color of the glow behind the ring. Defaults to
 *  [RickAndMortyExtendedTheme.extendedColors.accent] - this is exactly the "glow effect" use case
 *  the `accent` token is reserved for (see `ExtendedColors.accent` kdoc); this primitive must
 *  never be given a caller-supplied `glowColor` that turns it into a large flat accent fill.
 * @param content optional inner glyph, drawn centered inside the ring (e.g. [QuestionMarkGlyph]
 *  for Trivia, [CharacterSilhouetteGlyph] for Guess-the-Character). Leave `null` for a plain ring
 *  with no inner content, e.g. future Home hero / app icon usage.
 */
@Composable
fun PortalRingGlyph(
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    ringColor: Color = MaterialTheme.colorScheme.primary,
    glowColor: Color = RickAndMortyExtendedTheme.extendedColors.accent,
    content: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                } else {
                    hideFromAccessibility()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            // Soft glow: a multi-stop radial gradient standing in for a blurred glow, since
            // Compose Multiplatform has no cross-platform-consistent blur primitive to lean on
            // (see this composable's kdoc for the full cross-platform-consistency rationale).
            val glowRadius = this.size.minDimension * 0.85f
            drawCircle(
                brush = Brush.radialGradient(
                    0f to glowColor.copy(alpha = 0.55f),
                    0.5f to glowColor.copy(alpha = 0.22f),
                    1f to glowColor.copy(alpha = 0f),
                    center = center,
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = center,
            )

            val outerStrokeWidth = this.size.minDimension * 0.09f
            val outerRadius = this.size.minDimension / 2f - outerStrokeWidth
            drawArc(
                color = ringColor,
                startAngle = -50f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = outerStrokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2f, outerRadius * 2f),
            )

            val innerStrokeWidth = outerStrokeWidth * 0.55f
            val innerRadius = outerRadius - outerStrokeWidth * 1.4f
            drawArc(
                color = ringColor.copy(alpha = 0.6f),
                startAngle = 100f,
                sweepAngle = 230f,
                useCenter = false,
                style = Stroke(width = innerStrokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                size = Size(innerRadius * 2f, innerRadius * 2f),
            )
        }

        content?.invoke()
    }
}

/**
 * Inner glyph for [PortalRingGlyph] usages that represent Trivia Quiz (a question mark). Exposed
 * standalone so any future caller of [PortalRingGlyph] can reuse the exact same inner glyph
 * without duplicating it.
 */
@Composable
fun QuestionMarkGlyph(
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = "?",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}

/**
 * Inner glyph for [PortalRingGlyph] usages that represent Guess the Character (a simple head-and-
 * shoulders silhouette, not any specific character - deliberately generic since the actual round
 * always reveals a real, specific character elsewhere on screen). Canvas-drawn to match the
 * existing on-brand icon precedents in this module (see [CachedCharacterImage]'s
 * `PortalSignalLostIcon`) rather than pulling in an icon-pack dependency for one glyph.
 */
@Composable
fun CharacterSilhouetteGlyph(
    size: Dp = 22.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Head.
        drawCircle(
            color = color,
            radius = w * 0.19f,
            center = Offset(w * 0.5f, h * 0.28f),
        )

        // Shoulders/bust - a rounded trapezoid, clipped by the glyph's own bounds so it reads as
        // a bust rather than a full body.
        val shoulders = Path().apply {
            moveTo(w * 0.5f, h * 0.5f)
            cubicTo(w * 0.18f, h * 0.5f, w * 0.08f, h * 0.78f, w * 0.08f, h * 1f)
            lineTo(w * 0.92f, h * 1f)
            cubicTo(w * 0.92f, h * 0.78f, w * 0.82f, h * 0.5f, w * 0.5f, h * 0.5f)
            close()
        }
        drawPath(path = shoulders, color = color)
    }
}

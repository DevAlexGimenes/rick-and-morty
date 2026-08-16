package com.gimenes.alex.rickandmorty.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gimenes.alex.rickandmorty.core.designsystem.component.CharacterSilhouetteGlyph
import com.gimenes.alex.rickandmorty.core.designsystem.component.ModeCard
import com.gimenes.alex.rickandmorty.core.designsystem.component.PortalRingGlyph
import com.gimenes.alex.rickandmorty.core.designsystem.component.QuestionMarkGlyph
import com.gimenes.alex.rickandmorty.core.designsystem.theme.PortalRingShape
import com.gimenes.alex.rickandmorty.core.designsystem.theme.RickAndMortyExtendedTheme

/** Header glyph diameter for [ModeCard]'s compact header row per LUMA's R3 spec (~40-48dp). */
private val MODE_CARD_GLYPH_SIZE = 44.dp

/** Hero centerpiece glyph diameter per LUMA's R5 spec (~96-120dp). */
private val HERO_GLYPH_SIZE = 108.dp

/**
 * Test hooks for [com.gimenes.alex.rickandmorty.home.HomeHeroAccessibilityInstrumentedTest] (R5,
 * issue #41). [androidx.compose.ui.semantics.SemanticsProperties.TestTag] is a testing-only
 * semantics property - real accessibility services never read it - so tagging these purely
 * decorative nodes does not compromise their "hidden from accessibility" claim.
 */
internal const val HERO_GLYPH_TEST_TAG = "home_hero_glyph"
internal const val HERO_KICKER_TEST_TAG = "home_hero_kicker"

/**
 * Home: hero treatment (R5, issue #41), replacing the previous plain "title + subtitle" layout.
 * Top to bottom: a decorative kicker pill, a two-line headline (a plain line + a `primary`-accent
 * line with a soft glow behind it), a single in-voice tagline, a centerpiece [PortalRingGlyph],
 * and the two existing [ModeCard]s (unchanged from R3, just consumed here).
 *
 * Designed to fit without scrolling on standard phone sizes; [Modifier.verticalScroll] is kept
 * purely as a safety net for very short screens (LUMA's spec explicitly flags this as a fallback,
 * not a designed-in requirement) - if content already fits, there is nothing to scroll to.
 *
 * No idle/entrance animation is added here: acceptable per issue #41's scope (R6/R7 are the
 * dedicated motion issues for the Quiz/Guess-the-Character screens, not Home), so there is no
 * [com.gimenes.alex.rickandmorty.core.designsystem.motion.rememberReducedMotionEnabled] usage to
 * wire up in this file - the hero glyph and glow are fully static.
 */
@Composable
fun HomeScreen(
    onQuizClick: () -> Unit,
    onGuessCharacterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = RickAndMortyExtendedTheme.spacing
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.lg, vertical = spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            HeroKickerPill(text = "Multiverse Arcade")

            Text(
                text = "Rick and Morty",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            HeroAccentLine(text = "Mini Games")

            Text(
                text = "Step through the portal. Prove you know the multiverse.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            PortalRingGlyph(
                size = HERO_GLYPH_SIZE,
                modifier = Modifier
                    .padding(vertical = spacing.sm)
                    .testTag(HERO_GLYPH_TEST_TAG),
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                ModeCard(
                    title = "Trivia Quiz",
                    description = "Answer questions about the multiverse.",
                    onClick = onQuizClick,
                    icon = {
                        PortalRingGlyph(
                            size = MODE_CARD_GLYPH_SIZE,
                            content = { QuestionMarkGlyph() },
                        )
                    }
                )
                ModeCard(
                    title = "Guess the Character",
                    description = "Identify characters from the show.",
                    onClick = onGuessCharacterClick,
                    icon = {
                        PortalRingGlyph(
                            size = MODE_CARD_GLYPH_SIZE,
                            content = { CharacterSilhouetteGlyph() },
                        )
                    }
                )
            }
        }
    }
}

/**
 * Small rounded-pill outline in `primary`, a tiny decorative dot glyph + short branding label.
 * Purely decorative branding, not meaningful content - explicitly cleared from the accessibility
 * tree with [clearAndSetSemantics] (an empty properties block) rather than left to whatever
 * default semantics merging would otherwise expose, per LUMA's spec. See
 * [com.gimenes.alex.rickandmorty.home.HomeHeroAccessibilityInstrumentedTest] for the semantics-
 * tree verification of this claim (not just an assumption that it holds).
 */
@Composable
private fun HeroKickerPill(text: String) {
    val primary = MaterialTheme.colorScheme.primary
    val spacing = RickAndMortyExtendedTheme.spacing
    Row(
        modifier = Modifier
            .testTag(HERO_KICKER_TEST_TAG)
            .clearAndSetSemantics {}
            .border(BorderStroke(1.dp, primary), shape = PortalRingShape)
            .padding(horizontal = spacing.md, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = primary)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The headline's accent line: `primary`-green text (verified-contrast
 * [RickAndMortyExtendedTheme.extendedColors.heroAccentText], NOT `accent` - see that field's kdoc
 * for why `accent` itself must not be used as running text fill) with a soft `accent`-tinted glow
 * drawn as a separate [Canvas] layer *behind* the text, not baked into the text's own fill color.
 * Same manually-drawn-radial-gradient technique as [PortalRingGlyph]'s glow, for the same
 * cross-platform-consistency reasons (see that composable's kdoc).
 */
@Composable
private fun HeroAccentLine(text: String) {
    val glowColor = RickAndMortyExtendedTheme.extendedColors.accent
    val textColor = RickAndMortyExtendedTheme.extendedColors.heroAccentText
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val glowRadius = size.width.coerceAtLeast(size.height) * 0.9f
            drawCircle(
                brush = Brush.radialGradient(
                    0f to glowColor.copy(alpha = 0.45f),
                    0.5f to glowColor.copy(alpha = 0.18f),
                    1f to glowColor.copy(alpha = 0f),
                    center = center,
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = center,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}

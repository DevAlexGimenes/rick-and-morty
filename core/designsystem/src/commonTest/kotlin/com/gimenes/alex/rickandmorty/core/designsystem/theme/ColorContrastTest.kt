package com.gimenes.alex.rickandmorty.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * WCAG 2.x AA contrast verification for every foreground/background token pairing that is
 * actually used as text- or icon-content-on-fill in the app (issue #37 - R1 theme color tokens).
 *
 * Threshold used: 4.5:1 (the "normal text" AA threshold) for every pairing checked here, even
 * though some of these (button labels, the A/B/C/D answer badges, feedback tags) render at
 * Material3's `labelLarge`/`titleMedium` scale rather than large headline text. `labelLarge` at
 * 14sp with default (Medium) weight does not reliably clear WCAG's "large text" exemption (18pt
 * regular / 14pt *bold*), so this test deliberately holds every pairing to the stricter 4.5:1
 * bar rather than assuming the 3:1 large-text/UI-component exemption applies.
 *
 * Where a token in [Color.kt][com.gimenes.alex.rickandmorty.core.designsystem.theme] measurably
 * failed this bar, the hex was darkened slightly from LUMA's original design-intent value - see
 * the kdoc on the individual `val`s in Color.kt for the before/after value and rationale per
 * token. This test is what proves those adjustments were sufficient (and will catch any future
 * regression back below the original failing value).
 */
class ColorContrastTest {

    private fun relativeLuminance(color: Color): Double {
        fun channel(c: Float): Double {
            val cd = c.toDouble()
            return if (cd <= 0.04045) cd / 12.92 else ((cd + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    /** WCAG contrast ratio, always >= 1.0, order of arguments doesn't matter. */
    private fun contrastRatio(a: Color, b: Color): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val lighter = max(la, lb)
        val darker = min(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** WCAG AA threshold for normal-size text (see class kdoc for why this is used uniformly). */
    private val minNormalTextContrast = 4.5

    private fun assertPairPassesAa(name: String, foreground: Color, background: Color) {
        val ratio = contrastRatio(foreground, background)
        assertTrue(
            ratio >= minNormalTextContrast,
            "$name: contrast ratio $ratio is below the WCAG AA normal-text threshold " +
                "of $minNormalTextContrast:1 (foreground=$foreground, background=$background)"
        )
    }

    // ---- Dark theme ----

    @Test
    fun darkOnPrimaryOnPrimaryMeetsAa() =
        assertPairPassesAa("dark onPrimary/primary", OnPrimaryDark, PrimaryDark)

    @Test
    fun darkOnSecondaryOnSecondaryMeetsAa() =
        assertPairPassesAa("dark onSecondary/secondary", OnSecondaryDark, SecondaryDark)

    @Test
    fun darkOnTertiaryOnTertiaryMeetsAa() =
        assertPairPassesAa("dark onTertiary/tertiary", OnTertiaryDark, TertiaryDark)

    @Test
    fun darkOnBackgroundOnBackgroundMeetsAa() =
        assertPairPassesAa("dark onBackground/background", OnBackgroundDark, BackgroundDark)

    @Test
    fun darkOnSurfaceOnSurfaceMeetsAa() =
        assertPairPassesAa("dark onSurface/surface", OnSurfaceDark, SurfaceDark)

    @Test
    fun darkOnSurfaceVariantOnSurfaceVariantMeetsAa() =
        assertPairPassesAa(
            "dark onSurfaceVariant(=onBackground)/surfaceVariant",
            OnBackgroundDark,
            SurfaceVariantDark
        )

    @Test
    fun darkAccentOnBackgroundMeetsAaForLinkText() =
        assertPairPassesAa("dark accent/background (link-style CTA text)", AccentDark, BackgroundDark)

    @Test
    fun darkHeroAccentTextOnBackgroundMeetsAa() =
        // Dark mode reuses PrimaryDark directly (see ExtendedColors.heroAccentText kdoc) - this
        // pins that it stays valid rather than only relying on the already-tested onPrimary/
        // primary pairing, which is a different pairing (white-on-fill, not primary-as-text-on-
        // background).
        assertPairPassesAa(
            "dark heroAccentText(=PrimaryDark)/background (R5 hero headline accent line)",
            PrimaryDark,
            BackgroundDark
        )

    @Test
    fun darkOnFeedbackCorrectOnFeedbackCorrectMeetsAa() =
        assertPairPassesAa(
            "dark onFeedbackCorrect/feedbackCorrect",
            OnFeedbackCorrectDark,
            FeedbackCorrectDark
        )

    @Test
    fun darkOnFeedbackIncorrectOnFeedbackIncorrectMeetsAa() =
        assertPairPassesAa(
            "dark onFeedbackIncorrect/feedbackIncorrect",
            OnFeedbackIncorrectDark,
            FeedbackIncorrectDark
        )

    // ---- Light theme ----

    @Test
    fun lightOnPrimaryOnPrimaryMeetsAa() =
        // Deliberate polarity flip - see PrimaryLight/OnPrimaryLight kdoc.
        assertPairPassesAa("light onPrimary/primary", OnPrimaryLight, PrimaryLight)

    @Test
    fun lightOnSecondaryOnSecondaryMeetsAa() =
        assertPairPassesAa("light onSecondary/secondary", OnSecondaryLight, SecondaryLight)

    @Test
    fun lightOnTertiaryOnTertiaryMeetsAa() =
        // Deliberate polarity flip - see TertiaryLight/OnTertiaryLight kdoc.
        assertPairPassesAa("light onTertiary/tertiary", OnTertiaryLight, TertiaryLight)

    @Test
    fun lightOnBackgroundOnBackgroundMeetsAa() =
        assertPairPassesAa("light onBackground/background", OnBackgroundLight, BackgroundLight)

    @Test
    fun lightOnSurfaceOnSurfaceMeetsAa() =
        assertPairPassesAa("light onSurface/surface", OnSurfaceLight, SurfaceLight)

    @Test
    fun lightOnSurfaceVariantOnSurfaceVariantMeetsAa() =
        assertPairPassesAa(
            "light onSurfaceVariant(=onBackground)/surfaceVariant",
            OnBackgroundLight,
            SurfaceVariantLight
        )

    @Test
    fun lightAccentOnBackgroundMeetsAaForLinkText() =
        assertPairPassesAa("light accent/background (link-style CTA text)", AccentLight, BackgroundLight)

    @Test
    fun lightHeroAccentTextOnBackgroundMeetsAa() =
        // See HeroAccentTextLight kdoc: PrimaryLight itself only reaches ~4.25:1 against
        // BackgroundLight (fails AA) despite passing against pure white, hence the dedicated
        // darker token for this specific text-on-background use (R5's hero headline accent line).
        assertPairPassesAa(
            "light heroAccentText/background (R5 hero headline accent line)",
            HeroAccentTextLight,
            BackgroundLight
        )

    @Test
    fun lightOnFeedbackCorrectOnFeedbackCorrectMeetsAa() =
        assertPairPassesAa(
            "light onFeedbackCorrect/feedbackCorrect",
            OnFeedbackCorrectLight,
            FeedbackCorrectLight
        )

    @Test
    fun lightOnFeedbackIncorrectOnFeedbackIncorrectMeetsAa() =
        assertPairPassesAa(
            "light onFeedbackIncorrect/feedbackIncorrect",
            OnFeedbackIncorrectLight,
            FeedbackIncorrectLight
        )
}

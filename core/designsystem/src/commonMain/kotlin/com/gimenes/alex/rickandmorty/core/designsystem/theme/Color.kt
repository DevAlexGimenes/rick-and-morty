package com.gimenes.alex.rickandmorty.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Rick and Morty brand palette (R1 design-refresh pass, issue #37).
 *
 * Grounded in actual show colors rather than a generic sci-fi palette:
 * - primary   -> interdimensional portal swirl green
 * - secondary -> Rick's turquoise shirt
 * - tertiary  -> Morty's yellow t-shirt
 * - background-> a true night-sky navy/violet (dark) / cool near-white (light) - light mode is
 *   a genuine re-pass in the same hue family as dark, not a dark-mode inversion.
 *
 * Feedback colors (correct/incorrect) are deliberately NOT derived from the brand green so
 * that "correct" never reads as "just on-brand" - see [FeedbackCorrectDark]/[FeedbackIncorrectDark].
 *
 * All foreground/background pairings below that are used as text/icon-content-on-fill in the
 * app are verified for WCAG AA contrast (4.5:1) by
 * core/designsystem/src/commonTest/.../theme/ColorContrastTest.kt. Several of LUMA's proposed
 * hex values (issue #37) measurably failed that bar and were darkened slightly from the
 * original design-intent value; each such deviation is called out below with the original
 * value and the reason. Hue/saturation were preserved and only lightness was reduced, so the
 * deviations read as "a slightly deeper version of the same color", not a different color.
 */

// Dark theme
val PrimaryDark = Color(0xFF8FDB4A)
val OnPrimaryDark = Color(0xFF10210F)
val SecondaryDark = Color(0xFF6AD9EA)
val OnSecondaryDark = Color(0xFF0E1020)
val TertiaryDark = Color(0xFFFFCB3D)
val OnTertiaryDark = Color(0xFF0E1020)
val BackgroundDark = Color(0xFF14152B)
val OnBackgroundDark = Color(0xFFF6F3EC)
val SurfaceDark = Color(0xFF1C1E3D)
val OnSurfaceDark = Color(0xFFF6F3EC)
val SurfaceVariantDark = Color(0xFF262A4A)
val OutlineDark = Color(0xFF454A72)

/**
 * Portal-glow accent. Redefined in R1 (issue #37) from a muted tan (`#C9A87C`) to a vivid
 * portal-glow green.
 *
 * SCOPE CONSTRAINT: this token is reserved for glow effects, gradient stops, badges/pills, and
 * link-style CTA text ONLY. It must NEVER be used as a large flat fill or as body text color -
 * at full saturation/lightness it reads as near-fluorescent and is not intended to pass
 * "comfortable to stare at as a large surface" the way primary/secondary/tertiary are. Future
 * redesign issues (R3/R5/R6/R7) consuming this token must respect that constraint.
 */
val AccentDark = Color(0xFF3DFF7A)
val FeedbackCorrectDark = Color(0xFF19855D)
val OnFeedbackCorrectDark = Color(0xFFFFFFFF)
val FeedbackIncorrectDark = Color(0xFFDF2E12)
val OnFeedbackIncorrectDark = Color(0xFFFFFFFF)

// Light theme - re-passed for R1 (issue #37), not derived from the dark values by inversion.
/**
 * DELIBERATE POLARITY FLIP (issue #37): the light-mode primary is now dark/saturated enough
 * that WHITE text has higher contrast against it than the previous dark [OnPrimaryLight] did.
 * Do not "fix" this back to a dark onPrimary - white-on-primary is the correct, verified pairing.
 *
 * Contrast-driven deviation: LUMA's proposed value was `#4C9A22`, which only reaches ~3.53:1
 * against white (fails the 4.5:1 text threshold - see ColorContrastTest). Darkened (same hue/
 * saturation, lower lightness) to `#41841D` (~4.62:1) to pass WCAG AA for white button/badge text.
 */
val PrimaryLight = Color(0xFF41841D)
val OnPrimaryLight = Color(0xFFFFFFFF)

/**
 * Contrast-driven deviation: LUMA's proposed value was `#1C8FA3`, which only reaches ~3.81:1
 * against white onSecondary (fails 4.5:1). Darkened to `#198092` (~4.62:1).
 */
val SecondaryLight = Color(0xFF198092)
val OnSecondaryLight = Color(0xFFFFFFFF)

/**
 * DELIBERATE POLARITY FLIP (issue #37): same reasoning as [PrimaryLight] - the light-mode
 * tertiary is now dark/saturated enough that WHITE text has higher contrast against it than the
 * previous dark [OnTertiaryLight] did. Do not "fix" this back.
 *
 * Contrast-driven deviation: LUMA's proposed value was `#C08A00`, which only reaches ~3.05:1
 * against white (fails 4.5:1). Darkened to `#986D00` (~4.64:1).
 */
val TertiaryLight = Color(0xFF986D00)
val OnTertiaryLight = Color(0xFFFFFFFF)
val BackgroundLight = Color(0xFFF4F5FC)
val OnBackgroundLight = Color(0xFF14152B)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF14152B)
val SurfaceVariantLight = Color(0xFFE6E4F5)
val OutlineLight = Color(0xFFC7C4DE)

/**
 * Portal-glow accent (light mode). Same scope constraint as [AccentDark]: glow/gradient/badge/
 * link-CTA-text only, never a large flat fill or body text color.
 *
 * Contrast-driven deviation: LUMA's proposed value was `#1E9E3F`, which only reaches ~3.21:1
 * when used as link-style CTA text against [BackgroundLight] (fails 4.5:1). Darkened to
 * `#188033` (~4.63:1).
 */
val AccentLight = Color(0xFF188033)

/**
 * Contrast-driven deviation: LUMA's spec called for this to stay at its pre-R1 value (`#1B8F63`,
 * unchanged), but that value only reaches ~4.07:1 against white [OnFeedbackCorrectLight] (fails
 * 4.5:1) - it was already flagged as unverified before this issue. Darkened to `#19855C`
 * (~4.61:1) rather than shipping a token that measurably fails contrast.
 */
val FeedbackCorrectLight = Color(0xFF19855C)
val OnFeedbackCorrectLight = Color(0xFFFFFFFF)

// Unchanged from pre-R1: already passes WCAG AA (~4.86:1 against white) - see ColorContrastTest.
val FeedbackIncorrectLight = Color(0xFFC7452F)
val OnFeedbackIncorrectLight = Color(0xFFFFFFFF)

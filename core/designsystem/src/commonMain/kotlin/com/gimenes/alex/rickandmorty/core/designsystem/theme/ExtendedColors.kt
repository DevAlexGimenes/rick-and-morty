package com.gimenes.alex.rickandmorty.core.designsystem.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Brand tokens that don't map onto Material3's [androidx.compose.material3.ColorScheme] slots:
 * the streak/highlight accent, the colorblind-safe correct/incorrect feedback pair, and the
 * hero-headline accent-text color.
 *
 * Feedback colors are intentionally separate from primary/secondary/tertiary so quiz feedback
 * never gets confused with "on-brand" styling.
 *
 * [accent] scope constraint (issue #37): reserved for glow effects, gradient stops,
 * badges/pills, and link-style CTA text ONLY - never a large flat fill or body text color. See
 * [AccentDark]/[AccentLight] kdoc for the full rationale.
 *
 * [heroAccentText] (issue #41): the verified-contrast color for `primary`-green text rendered
 * directly on [BackgroundLight]/[BackgroundDark] (e.g. Home's hero headline accent line) - see
 * [HeroAccentTextLight] kdoc for why this needs its own token rather than reusing [PrimaryLight]
 * directly in light mode.
 */
data class ExtendedColors(
    val accent: Color,
    val feedbackCorrect: Color,
    val onFeedbackCorrect: Color,
    val feedbackIncorrect: Color,
    val onFeedbackIncorrect: Color,
    val heroAccentText: Color
)

val DarkExtendedColors = ExtendedColors(
    accent = AccentDark,
    feedbackCorrect = FeedbackCorrectDark,
    onFeedbackCorrect = OnFeedbackCorrectDark,
    feedbackIncorrect = FeedbackIncorrectDark,
    onFeedbackIncorrect = OnFeedbackIncorrectDark,
    heroAccentText = PrimaryDark
)

val LightExtendedColors = ExtendedColors(
    accent = AccentLight,
    feedbackCorrect = FeedbackCorrectLight,
    onFeedbackCorrect = OnFeedbackCorrectLight,
    feedbackIncorrect = FeedbackIncorrectLight,
    onFeedbackIncorrect = OnFeedbackIncorrectLight,
    heroAccentText = HeroAccentTextLight
)

val LocalExtendedColors = compositionLocalOf { DarkExtendedColors }

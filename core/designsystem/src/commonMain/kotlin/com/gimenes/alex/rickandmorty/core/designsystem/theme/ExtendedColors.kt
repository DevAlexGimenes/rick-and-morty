package com.gimenes.alex.rickandmorty.core.designsystem.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Brand tokens that don't map onto Material3's [androidx.compose.material3.ColorScheme] slots:
 * the streak/highlight accent and the colorblind-safe correct/incorrect feedback pair.
 *
 * Feedback colors are intentionally separate from primary/secondary/tertiary so quiz feedback
 * never gets confused with "on-brand" styling.
 */
data class ExtendedColors(
    val accent: Color,
    val feedbackCorrect: Color,
    val onFeedbackCorrect: Color,
    val feedbackIncorrect: Color,
    val onFeedbackIncorrect: Color
)

val DarkExtendedColors = ExtendedColors(
    accent = AccentDark,
    feedbackCorrect = FeedbackCorrectDark,
    onFeedbackCorrect = OnFeedbackCorrectDark,
    feedbackIncorrect = FeedbackIncorrectDark,
    onFeedbackIncorrect = OnFeedbackIncorrectDark
)

val LightExtendedColors = ExtendedColors(
    accent = AccentLight,
    feedbackCorrect = FeedbackCorrectLight,
    onFeedbackCorrect = OnFeedbackCorrectLight,
    feedbackIncorrect = FeedbackIncorrectLight,
    onFeedbackIncorrect = OnFeedbackIncorrectLight
)

val LocalExtendedColors = compositionLocalOf { DarkExtendedColors }

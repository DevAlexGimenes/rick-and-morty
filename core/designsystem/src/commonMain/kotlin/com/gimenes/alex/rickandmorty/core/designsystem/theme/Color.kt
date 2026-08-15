package com.gimenes.alex.rickandmorty.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Rick and Morty brand palette.
 *
 * Grounded in actual show colors rather than a generic sci-fi palette:
 * - primary   -> interdimensional portal swirl green
 * - secondary -> Rick's turquoise shirt
 * - tertiary  -> Morty's yellow t-shirt
 * - background-> the show's recurring night-sky navy (dark) / warm off-white (light)
 *
 * Feedback colors (correct/incorrect) are deliberately NOT derived from the brand green so
 * that "correct" never reads as "just on-brand" - see [FeedbackCorrectDark]/[FeedbackIncorrectDark].
 *
 * Light-mode hex values are LUMA's best-estimate deepened values for WCAG AA contrast against
 * their paired surface; they still need to be run through an automated contrast check before
 * being treated as final (flagged by LUMA, not yet independently re-verified here).
 */

// Dark theme
val PrimaryDark = Color(0xFF97CE4C)
val OnPrimaryDark = Color(0xFF0B1A2B)
val SecondaryDark = Color(0xFF7FD1E0)
val OnSecondaryDark = Color(0xFF0B1A2B)
val TertiaryDark = Color(0xFFF5C518)
val OnTertiaryDark = Color(0xFF0B1A2B)
val BackgroundDark = Color(0xFF0B1A2B)
val OnBackgroundDark = Color(0xFFF3F1E7)
val SurfaceDark = Color(0xFF152943)
val OnSurfaceDark = Color(0xFFF3F1E7)
val SurfaceVariantDark = Color(0xFF2C3E56)
val OutlineDark = Color(0xFF5C7085)
val AccentDark = Color(0xFFC9A87C)
val FeedbackCorrectDark = Color(0xFF20A876)
val OnFeedbackCorrectDark = Color(0xFFFFFFFF)
val FeedbackIncorrectDark = Color(0xFFE1523D)
val OnFeedbackIncorrectDark = Color(0xFFFFFFFF)

// Light theme
val PrimaryLight = Color(0xFF7BB13C)
val OnPrimaryLight = Color(0xFF0B1A2B)
val SecondaryLight = Color(0xFF3E9AAD)
val OnSecondaryLight = Color(0xFFFFFFFF)
val TertiaryLight = Color(0xFFC99A00)
val OnTertiaryLight = Color(0xFF0B1A2B)
val BackgroundLight = Color(0xFFF3F1E7)
val OnBackgroundLight = Color(0xFF0B1A2B)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF0B1A2B)
val SurfaceVariantLight = Color(0xFFE3DFC8)
val OutlineLight = Color(0xFFB7AE93)
val AccentLight = Color(0xFFA9835A)
val FeedbackCorrectLight = Color(0xFF1B8F63)
val OnFeedbackCorrectLight = Color(0xFFFFFFFF)
val FeedbackIncorrectLight = Color(0xFFC7452F)
val OnFeedbackIncorrectLight = Color(0xFFFFFFFF)

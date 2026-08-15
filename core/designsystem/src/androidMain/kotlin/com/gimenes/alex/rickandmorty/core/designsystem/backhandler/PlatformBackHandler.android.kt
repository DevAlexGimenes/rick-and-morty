package com.gimenes.alex.rickandmorty.core.designsystem.backhandler

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Delegates directly to `androidx.activity.compose.BackHandler`, which intercepts both the
 * legacy hardware/software back button and the gesture-navigation back swipe on Android.
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

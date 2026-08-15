package com.gimenes.alex.rickandmorty.core.designsystem.backhandler

import androidx.compose.runtime.Composable

// TODO(ios-back-handler): iOS has no system-level "back" gesture/button equivalent to Android's
//  back button/predictive-back gesture to intercept in the first place - iOS navigation is
//  driven entirely by in-screen UI (e.g. a nav bar back button, or an edge-swipe gesture that's
//  owned by whatever container is hosting this Compose content, not by this screen's own code).
//  This is a genuine platform difference, not a missing feature: there is currently nothing for
//  this to do on iOS, so it's a no-op - [enabled] and [onBack] are intentionally unused. If this
//  project later hosts Guess the Character inside a native UINavigationController (or another
//  container with its own interactive-pop gesture) and needs to guard *that* gesture with the
//  same exit confirmation, this actual is where that platform-specific interception would go -
//  following the same honesty-about-platform-gaps pattern as the reduced-motion TODOs elsewhere
//  in this codebase (see GuessCharacterViewModel's kdoc).
@Suppress("UNUSED_PARAMETER")
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op on iOS - see file-level TODO above.
}

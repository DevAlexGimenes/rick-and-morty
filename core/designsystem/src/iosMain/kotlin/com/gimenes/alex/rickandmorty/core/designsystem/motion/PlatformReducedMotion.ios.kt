package com.gimenes.alex.rickandmorty.core.designsystem.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

// TODO(ios-reduced-motion): iOS does expose an equivalent signal -
//  `platform.UIKit.UIAccessibility.isReduceMotionEnabled`, updated via the
//  `UIAccessibilityReduceMotionStatusDidChangeNotification` notification - but wiring that
//  Kotlin/Native interop (plus the notification-observer plumbing needed to match the Android
//  actual's "takes effect on the very next transition without restarting the app" behavior) is
//  out of scope for this pass. This is a documented platform gap, not a missing feature - see
//  PlatformBackHandler's iOS actual for the same honesty-about-platform-gaps precedent this
//  follows. Always reports "motion enabled" (`false`) until this is wired up.
@Composable
actual fun rememberReducedMotionEnabled(): State<Boolean> {
    return remember { mutableStateOf(false) }
}

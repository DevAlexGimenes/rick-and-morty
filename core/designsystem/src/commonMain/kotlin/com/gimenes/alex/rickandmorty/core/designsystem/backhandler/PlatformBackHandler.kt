package com.gimenes.alex.rickandmorty.core.designsystem.backhandler

import androidx.compose.runtime.Composable

/**
 * Intercepts the platform's system/hardware back gesture (Android's back button/gesture; no
 * equivalent system-level gesture exists on iOS) so a screen can run its own logic - e.g. an
 * "are you sure?" confirmation - instead of silently letting the navigation back stack pop.
 *
 * This exists because Compose Multiplatform's `navigation-compose` doesn't (as of this project's
 * pinned version, see `navigationCompose` in the version catalog) expose a uniform cross-platform
 * way to intercept the *system* back action the way `androidx.activity.compose.BackHandler` does
 * on Android - only in-screen UI (buttons) can be trivially made cross-platform. See the
 * `.android.kt`/`.ios.kt` actuals for what's actually done per platform, and
 * [com.gimenes.alex.rickandmorty.feature.guesscharacter.GuessCharacterScreen]'s use of this for
 * why it's needed: without it, the system back button bypassed that screen's mid-round exit
 * confirmation entirely, silently discarding an active streak.
 *
 * @param enabled whether the system back action should currently be intercepted. When `false`,
 *  the system back action falls through to its normal behavior (e.g. popping the nav back stack)
 *  exactly as if this handler weren't present.
 * @param onBack invoked instead of the normal system back behavior when [enabled] is `true`.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

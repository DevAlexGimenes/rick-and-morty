package com.gimenes.alex.rickandmorty.core.designsystem.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * Whether the platform currently signals a user preference for reduced motion, so a caller can
 * swap a decorative animation (a crossfade, a slide, a screen transition, ...) for its
 * near-instant/static equivalent instead.
 *
 * This exists for the same reason [com.gimenes.alex.rickandmorty.core.designsystem.backhandler.PlatformBackHandler]
 * does: Compose Multiplatform doesn't (as of this project's pinned Compose Multiplatform version)
 * expose a uniform cross-platform reduced-motion signal the way Android's
 * `Settings.Global.ANIMATOR_DURATION_SCALE` does - see the `.android.kt`/`.ios.kt` actuals for
 * what's actually available per platform. This resolves the gap previously left as a `TODO` on
 * [com.gimenes.alex.rickandmorty.feature.guesscharacter.GuessCharacterScreen]'s round-transition
 * `Crossfade` (see that file's history) for Android, where a real signal turned out to exist; iOS
 * remains a documented no-op, following the same honesty-about-platform-gaps precedent as
 * [com.gimenes.alex.rickandmorty.core.designsystem.backhandler.PlatformBackHandler]'s iOS actual.
 *
 * Returned as a [State] (rather than a plain `Boolean`) so callers building non-`@Composable`
 * closures around it (e.g. `navigation-compose`'s per-destination `enterTransition`/`exitTransition`
 * lambdas, which are evaluated at navigation time, not at composition time) can read the *current*
 * value on every invocation instead of capturing a value that's stale the moment the platform
 * setting changes.
 */
@Composable
expect fun rememberReducedMotionEnabled(): State<Boolean>

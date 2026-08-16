package com.gimenes.alex.rickandmorty.core.designsystem.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

/**
 * Reduced-motion-aware building blocks for the small, repeated "press/lock/reveal" animation
 * moments used by the game screens (Trivia Quiz - issue #42/R6 - and, per that issue's kdoc,
 * intended for reuse by Guess the Character's near-identical R7 follow-up). None of these decide
 * *whether* to reduce motion themselves - every one takes a `reducedMotion: Boolean` (read once
 * per screen via [rememberReducedMotionEnabled] and threaded down), so callers stay in control of
 * where that read happens, matching the rest of this project's reduced-motion call sites
 * (`App.kt`'s nav transitions, `GuessCharacterScreen`'s round `Crossfade`).
 */

/**
 * A [FiniteAnimationSpec] that's an ordinary [tween] under normal motion, or an instant [snap]
 * under [reducedMotion] - the single place the "instant swap, exactly today's shipped behavior"
 * reduced-motion fallback required throughout issue #42 is expressed, so every color/float tween
 * in a caller doesn't have to re-derive its own `if (reducedMotion) snap() else tween(...)`.
 */
fun <T> reducedMotionAwareSpec(
    reducedMotion: Boolean,
    durationMillis: Int,
    easing: Easing = FastOutSlowInEasing,
): FiniteAnimationSpec<T> = if (reducedMotion) snap() else tween(durationMillis, easing = easing)

/**
 * A spring-driven float that moves to [pressedValue] while [interactionSource] reports a press,
 * and back to [restValue] otherwise - e.g. an answer row's 97% press scale, or a press-triggered
 * glow bloom's alpha (issue #42 moment 2). Under [reducedMotion] the target never leaves
 * [restValue], so the value never departs from rest at all - matching the "default state-layer/
 * ripple only, no scale/glow" reduced-motion fallback (not just a faster version of the effect).
 */
@Composable
fun rememberPressAnimatedFloat(
    interactionSource: InteractionSource,
    reducedMotion: Boolean,
    pressedValue: Float,
    restValue: Float = 1f,
): State<Float> {
    val isPressed by interactionSource.collectIsPressedAsState()
    val targetValue = if (!reducedMotion && isPressed) pressedValue else restValue
    return animateFloatAsState(
        targetValue = targetValue,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "press-animated-float",
    )
}

/**
 * A one-shot "pop" - scale ramps from 1f up to [peakScale] and back to 1f over [durationMillis]
 * (split evenly between the two legs) - fired every time [trigger] flips from `false` to `true`.
 * Used for issue #42 moment 3 (an answer row's 80ms lock-in pop to 103%) and moment 4 (a feedback
 * badge's 250ms reveal flourish to 115%), which are otherwise identical "pop once on this boolean
 * becoming true" effects at different scales/durations.
 *
 * Under [reducedMotion] (or while [trigger] is `false`) the scale is held/snapped at `1f` - the
 * "instant state change, no pop" fallback, not a shorter pop.
 */
@Composable
fun rememberScalePop(
    trigger: Boolean,
    reducedMotion: Boolean,
    peakScale: Float,
    durationMillis: Int,
): State<Float> {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(trigger, reducedMotion) {
        if (!trigger || reducedMotion) {
            scale.snapTo(1f)
            return@LaunchedEffect
        }
        val halfDuration = durationMillis / 2
        scale.animateTo(peakScale, tween(halfDuration, easing = LinearOutSlowInEasing))
        scale.animateTo(1f, tween(halfDuration, easing = FastOutLinearInEasing))
    }
    // Animatable doesn't itself implement State<T> - derivedStateOf adapts its snapshot-backed
    // `.value` into a proper State so callers can use the familiar `by rememberScalePop(...)`
    // delegate syntax.
    return remember { derivedStateOf { scale.value } }
}

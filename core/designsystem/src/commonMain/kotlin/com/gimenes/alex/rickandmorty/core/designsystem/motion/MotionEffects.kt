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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Reduced-motion-aware building blocks for the small, repeated "press/lock/reveal" animation
 * moments used by the game screens (Trivia Quiz - issue #42/R6 - and, per that issue's kdoc,
 * reused by Guess the Character's near-identical R7 follow-up, which also added
 * [rememberValuePop] and [rememberKeyedValuePop] here rather than duplicating logic locally - see
 * their kdoc for the two gaps R6's original [rememberScalePop]/[rememberPressAnimatedFloat]
 * didn't cover). None of these decide *whether* to reduce motion themselves - every one takes a
 * `reducedMotion: Boolean` (read once per screen via [rememberReducedMotionEnabled] and threaded
 * down), so callers stay in control of where that read happens, matching the rest of this
 * project's reduced-motion call sites (`App.kt`'s nav transitions, `GuessCharacterScreen`'s round
 * `Crossfade`).
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
 * A one-shot "pop" - the value ramps from [baseValue] up to [peakValue] and back to [baseValue]
 * over [durationMillis] (split evenly between the two legs) - fired every time [trigger] flips
 * from `false` to `true`. The base/peak generalization of what issue #42 originally shipped as
 * [rememberScalePop] (which always popped a *scale* from 1f): issue #47/R7's Guess the Character
 * portal-frame intensify (moment 2) needed the identical one-shot up/down shape for a *glow alpha*
 * popping from `0f` to `1f`, not a scale from `1f`, so this is the shared shape both now sit on
 * top of - see [rememberScalePop]'s kdoc for why that function still exists as a thin wrapper
 * rather than being replaced at its call sites.
 *
 * Under [reducedMotion] (or while [trigger] is `false`) the value is held/snapped at [baseValue] -
 * the "instant state change, no pop" fallback, not a shorter pop.
 */
@Composable
fun rememberValuePop(
    trigger: Boolean,
    reducedMotion: Boolean,
    baseValue: Float,
    peakValue: Float,
    durationMillis: Int,
): State<Float> {
    val value = remember { Animatable(baseValue) }
    LaunchedEffect(trigger, reducedMotion) {
        if (!trigger || reducedMotion) {
            value.snapTo(baseValue)
            return@LaunchedEffect
        }
        val halfDuration = durationMillis / 2
        value.animateTo(peakValue, tween(halfDuration, easing = LinearOutSlowInEasing))
        value.animateTo(baseValue, tween(halfDuration, easing = FastOutLinearInEasing))
    }
    // Animatable doesn't itself implement State<T> - derivedStateOf adapts its snapshot-backed
    // `.value` into a proper State so callers can use the familiar `by rememberValuePop(...)`
    // delegate syntax.
    return remember { derivedStateOf { value.value } }
}

/**
 * A one-shot scale "pop" from 1f up to [peakScale] and back to 1f - the original issue #42 shape,
 * kept as its own function (rather than requiring every existing call site to spell out
 * `baseValue = 1f`) since a *scale* popping from its natural resting value of 1f is by far the
 * most common case ([QuestionContent]'s answer-row lock-in pop, its feedback badge's reveal pop).
 * See [rememberValuePop] for the general form this now delegates to.
 */
@Composable
fun rememberScalePop(
    trigger: Boolean,
    reducedMotion: Boolean,
    peakScale: Float,
    durationMillis: Int,
): State<Float> = rememberValuePop(
    trigger = trigger,
    reducedMotion = reducedMotion,
    baseValue = 1f,
    peakValue = peakScale,
    durationMillis = durationMillis,
)

/**
 * A one-shot "pop" from [baseValue] to [peakValue] and back, fired whenever [key] changes to a
 * *new* value - not on the composable's first composition with that key, and not by comparing a
 * boolean's own false/true transition the way [rememberValuePop]/[rememberScalePop] do.
 *
 * This exists for R7's streak-counter celebration (moment 3): [rememberValuePop]'s boolean-trigger
 * shape works when the caller has some other state that naturally cycles back to `false` between
 * occurrences (e.g. Trivia's/Guess the Character's `state.isLocked`, which resets to `false` every
 * time a fresh question/round begins). A running streak count has no such reset - it only ever
 * increases for the lifetime of a run - so there's no `false` state to re-arm a boolean trigger
 * between one correct answer and the next. Popping on *key identity change* instead (each new
 * streak value is a new [key]) sidesteps that entirely: every distinct value fires its own pop,
 * with no extra "did this already fire" bookkeeping required of the caller.
 *
 * Under [reducedMotion] the value is held/snapped at [baseValue], matching [rememberValuePop]'s
 * fallback.
 */
@Composable
fun rememberKeyedValuePop(
    key: Any?,
    reducedMotion: Boolean,
    baseValue: Float,
    peakValue: Float,
    durationMillis: Int,
): State<Float> {
    val value = remember { Animatable(baseValue) }
    val previousKey = remember { mutableStateOf(key) }
    val hasComposedBefore = remember { mutableStateOf(false) }
    LaunchedEffect(key, reducedMotion) {
        val isNewValue = hasComposedBefore.value && previousKey.value != key
        previousKey.value = key
        hasComposedBefore.value = true
        if (!isNewValue || reducedMotion) {
            value.snapTo(baseValue)
            return@LaunchedEffect
        }
        val halfDuration = durationMillis / 2
        value.animateTo(peakValue, tween(halfDuration, easing = LinearOutSlowInEasing))
        value.animateTo(baseValue, tween(halfDuration, easing = FastOutLinearInEasing))
    }
    return remember { derivedStateOf { value.value } }
}

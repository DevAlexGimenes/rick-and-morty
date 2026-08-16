package com.gimenes.alex.rickandmorty.feature.guesscharacter

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gimenes.alex.rickandmorty.core.designsystem.backhandler.PlatformBackHandler
import com.gimenes.alex.rickandmorty.core.designsystem.component.CachedCharacterImage
import com.gimenes.alex.rickandmorty.core.designsystem.component.PrimaryGradientButton
import com.gimenes.alex.rickandmorty.core.designsystem.component.SecondaryOutlinedButton
import com.gimenes.alex.rickandmorty.core.designsystem.motion.reducedMotionAwareSpec
import com.gimenes.alex.rickandmorty.core.designsystem.motion.rememberKeyedValuePop
import com.gimenes.alex.rickandmorty.core.designsystem.motion.rememberPressAnimatedFloat
import com.gimenes.alex.rickandmorty.core.designsystem.motion.rememberReducedMotionEnabled
import com.gimenes.alex.rickandmorty.core.designsystem.motion.rememberScalePop
import com.gimenes.alex.rickandmorty.core.designsystem.motion.rememberValuePop
import com.gimenes.alex.rickandmorty.core.designsystem.theme.RickAndMortyExtendedTheme
import com.gimenes.alex.rickandmorty.core.designsystem.theme.RickAndMortyShapes
import org.koin.compose.viewmodel.koinViewModel

/**
 * The full Guess the Character flow (issue #16): Loading -> Round (Character Reveal + Choices,
 * looping) -> Feedback -> ... -> Run Ended -> Play Again / Home. One screen, driven by
 * [GuessCharacterViewModel]'s single [GuessCharacterUiState] - see that file's kdoc for why this
 * isn't a nested nav graph, mirroring [com.gimenes.alex.rickandmorty.feature.quiz.QuizScreen]
 * (issue #15).
 *
 * ### Manual exit confirmation is screen-local state, not ViewModel state
 * The confirm/cancel dialog shown when exiting mid-streak is kept as plain [rememberSaveable]
 * Compose state here rather than a [GuessCharacterUiState] variant. It's a transient "are you sure"
 * prompt layered *over* whatever [GuessCharacterUiState.Round] is currently showing - it doesn't
 * change any game data (nothing is persisted or lost either way, see
 * [GuessCharacterViewModel.endRunManually]'s kdoc), and modeling it as a sealed state would force
 * every consumer to reconstruct "which round were we even confirming exit from?" instead of simply
 * rendering a dialog on top of the round that's still right there in [uiState]. [rememberSaveable]
 * (not plain `remember`) so the dialog survives a configuration change without needing ViewModel
 * involvement.
 *
 * ### The system/hardware back gesture is intercepted too, not just the in-screen exit button
 * The in-screen exit [IconButton] (in [RoundContent]) was the only way to trigger the exit
 * confirmation until issue #17: the platform's own back button/gesture went straight to
 * `navigation-compose`'s default behavior (popping the back stack), silently discarding an
 * active streak without ever showing [ExitConfirmationDialog]. [PlatformBackHandler] below closes
 * that gap on Android (see its kdoc for why this needs an expect/actual rather than being plain
 * common code, and why iOS is a documented no-op).
 *

 * @param onExit invoked when the player leaves the flow (Run Ended's "Home" CTA, or a confirmed/
 *  no-confirmation-needed manual exit). The caller (currently `App.kt`, eventually issue #17's real
 *  nav wiring) decides what "leaving Guess the Character" means - today that's popping back to Home.
 */
@Composable
fun GuessCharacterScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GuessCharacterViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExitConfirmation by rememberSaveable { mutableStateOf(false) }

    // Mirrors RoundContent's onExitRequested logic (streak > 0 => confirm, otherwise let the
    // system back action proceed normally) so the hardware/gesture back button can't bypass the
    // same confirmation the in-screen exit button enforces. Only enabled during an active round
    // with progress at risk - see this file's kdoc.
    val currentRoundStreak = (uiState as? GuessCharacterUiState.Round)?.streak ?: 0
    PlatformBackHandler(enabled = currentRoundStreak > 0) {
        showExitConfirmation = true
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                GuessCharacterUiState.Loading -> LoadingContent()

                GuessCharacterUiState.NetworkError -> NetworkErrorContent(
                    onRetry = viewModel::retry,
                    onExit = onExit,
                )

                GuessCharacterUiState.Empty -> EmptyContent(onExit = onExit)

                is GuessCharacterUiState.Round -> RoundContent(
                    state = state,
                    onSelectAnswer = viewModel::selectAnswer,
                    onExitRequested = {
                        // Per the UX spec: no active progress means nothing to confirm away.
                        if (state.streak > 0) showExitConfirmation = true else onExit()
                    },
                )

                is GuessCharacterUiState.RunEnded -> RunEndedContent(
                    state = state,
                    onPlayAgain = viewModel::playAgain,
                    onExit = onExit,
                )
            }
        }
    }

    if (showExitConfirmation) {
        val streakAtRisk = (uiState as? GuessCharacterUiState.Round)?.streak ?: 0
        ExitConfirmationDialog(
            streak = streakAtRisk,
            onConfirm = {
                showExitConfirmation = false
                viewModel.endRunManually()
            },
            onDismiss = { showExitConfirmation = false },
        )
    }
}

@Composable
private fun ExitConfirmationDialog(
    streak: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("End this run?") },
        text = {
            Text(
                "Your streak of $streak will be recorded - nothing is lost by leaving now, " +
                    "but you'll start fresh next time. End this run?",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("End Run") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep Playing") }
        },
    )
}

// ---------------------------------------------------------------------------------------------
// Loading / Network Error / Empty (shared states)
// ---------------------------------------------------------------------------------------------

@Composable
private fun LoadingContent() {
    val spacing = RickAndMortyExtendedTheme.spacing
    Box(modifier = Modifier.fillMaxSize().padding(spacing.lg), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                text = "Scanning dimensions for a face…",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NetworkErrorContent(onRetry: () -> Unit, onExit: () -> Unit) {
    FullScreenMessage(
        title = "Ohh Geez",
        message = "Can't reach the multiverse right now. Check your connection and try again.",
        primaryActionLabel = "Retry",
        onPrimaryAction = onRetry,
        secondaryActionLabel = "Home",
        onSecondaryAction = onExit,
    )
}

@Composable
private fun EmptyContent(onExit: () -> Unit) {
    FullScreenMessage(
        title = "Not Enough Multiverse Data",
        message = "There isn't enough character data cached right now to build a round. " +
            "Rick probably deleted a dimension.",
        primaryActionLabel = "Home",
        onPrimaryAction = onExit,
        secondaryActionLabel = null,
        onSecondaryAction = null,
    )
}

@Composable
private fun FullScreenMessage(
    title: String,
    message: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    secondaryActionLabel: String?,
    onSecondaryAction: (() -> Unit)?,
) {
    val spacing = RickAndMortyExtendedTheme.spacing
    Box(modifier = Modifier.fillMaxSize().padding(spacing.lg), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            GlitchedPortalIcon()
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            PrimaryGradientButton(
                text = primaryActionLabel,
                onClick = onPrimaryAction,
                modifier = Modifier.fillMaxWidth(),
            )
            if (secondaryActionLabel != null && onSecondaryAction != null) {
                TextButton(
                    onClick = onSecondaryAction,
                    modifier = Modifier.fillMaxWidth().heightIn(min = MIN_TOUCH_TARGET),
                ) {
                    Text(secondaryActionLabel)
                }
            }
        }
    }
}

@Composable
private fun GlitchedPortalIcon() {
    val color = MaterialTheme.colorScheme.error
    Canvas(modifier = Modifier.size(48.dp)) {
        val strokeWidth = size.minDimension * 0.1f
        drawCircle(
            color = color,
            radius = size.minDimension / 2f - strokeWidth / 2f,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.2f, size.height * 0.2f),
            end = Offset(size.width * 0.8f, size.height * 0.8f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.8f, size.height * 0.2f),
            end = Offset(size.width * 0.2f, size.height * 0.8f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Round (Character Reveal + Choices / Feedback)
// ---------------------------------------------------------------------------------------------

@Composable
private fun RoundContent(
    state: GuessCharacterUiState.Round,
    onSelectAnswer: (Int) -> Unit,
    onExitRequested: () -> Unit,
) {
    val spacing = RickAndMortyExtendedTheme.spacing
    val extendedColors = RickAndMortyExtendedTheme.extendedColors
    val focusRequester = remember { FocusRequester() }

    // Read once per screen and threaded down to every R7 motion moment below (portal-frame
    // intensify, streak pop/pulse, answer press/lock-in) - matches this file's existing
    // round-transition Crossfade and QuizScreen's precedent (see MotionEffects.kt's kdoc).
    val reducedMotion by rememberReducedMotionEnabled()

    // A11y requirement: accessibility focus moves to the round heading whenever a new round
    // appears (including the very first one), so screen-reader users get an explicit
    // announcement instead of having to manually re-discover the new content - matches Trivia's
    // per-question refocus (see QuizScreen.QuestionContent).
    LaunchedEffect(state.target.id) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onExitRequested,
                modifier = Modifier.size(MIN_TOUCH_TARGET),
            ) {
                ExitGlyph(color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StreakCounter(streak = state.streak, reducedMotion = reducedMotion)
        }

        Text(
            text = "Guess the Character",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusable()
                .semantics { heading() },
        )

        // Resolved via issue #40's PlatformReducedMotion (see its kdoc): this Crossfade uses a
        // near-zero duration when the platform signals a reduced-motion preference, previously
        // left as a TODO here because Compose Multiplatform didn't yet expose a uniform
        // cross-platform reduced-motion API - a real one turned out to exist on Android
        // (Settings.Global.ANIMATOR_DURATION_SCALE); iOS remains a documented no-op for now, see
        // PlatformReducedMotion's iOS actual. The underlying round-advance *timing* (the actual
        // pacing pause, see GuessCharacterViewModel's kdoc) is unaffected either way; only this
        // visual flourish is gated here.
        //
        // Keyed by target.id (round identity), not the full `state`, so only an actual round
        // change (auto-advance after a correct guess) triggers the fade - locking in an answer or
        // the streak ticking up shouldn't itself replay the transition. The content lambda closes
        // over the outer `state` directly so it always reflects the latest selection/lock/streak.
        val crossfadeDurationMs = if (reducedMotion) 0 else CROSSFADE_DEFAULT_DURATION_MS
        Crossfade(
            targetState = state.target.id,
            animationSpec = tween(crossfadeDurationMs),
            label = "guess-character-round",
        ) {
            val isCorrect = state.isLocked && state.selectedOptionIndex == state.correctOptionIndex
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                // R7 (issue #43) moments 1 + 2: the character reveal wrapped in a portal-ring
                // frame (static accent->primary gradient border + ambient glow, moment 1) that
                // briefly intensifies in the feedback color once the round locks (moment 2).
                PortalFrame(
                    isLocked = state.isLocked,
                    isCorrect = isCorrect,
                    reducedMotion = reducedMotion,
                ) {
                    CachedCharacterImage(
                        imageUrl = state.target.image,
                        contentDescription = if (state.isLocked) {
                            "Character image: ${state.target.name}"
                        } else {
                            "Character image — guess who this is"
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (state.isLocked) {
                    Text(
                        text = if (isCorrect) {
                            "Correct!"
                        } else {
                            "Incorrect. It was ${state.options[state.correctOptionIndex].name}."
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isCorrect) {
                            extendedColors.feedbackCorrect
                        } else {
                            extendedColors.feedbackIncorrect
                        },
                        // A11y requirement: feedback (and the streak change it drives) is
                        // announced via a live region rather than requiring the user to manually
                        // navigate to discover the result. Reduced motion doesn't affect this -
                        // it's the primary feedback channel either way (see PortalFrame's kdoc).
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    state.options.forEachIndexed { index, option ->
                        AnswerOptionButton(
                            text = option.name,
                            optionState = answerOptionState(index, state),
                            enabled = !state.isLocked,
                            isJustLocked = state.isLocked && index == state.selectedOptionIndex,
                            reducedMotion = reducedMotion,
                            onClick = { onSelectAnswer(index) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * R7 (issue #43) moments 1 + 2: wraps [content] (the character image) in a portal-ring frame -
 * reusing the same `accent`->`primary` gradient and ambient-glow visual language as
 * [com.gimenes.alex.rickandmorty.core.designsystem.component.PortalRingGlyph] (R3, issue #39)
 * rather than that composable directly, since this frames a square/rectangular image rather than
 * drawing a self-contained glyph. The glow deliberately isn't clipped to this composable's own
 * layout bounds (same trick as `PortalRingGlyph`'s kdoc documents) so it bleeds into the page
 * behind the frame.
 *
 * Moment 1 (the static ring + glow) is unconditional, same as R6's `QuestionContent` card wrapper
 * - it's a static visual change, not motion, so it isn't itself gated by [reducedMotion]. Moment 2
 * is: the glow briefly intensifies (~300ms) in [isCorrect]'s feedback color the instant [isLocked]
 * flips to `true`, via [rememberValuePop] (see that function's kdoc for why this is the
 * `rememberScalePop` shape generalized to a glow alpha rather than a scale). Under [reducedMotion]
 * the frame stays static - the intensify never fires - and the existing "Correct!"/"Incorrect..."
 * live-region text plus each [AnswerOptionButton]'s correct/incorrect [FeedbackTag] still carry the
 * signal regardless, so reduced motion loses no information, only the flourish.
 */
@Composable
private fun PortalFrame(
    isLocked: Boolean,
    isCorrect: Boolean,
    reducedMotion: Boolean,
    content: @Composable () -> Unit,
) {
    val accent = RickAndMortyExtendedTheme.extendedColors.accent
    val primary = MaterialTheme.colorScheme.primary
    val feedbackColor = if (isCorrect) {
        RickAndMortyExtendedTheme.extendedColors.feedbackCorrect
    } else {
        RickAndMortyExtendedTheme.extendedColors.feedbackIncorrect
    }

    val intensifyAlpha by rememberValuePop(
        trigger = isLocked,
        reducedMotion = reducedMotion,
        baseValue = 0f,
        peakValue = 1f,
        durationMillis = PORTAL_INTENSIFY_DURATION_MS,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val inflate = PORTAL_GLOW_BLEED.toPx()
                val glowSize = Size(size.width + inflate * 2f, size.height + inflate * 2f)
                val glowTopLeft = Offset(-inflate, -inflate)
                val glowCornerRadius = CornerRadius(PORTAL_GLOW_CORNER_RADIUS.toPx() + inflate)

                // Static ambient glow (moment 1): always present, reinforcing the portal-ring
                // motif regardless of reduced motion.
                drawRoundRect(
                    brush = Brush.radialGradient(
                        0f to accent.copy(alpha = 0.35f),
                        0.6f to primary.copy(alpha = 0.16f),
                        1f to primary.copy(alpha = 0f),
                        center = center,
                        radius = glowSize.maxDimension / 2f,
                    ),
                    topLeft = glowTopLeft,
                    size = glowSize,
                    cornerRadius = glowCornerRadius,
                )

                // Moment 2's intensify overlay - only drawn while popping, invisible (alpha 0)
                // the rest of the time, and never drawn at all under reduced motion since
                // intensifyAlpha never leaves 0f there.
                if (intensifyAlpha > 0f) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            0f to feedbackColor.copy(alpha = 0.65f * intensifyAlpha),
                            0.6f to feedbackColor.copy(alpha = 0.28f * intensifyAlpha),
                            1f to feedbackColor.copy(alpha = 0f),
                            center = center,
                            radius = glowSize.maxDimension / 2f,
                        ),
                        topLeft = glowTopLeft,
                        size = glowSize,
                        cornerRadius = glowCornerRadius,
                    )
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = PORTAL_RING_WIDTH,
                    brush = Brush.linearGradient(listOf(accent, primary)),
                    shape = RickAndMortyShapes.medium,
                ),
        ) {
            content()
        }
    }
}

/**
 * Streak counter, prominent and persistent across the round per the UX spec - icon + weighted
 * color rather than a plain number, using [MaterialTheme.colorScheme.tertiary] (Morty's yellow,
 * the palette's closest existing "acid-yellow" tone - see [ExtendedColors][com.gimenes.alex.rickandmorty.core.designsystem.theme.ExtendedColors]'s
 * kdoc) as the highlight color, since this design system doesn't (yet) define a dedicated
 * streak-highlight token distinct from the brand palette. [ExtendedColors.accent] was considered
 * but is a muted tan/gold, not the acid-yellow the UX spec calls for - `tertiary` is the closer
 * match without inventing a new, undocumented color. Worth flagging to ATLAS/LUMA as a follow-up:
 * a dedicated `ExtendedColors.streakHighlight` token would let this stop borrowing a Material slot
 * that's nominally reserved for other tertiary UI.
 *
 * Wrapped in its own live region so a streak increment (or reset) is announced to screen readers
 * as it happens, independent of the feedback text's own live region.
 *
 * R7 (issue #43) moment 3: a scale-pop (reusing R6's [rememberScalePop] unmodified) plus one
 * outward glow pulse fire every time [streak] changes to a new value - see
 * [com.gimenes.alex.rickandmorty.core.designsystem.motion.rememberKeyedValuePop]'s kdoc for why a
 * monotonically-increasing counter needed that new key-identity-based primitive rather than the
 * boolean-trigger [rememberScalePop]/`rememberValuePop` shape moment 2 above uses. A bigger
 * version of the same pulse fires at milestone streaks (every [STREAK_MILESTONE_INTERVAL], e.g. 5,
 * 10, 15…) - issue #16's streak model has no existing "milestone" concept, so this is intentionally
 * just that modulo check, not a new milestone system. Synced with the live region below: both the
 * visual pop and the announcement key off the exact same [streak] value in the same recomposition.
 * Reduced motion: the number updates instantly with no pop/pulse/ring, and the live-region
 * announcement (unaffected by [reducedMotion]) remains the primary feedback channel either way.
 */
@Composable
private fun StreakCounter(streak: Int, reducedMotion: Boolean) {
    val spacing = RickAndMortyExtendedTheme.spacing
    val tertiary = MaterialTheme.colorScheme.tertiary
    val isMilestone = streak > 0 && streak % STREAK_MILESTONE_INTERVAL == 0

    val popScale by rememberKeyedValuePop(
        key = streak,
        reducedMotion = reducedMotion,
        baseValue = 1f,
        peakValue = if (isMilestone) STREAK_MILESTONE_POP_SCALE else STREAK_POP_SCALE,
        durationMillis = STREAK_POP_DURATION_MS,
    )
    val glowAlpha by rememberKeyedValuePop(
        key = streak,
        reducedMotion = reducedMotion,
        baseValue = 0f,
        peakValue = 1f,
        durationMillis = if (isMilestone) STREAK_MILESTONE_GLOW_DURATION_MS else STREAK_POP_DURATION_MS,
    )
    val glowBleed = if (isMilestone) STREAK_MILESTONE_GLOW_BLEED else STREAK_GLOW_BLEED

    Box(contentAlignment = Alignment.Center) {
        if (glowAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val radius = size.maxDimension / 2f + glowBleed.toPx()
                        drawCircle(
                            brush = Brush.radialGradient(
                                0f to tertiary.copy(alpha = 0.55f * glowAlpha),
                                0.6f to tertiary.copy(alpha = 0.22f * glowAlpha),
                                1f to tertiary.copy(alpha = 0f),
                                center = center,
                                radius = radius,
                            ),
                            radius = radius,
                            center = center,
                        )
                    },
            )
        }
        Surface(
            shape = RickAndMortyShapes.medium,
            color = tertiary.copy(alpha = 0.16f),
            modifier = Modifier
                .graphicsLayer {
                    scaleX = popScale
                    scaleY = popScale
                }
                .semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = "Streak $streak"
                },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                StreakFlameIcon(color = tertiary)
                Text(
                    text = "Streak: $streak",
                    style = MaterialTheme.typography.titleMedium,
                    color = tertiary,
                )
            }
        }
    }
}

@Composable
private fun StreakFlameIcon(color: Color, size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            cubicTo(w * 0.85f, h * 0.4f, w * 0.75f, h * 0.55f, w * 0.65f, h * 0.45f)
            cubicTo(w * 0.7f, h * 0.7f, w * 0.55f, h * 0.98f, w * 0.5f, h * 0.98f)
            cubicTo(w * 0.45f, h * 0.98f, w * 0.15f, h * 0.75f, w * 0.3f, h * 0.4f)
            cubicTo(w * 0.35f, h * 0.55f, w * 0.45f, h * 0.45f, w * 0.4f, h * 0.25f)
            cubicTo(w * 0.45f, h * 0.15f, w * 0.5f, h * 0.1f, w * 0.5f, h * 0.05f)
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Composable
private fun ExitGlyph(color: Color, size: Dp = 20.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.14f
        drawLine(
            color = color,
            start = Offset(this.size.width * 0.15f, this.size.height * 0.15f),
            end = Offset(this.size.width * 0.85f, this.size.height * 0.85f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(this.size.width * 0.85f, this.size.height * 0.15f),
            end = Offset(this.size.width * 0.15f, this.size.height * 0.85f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

private enum class AnswerOptionState { NEUTRAL, CORRECT, INCORRECT }

private fun answerOptionState(index: Int, state: GuessCharacterUiState.Round): AnswerOptionState {
    if (!state.isLocked) return AnswerOptionState.NEUTRAL
    return when (index) {
        state.correctOptionIndex -> AnswerOptionState.CORRECT
        state.selectedOptionIndex -> AnswerOptionState.INCORRECT
        else -> AnswerOptionState.NEUTRAL
    }
}

/**
 * A single answer option - deliberately mirrors
 * [com.gimenes.alex.rickandmorty.feature.quiz.QuizScreen]'s `AnswerOptionButton`: locking is a
 * distinct visual state (border width/color + elevation), not color alone, and the correct/
 * incorrect reveal always pairs its color with an icon and an explicit text label - colorblind-safe
 * by construction. Minimum 56dp height, >=8dp spacing between options (via the caller's
 * `Arrangement.spacedBy(spacing.sm)`) per the a11y spec.
 *
 * R7 (issue #43) moment 5 layers the same three motion touches R6 gave Trivia's equivalent row,
 * reusing the exact same shared `core:designsystem` primitives (see `MotionEffects.kt`):
 * - a spring-driven 97% press scale + `accent` glow bloom while pressed
 *   ([rememberPressAnimatedFloat]), on top of [Surface]'s own default ripple/state-layer.
 * - an 80ms 100%->103%->100% pop the instant this row locks in as the tapped answer
 *   ([isJustLocked], via [rememberScalePop]).
 * - the border/tint color swap eases over ~250ms instead of snapping instantly
 *   ([reducedMotionAwareSpec]).
 * All three collapse to today's shipped instant behavior under [reducedMotion].
 */
@Composable
private fun AnswerOptionButton(
    text: String,
    optionState: AnswerOptionState,
    enabled: Boolean,
    isJustLocked: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
) {
    val spacing = RickAndMortyExtendedTheme.spacing
    val extendedColors = RickAndMortyExtendedTheme.extendedColors

    val targetBorderColor: Color
    val borderWidth: Dp
    val targetContainerColor: Color
    val tonalElevation: Dp
    when (optionState) {
        AnswerOptionState.CORRECT -> {
            targetBorderColor = extendedColors.feedbackCorrect
            borderWidth = 3.dp
            targetContainerColor = extendedColors.feedbackCorrect.copy(alpha = 0.12f)
            tonalElevation = 4.dp
        }

        AnswerOptionState.INCORRECT -> {
            targetBorderColor = extendedColors.feedbackIncorrect
            borderWidth = 3.dp
            targetContainerColor = extendedColors.feedbackIncorrect.copy(alpha = 0.12f)
            tonalElevation = 4.dp
        }

        AnswerOptionState.NEUTRAL -> {
            targetBorderColor = MaterialTheme.colorScheme.outline
            borderWidth = 1.dp
            targetContainerColor = MaterialTheme.colorScheme.surface
            tonalElevation = 0.dp
        }
    }

    val colorSpec = reducedMotionAwareSpec<Color>(reducedMotion, FEEDBACK_TRANSITION_MS)
    val borderColor by animateColorAsState(targetBorderColor, colorSpec, label = "answer-border-color")
    val containerColor by animateColorAsState(targetContainerColor, colorSpec, label = "answer-container-color")

    val interactionSource = remember { MutableInteractionSource() }
    val pressScale by rememberPressAnimatedFloat(
        interactionSource = interactionSource,
        reducedMotion = reducedMotion,
        pressedValue = PRESS_SCALE,
    )
    val glowAlpha by rememberPressAnimatedFloat(
        interactionSource = interactionSource,
        reducedMotion = reducedMotion,
        pressedValue = 1f,
        restValue = 0f,
    )
    val lockPopScale by rememberScalePop(
        trigger = isJustLocked,
        reducedMotion = reducedMotion,
        peakScale = LOCK_POP_SCALE,
        durationMillis = LOCK_POP_DURATION_MS,
    )
    val accentGlow = extendedColors.accent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val combinedScale = pressScale * lockPopScale
                scaleX = combinedScale
                scaleY = combinedScale
            },
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth().heightIn(min = MIN_ANSWER_HEIGHT),
            shape = RickAndMortyShapes.medium,
            color = containerColor,
            tonalElevation = tonalElevation,
            border = BorderStroke(borderWidth, borderColor),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                when (optionState) {
                    AnswerOptionState.CORRECT -> FeedbackTag(
                        label = "Correct",
                        color = extendedColors.feedbackCorrect,
                        icon = { CorrectMarkIcon(extendedColors.feedbackCorrect) },
                    )

                    AnswerOptionState.INCORRECT -> FeedbackTag(
                        label = "Incorrect",
                        color = extendedColors.feedbackIncorrect,
                        icon = { IncorrectMarkIcon(extendedColors.feedbackIncorrect) },
                    )

                    AnswerOptionState.NEUTRAL -> Unit
                }
            }
        }

        // Press glow bloom - a soft accent ring hugging the row's own rounded corners, drawn
        // above the Surface (so it isn't hidden underneath its opaque container fill) but at low
        // peak alpha and concentrated toward the edges so it reads as a bloom, not a wash that
        // would obscure the option text. No pointer-input modifiers of its own, so it never
        // steals touches from the Surface beneath it.
        if (glowAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RickAndMortyShapes.medium)
                    .drawBehind {
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                0f to accentGlow.copy(alpha = 0f),
                                0.7f to accentGlow.copy(alpha = 0f),
                                1f to accentGlow.copy(alpha = glowAlpha * PRESS_GLOW_MAX_ALPHA),
                            ),
                            cornerRadius = CornerRadius(ANSWER_GLOW_CORNER_RADIUS.toPx()),
                        )
                    },
            )
        }
    }
}

@Composable
private fun FeedbackTag(label: String, color: Color, icon: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RickAndMortyExtendedTheme.spacing.xs),
    ) {
        icon()
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun CorrectMarkIcon(color: Color, size: Dp = 20.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.14f
        val path = Path().apply {
            moveTo(this@Canvas.size.width * 0.18f, this@Canvas.size.height * 0.55f)
            lineTo(this@Canvas.size.width * 0.42f, this@Canvas.size.height * 0.78f)
            lineTo(this@Canvas.size.width * 0.85f, this@Canvas.size.height * 0.25f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
private fun IncorrectMarkIcon(color: Color, size: Dp = 20.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.14f
        drawLine(
            color = color,
            start = Offset(this.size.width * 0.2f, this.size.height * 0.2f),
            end = Offset(this.size.width * 0.8f, this.size.height * 0.8f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(this.size.width * 0.8f, this.size.height * 0.2f),
            end = Offset(this.size.width * 0.2f, this.size.height * 0.8f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Run Ended
// ---------------------------------------------------------------------------------------------

@Composable
private fun RunEndedContent(
    state: GuessCharacterUiState.RunEnded,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit,
) {
    val spacing = RickAndMortyExtendedTheme.spacing
    val flavorText = when {
        state.finalStreak >= HIGH_STREAK_THRESHOLD ->
            "Interdimensional legend. Even Rick's mildly impressed."
        state.finalStreak >= MID_STREAK_THRESHOLD ->
            "Solid run. You know your multiverse."
        state.finalStreak > 0 ->
            "Not bad for a beta version of you."
        else -> "Every genius starts somewhere. Give it another spin."
    }

    // R7 (issue #43) moment 4: the final streak number fades+scales in once, the first time this
    // composable enters composition - the exact same local Compose state approach (Animatable +
    // LaunchedEffect(Unit)) R6 used for Trivia's Results score reveal
    // (see QuizScreen.ResultsContent's kdoc for why this is screen-local state, not
    // GuessCharacterUiState.RunEnded state: it's purely "has this composable's number already
    // animated in," nothing GuessCharacterViewModel needs to know about). Under reduced motion the
    // Animatable starts (and stays) at its final value, so the number renders fully-formed on the
    // very first frame - no fade/scale at all, not just a faster one.
    val reducedMotion by rememberReducedMotionEnabled()
    val streakReveal = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        streakReveal.animateTo(1f, reducedMotionAwareSpec(reducedMotion, STREAK_REVEAL_DURATION_MS))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(
            text = "Run Ended",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "${state.finalStreak}",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.graphicsLayer {
                alpha = streakReveal.value
                val scale = STREAK_REVEAL_MIN_SCALE +
                    (1f - STREAK_REVEAL_MIN_SCALE) * streakReveal.value
                scaleX = scale
                scaleY = scale
            },
        )
        Text(text = "Final Streak", style = MaterialTheme.typography.labelLarge)
        Text(text = flavorText, style = MaterialTheme.typography.bodyLarge)

        if (state.isNewBest) {
            NewBestCallout(bestStreakEver = state.bestStreakEver)
        } else {
            Text(
                text = "Best streak ever: ${state.bestStreakEver}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            SecondaryOutlinedButton(
                text = "Home",
                onClick = onExit,
                modifier = Modifier.weight(1f),
            )
            PrimaryGradientButton(
                text = "Play Again",
                onClick = onPlayAgain,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NewBestCallout(bestStreakEver: Int) {
    val spacing = RickAndMortyExtendedTheme.spacing
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RickAndMortyShapes.medium,
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
    ) {
        Column(modifier = Modifier.padding(spacing.md)) {
            Text(
                text = "New Best!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = "$bestStreakEver in a row is your best run yet.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Matches [Crossfade]'s own default `animationSpec` duration (`tween()`'s default), kept as an
 * explicit local constant (rather than relying on `androidx.compose.animation.core`'s internal
 * default) so it can be swapped to `0` under reduced motion - see this file's round-transition
 * `Crossfade`.
 */
private const val CROSSFADE_DEFAULT_DURATION_MS = 300

/** Minimum interactive touch target per the a11y spec (≥48dp). */
private val MIN_TOUCH_TARGET = 48.dp

/** Minimum answer option height per the a11y spec (≥56dp). */
private val MIN_ANSWER_HEIGHT = 56.dp

private const val HIGH_STREAK_THRESHOLD = 8
private const val MID_STREAK_THRESHOLD = 3

// ---------------------------------------------------------------------------------------------
// R7 (issue #43) motion constants - see the individual moments' kdoc (PortalFrame, StreakCounter,
// AnswerOptionButton, RunEndedContent) for what each drives.
// ---------------------------------------------------------------------------------------------

/** Moment 1: portal-ring border thickness (2-3dp per the UX spec). */
private val PORTAL_RING_WIDTH = 2.5.dp

/** Moment 1: how far past the frame's own bounds the ambient glow is allowed to bleed. */
private val PORTAL_GLOW_BLEED = 24.dp

/** Moment 1: glow corner radius, matching [RickAndMortyShapes.medium] (the frame/image's own shape). */
private val PORTAL_GLOW_CORNER_RADIUS = 20.dp

/** Moment 2: the portal frame's feedback-color intensify pulse duration. */
private const val PORTAL_INTENSIFY_DURATION_MS = 300

/** Moment 3: the streak pill's scale-pop peak on an ordinary correct answer. */
private const val STREAK_POP_SCALE = 1.15f

/** Moment 3: the streak pill's scale-pop peak at a milestone streak - a bigger version of the same pulse. */
private const val STREAK_MILESTONE_POP_SCALE = 1.3f

/** Moment 3: the streak pill's pop+glow duration on an ordinary correct answer. */
private const val STREAK_POP_DURATION_MS = 300

/** Moment 3: the milestone glow ring's (slightly longer) duration. */
private const val STREAK_MILESTONE_GLOW_DURATION_MS = 450

/** Moment 3: how far past the pill the glow pulse extends on an ordinary correct answer. */
private val STREAK_GLOW_BLEED = 10.dp

/** Moment 3: how far past the pill the bigger milestone glow ring extends. */
private val STREAK_MILESTONE_GLOW_BLEED = 22.dp

/** Moment 3: every Nth streak (5, 10, 15…) gets the bigger milestone pulse - a simple modulo check, not a new milestone system (issue #16 has no existing milestone concept to build on). */
private const val STREAK_MILESTONE_INTERVAL = 5

/** Moment 4: final streak reveal fade+scale-in duration on first appearance, matching R6's Results score reveal. */
private const val STREAK_REVEAL_DURATION_MS = 400

/** Moment 4: final streak reveal's starting scale (grows from this to 100% as it fades in). */
private const val STREAK_REVEAL_MIN_SCALE = 0.7f

/** Moment 5: border/tint color transition duration, matching R6's Trivia equivalent. */
private const val FEEDBACK_TRANSITION_MS = 250

/** Moment 5: the tapped row's lock-in pop total duration (up + down). */
private const val LOCK_POP_DURATION_MS = 80

/** Moment 5: the tapped row's lock-in pop peak scale (100% -> 103% -> 100%). */
private const val LOCK_POP_SCALE = 1.03f

/** Moment 5: press-scale target (100% -> 97%). */
private const val PRESS_SCALE = 0.97f

/** Moment 5: peak alpha of the press glow bloom at its most visible (fully pressed). */
private const val PRESS_GLOW_MAX_ALPHA = 0.35f

/** Moment 5: corner radius the press glow bloom is drawn with, matching [RickAndMortyShapes.medium]. */
private val ANSWER_GLOW_CORNER_RADIUS = 20.dp

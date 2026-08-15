package com.gimenes.alex.rickandmorty.feature.guesscharacter

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gimenes.alex.rickandmorty.core.designsystem.component.CachedCharacterImage
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
            Button(
                onClick = onPrimaryAction,
                modifier = Modifier.fillMaxWidth().heightIn(min = MIN_TOUCH_TARGET),
            ) {
                Text(primaryActionLabel)
            }
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
            StreakCounter(streak = state.streak)
        }

        Text(
            text = "Guess the Character",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusable()
                .semantics { heading() },
        )

        // TODO(reduced-motion): this Crossfade should use a near-zero duration when the platform
        //  signals a reduced-motion preference. Compose Multiplatform doesn't yet expose a uniform
        //  cross-platform reduced-motion API the way Android's Settings.Global.ANIMATOR_DURATION_SCALE
        //  does - see GuessCharacterViewModel's kdoc. The underlying round-advance *timing* (the
        //  actual pacing pause) is unaffected either way; only this visual flourish is gated here.
        //
        // Keyed by target.id (round identity), not the full `state`, so only an actual round
        // change (auto-advance after a correct guess) triggers the fade - locking in an answer or
        // the streak ticking up shouldn't itself replay the transition. The content lambda closes
        // over the outer `state` directly so it always reflects the latest selection/lock/streak.
        Crossfade(targetState = state.target.id, label = "guess-character-round") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                CachedCharacterImage(
                    imageUrl = state.target.image,
                    contentDescription = if (state.isLocked) {
                        "Character image: ${state.target.name}"
                    } else {
                        "Character image — guess who this is"
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.isLocked) {
                    val isCorrect = state.selectedOptionIndex == state.correctOptionIndex
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
                        // navigate to discover the result.
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    state.options.forEachIndexed { index, option ->
                        AnswerOptionButton(
                            text = option.name,
                            optionState = answerOptionState(index, state),
                            enabled = !state.isLocked,
                            onClick = { onSelectAnswer(index) },
                        )
                    }
                }
            }
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
 */
@Composable
private fun StreakCounter(streak: Int) {
    val spacing = RickAndMortyExtendedTheme.spacing
    Surface(
        shape = RickAndMortyShapes.medium,
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
        modifier = Modifier.semantics(mergeDescendants = true) {
            liveRegion = LiveRegionMode.Polite
            contentDescription = "Streak $streak"
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            StreakFlameIcon(color = MaterialTheme.colorScheme.tertiary)
            Text(
                text = "Streak: $streak",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
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
 */
@Composable
private fun AnswerOptionButton(
    text: String,
    optionState: AnswerOptionState,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val spacing = RickAndMortyExtendedTheme.spacing
    val extendedColors = RickAndMortyExtendedTheme.extendedColors

    val borderColor: Color
    val borderWidth: Dp
    val containerColor: Color
    val tonalElevation: Dp
    when (optionState) {
        AnswerOptionState.CORRECT -> {
            borderColor = extendedColors.feedbackCorrect
            borderWidth = 3.dp
            containerColor = extendedColors.feedbackCorrect.copy(alpha = 0.12f)
            tonalElevation = 4.dp
        }

        AnswerOptionState.INCORRECT -> {
            borderColor = extendedColors.feedbackIncorrect
            borderWidth = 3.dp
            containerColor = extendedColors.feedbackIncorrect.copy(alpha = 0.12f)
            tonalElevation = 4.dp
        }

        AnswerOptionState.NEUTRAL -> {
            borderColor = MaterialTheme.colorScheme.outline
            borderWidth = 1.dp
            containerColor = MaterialTheme.colorScheme.surface
            tonalElevation = 0.dp
        }
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
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
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.weight(1f).heightIn(min = MIN_TOUCH_TARGET),
            ) {
                Text("Home")
            }
            Button(
                onClick = onPlayAgain,
                modifier = Modifier.weight(1f).heightIn(min = MIN_TOUCH_TARGET),
            ) {
                Text("Play Again")
            }
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

/** Minimum interactive touch target per the a11y spec (≥48dp). */
private val MIN_TOUCH_TARGET = 48.dp

/** Minimum answer option height per the a11y spec (≥56dp). */
private val MIN_ANSWER_HEIGHT = 56.dp

private const val HIGH_STREAK_THRESHOLD = 8
private const val MID_STREAK_THRESHOLD = 3

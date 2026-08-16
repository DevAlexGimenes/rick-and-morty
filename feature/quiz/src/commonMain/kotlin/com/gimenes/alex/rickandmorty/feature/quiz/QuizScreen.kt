package com.gimenes.alex.rickandmorty.feature.quiz

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import com.gimenes.alex.rickandmorty.core.designsystem.component.PrimaryGradientButton
import com.gimenes.alex.rickandmorty.core.designsystem.component.SecondaryOutlinedButton
import com.gimenes.alex.rickandmorty.core.designsystem.theme.PortalRingShape
import com.gimenes.alex.rickandmorty.core.designsystem.theme.RickAndMortyExtendedTheme
import com.gimenes.alex.rickandmorty.core.designsystem.theme.RickAndMortyShapes
import org.koin.compose.viewmodel.koinViewModel

/**
 * The full Trivia Quiz flow (issue #15): Category Select -> Loading -> Question (x10) -> Answer
 * Feedback -> Results/Review -> Replay / Home. One screen, driven by [QuizViewModel]'s single
 * [QuizUiState] - see that file's kdoc for why this isn't a nested nav graph.
 *
 * @param onExit invoked from the Results screen's "Home" CTA. The caller (currently `App.kt`,
 *  eventually issue #17's real nav wiring) decides what "leaving Quiz" means - today that's
 *  popping back to Home.
 */
@Composable
fun QuizScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is QuizUiState.CategorySelect -> CategorySelectContent(
                    state = state,
                    onSelectCategory = viewModel::selectCategory,
                    onStartQuiz = { state.selectedCategory?.let(viewModel::startQuiz) },
                )

                QuizUiState.Loading -> LoadingContent()

                QuizUiState.NetworkError -> NetworkErrorContent(
                    onRetry = viewModel::retry,
                    onBack = viewModel::backToCategorySelect,
                )

                QuizUiState.Empty -> EmptyContent(onBack = viewModel::backToCategorySelect)

                is QuizUiState.Question -> QuestionContent(
                    state = state,
                    onSelectAnswer = viewModel::selectAnswer,
                    onNext = viewModel::nextQuestion,
                )

                is QuizUiState.Results -> ResultsContent(
                    state = state,
                    onReplay = viewModel::replay,
                    onExit = onExit,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Category Select
// ---------------------------------------------------------------------------------------------

@Composable
private fun CategorySelectContent(
    state: QuizUiState.CategorySelect,
    onSelectCategory: (QuizCategory) -> Unit,
    onStartQuiz: () -> Unit,
) {
    val spacing = RickAndMortyExtendedTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(
            text = "Pick a Category",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Character trivia is live. The rest of the multiverse is still loading.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        QuizCategory.entries.forEach { category ->
            if (category.isAvailable) {
                SelectableCategoryCard(
                    category = category,
                    isSelected = state.selectedCategory == category,
                    onClick = { onSelectCategory(category) },
                )
            } else {
                LockedCategoryCard(category = category)
            }
        }

        if (state.selectedCategory != null) {
            PrimaryGradientButton(
                text = "Start Quiz",
                onClick = onStartQuiz,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SelectableCategoryCard(
    category: QuizCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val spacing = RickAndMortyExtendedTheme.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET)
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton),
        shape = RickAndMortyShapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = category.displayName, style = MaterialTheme.typography.titleMedium)
            if (isSelected) {
                Text(
                    text = "Selected",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Non-interactive "Coming Soon" card for Episode/Location. Dimmed visually, but - per the a11y
 * spec - screen-reader announced with its unavailable state and reason via [clearAndSetSemantics],
 * not just visually greyed out.
 */
@Composable
private fun LockedCategoryCard(category: QuizCategory) {
    val spacing = RickAndMortyExtendedTheme.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET)
            .clearAndSetSemantics {
                disabled()
                contentDescription = "${category.displayName} category, coming soon, unavailable"
            },
        shape = RickAndMortyShapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                shape = PortalRingShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = "Coming Soon",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
                )
            }
        }
    }
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
                text = "Charging the portal gun…",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NetworkErrorContent(onRetry: () -> Unit, onBack: () -> Unit) {
    FullScreenMessage(
        title = "Ohh Geez",
        message = "Can't reach the multiverse right now. Check your connection and try again.",
        primaryActionLabel = "Retry",
        onPrimaryAction = onRetry,
        secondaryActionLabel = "Back to Categories",
        onSecondaryAction = onBack,
    )
}

@Composable
private fun EmptyContent(onBack: () -> Unit) {
    FullScreenMessage(
        title = "Not Enough Multiverse Data",
        message = "There isn't enough character data cached right now to build a full round. " +
            "Rick probably deleted a dimension.",
        primaryActionLabel = "Back to Categories",
        onPrimaryAction = onBack,
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

/** A small "glitched portal ring" glyph for error/empty states, matching the on-brand icon style. */
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
// Question / Answer Feedback
// ---------------------------------------------------------------------------------------------

@Composable
private fun QuestionContent(
    state: QuizUiState.Question,
    onSelectAnswer: (Int) -> Unit,
    onNext: () -> Unit,
) {
    val spacing = RickAndMortyExtendedTheme.spacing
    val extendedColors = RickAndMortyExtendedTheme.extendedColors
    val focusRequester = remember { FocusRequester() }

    // A11y requirement: accessibility focus moves to the question heading whenever a new
    // question appears (including question 1), so screen-reader users get an explicit
    // announcement instead of having to manually re-discover the new content.
    LaunchedEffect(state.questionNumber) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(
            text = "Question ${state.questionNumber} of ${state.totalQuestions}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { state.questionNumber / state.totalQuestions.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                // Purely decorative alongside the "Question X of Y" text above - clear its
                // semantics so it isn't announced as a second, redundant node.
                .clearAndSetSemantics {},
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = state.questionText,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusable()
                .semantics { heading() },
        )

        if (state.isLocked) {
            val isCorrect = state.selectedOptionIndex == state.correctOptionIndex
            Text(
                text = if (isCorrect) {
                    "Correct!"
                } else {
                    "Incorrect. The correct answer is ${state.options[state.correctOptionIndex]}."
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (isCorrect) extendedColors.feedbackCorrect else extendedColors.feedbackIncorrect,
                // A11y requirement: feedback is announced via a live region rather than
                // requiring the user to manually navigate to discover the result.
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            state.options.forEachIndexed { index, option ->
                AnswerOptionButton(
                    text = option,
                    optionLetter = 'A' + index,
                    optionState = answerOptionState(index, state),
                    enabled = !state.isLocked,
                    onClick = { onSelectAnswer(index) },
                )
            }
        }

        if (state.isLocked) {
            PrimaryGradientButton(
                text = if (state.questionNumber == state.totalQuestions) "See Results" else "Next",
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private enum class AnswerOptionState { NEUTRAL, CORRECT, INCORRECT }

private fun answerOptionState(index: Int, state: QuizUiState.Question): AnswerOptionState {
    if (!state.isLocked) return AnswerOptionState.NEUTRAL
    return when (index) {
        state.correctOptionIndex -> AnswerOptionState.CORRECT
        state.selectedOptionIndex -> AnswerOptionState.INCORRECT
        else -> AnswerOptionState.NEUTRAL
    }
}

/**
 * A single answer option. Locking is a distinct visual state (border width/color + elevation),
 * not color alone; the correct/incorrect reveal always pairs its color with an icon and an
 * explicit "Correct"/"Incorrect" text label - colorblind-safe by construction, not just by color
 * choice. Minimum 56dp height per the a11y spec ([MIN_ANSWER_HEIGHT]).
 *
 * Each option is prefixed with an [optionLetter] badge (A/B/C/D, issue #35). The badge is a plain
 * [Text] living inside the same [Surface] as the option copy - it does not carry its own
 * semantics or content description, so it is picked up by the existing merged-semantics
 * announcement (the whole row is one accessibility node, per Compose's default `clickable`
 * merging) alongside the full option text rather than being announced as a separate, disconnected
 * element. E.g. a screen reader announces "A, Rick Sanchez", not just "A".
 */
@Composable
private fun AnswerOptionButton(
    text: String,
    optionLetter: Char,
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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                AnswerLetterBadge(letter = optionLetter, optionState = optionState)
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
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

/**
 * Small round A/B/C/D badge (issue #35) prefixing each answer option. Purely additive/visual -
 * see [AnswerOptionButton]'s kdoc for how it stays accessible without becoming a separate,
 * disconnected accessibility node. Uses the existing [PortalRingShape] token ("badges, streak
 * pips, avatar frames" per its own doc) and [RickAndMortyExtendedTheme] color/spacing tokens -
 * no new hardcoded shape or color values.
 *
 * R3 (issue #39): the neutral (unanswered) fill changed from `primaryContainer`/
 * `onPrimaryContainer` to a `surfaceVariant` fill with an `outline`-colored ring border - this
 * keeps `primary`/`accent` visually reserved for actual selection/feedback moments rather than
 * "just sitting there" on every unanswered option. Correct/incorrect reveal states are untouched.
 */
@Composable
private fun AnswerLetterBadge(letter: Char, optionState: AnswerOptionState) {
    val spacing = RickAndMortyExtendedTheme.spacing
    val extendedColors = RickAndMortyExtendedTheme.extendedColors
    val (containerColor, contentColor) = when (optionState) {
        AnswerOptionState.CORRECT -> extendedColors.feedbackCorrect to extendedColors.onFeedbackCorrect
        AnswerOptionState.INCORRECT -> extendedColors.feedbackIncorrect to extendedColors.onFeedbackIncorrect
        AnswerOptionState.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to
            MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = PortalRingShape,
        color = containerColor,
        border = if (optionState == AnswerOptionState.NEUTRAL) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        } else {
            null
        },
        modifier = Modifier.size(spacing.lg),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = letter.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
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
// Results / Review
// ---------------------------------------------------------------------------------------------

@Composable
private fun ResultsContent(
    state: QuizUiState.Results,
    onReplay: () -> Unit,
    onExit: () -> Unit,
) {
    val spacing = RickAndMortyExtendedTheme.spacing
    val ratio = if (state.total > 0) state.score.toFloat() / state.total else 0f
    val flavorText = when {
        ratio >= HIGH_SCORE_THRESHOLD -> "Nice work, genius. Even Rick's mildly impressed."
        ratio >= MID_SCORE_THRESHOLD -> "Not bad. You probably won't get turned into a Cronenberg for that."
        else -> "Yikes. Get schwifty and give it another shot."
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item {
            Text(
                text = "Results",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
        }
        item {
            Text(
                text = "${state.score} / ${state.total}",
                style = MaterialTheme.typography.displayMedium,
            )
        }
        item {
            Text(text = flavorText, style = MaterialTheme.typography.bodyLarge)
        }
        item {
            if (state.missedQuestions.isEmpty()) {
                PerfectRunCallout()
            } else {
                Text(text = "Missed Questions", style = MaterialTheme.typography.titleMedium)
            }
        }
        items(state.missedQuestions) { missed -> MissedQuestionCard(missed) }
        item {
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
                    text = "Replay",
                    onClick = onReplay,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PerfectRunCallout() {
    val spacing = RickAndMortyExtendedTheme.spacing
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RickAndMortyShapes.medium,
        color = RickAndMortyExtendedTheme.extendedColors.feedbackCorrect.copy(alpha = 0.15f),
    ) {
        Column(modifier = Modifier.padding(spacing.md)) {
            Text(
                text = "Perfect Run!",
                style = MaterialTheme.typography.titleMedium,
                color = RickAndMortyExtendedTheme.extendedColors.feedbackCorrect,
            )
            Text(
                text = "10 for 10. Not even Rick could nitpick that one.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun MissedQuestionCard(missed: MissedQuestion) {
    val spacing = RickAndMortyExtendedTheme.spacing
    val extendedColors = RickAndMortyExtendedTheme.extendedColors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RickAndMortyShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(text = missed.questionText, style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                IncorrectMarkIcon(extendedColors.feedbackIncorrect, size = 16.dp)
                Text(
                    text = "Your answer: ${missed.chosenAnswer}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extendedColors.feedbackIncorrect,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                CorrectMarkIcon(extendedColors.feedbackCorrect, size = 16.dp)
                Text(
                    text = "Correct answer: ${missed.correctAnswer}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extendedColors.feedbackCorrect,
                )
            }
        }
    }
}

/** Minimum interactive touch target per the a11y spec (≥48dp). */
private val MIN_TOUCH_TARGET = 48.dp

/** Minimum answer option height per the a11y spec (≥56dp). */
private val MIN_ANSWER_HEIGHT = 56.dp

private const val HIGH_SCORE_THRESHOLD = 0.8f
private const val MID_SCORE_THRESHOLD = 0.5f

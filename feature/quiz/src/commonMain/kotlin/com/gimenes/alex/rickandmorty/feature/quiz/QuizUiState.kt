package com.gimenes.alex.rickandmorty.feature.quiz

/**
 * The full Trivia Quiz flow (issue #15) - Category Select -> Loading -> Question (x10) -> Answer
 * Feedback -> Results/Review - is modeled as a single sealed [QuizUiState] driven entirely by
 * [QuizViewModel], rather than as a nested navigation graph with its own back stack.
 *
 * This is a deliberate choice: every step in the flow is a strictly linear progression through one
 * in-flight quiz attempt (no step needs its own deep-linkable destination, and "back" within the
 * flow is never a literal previous-screen pop - e.g. there's no "go back to the previous question").
 * A nested `NavHost` would add a second source of truth (nav back stack position vs. quiz progress)
 * that has to be kept in sync with the ViewModel's own state for zero benefit here. The outer app
 * (`App.kt`) only needs to know about entering/leaving Quiz as one destination, which this satisfies
 * via [QuizScreen]'s `onExit` callback.
 *
 * Answer Feedback is intentionally not its own [QuizUiState] variant - it is [Question] with
 * [Question.isLocked] set to `true`. The question being answered doesn't change when feedback
 * reveals (same [Question.questionNumber]/[Question.options]), only whether the options are
 * interactive and whether correctness is revealed, so modeling it as a state flag rather than a
 * separate sealed variant avoids duplicating every question field across two variants.
 */
sealed interface QuizUiState {

    /**
     * Category chip picker. Only [QuizCategory.CHARACTER] is selectable in v1 (episode/location are
     * rendered as locked/coming-soon cards) - see [QuizCategory.isAvailable].
     */
    data class CategorySelect(val selectedCategory: QuizCategory? = null) : QuizUiState

    /** Character pool fetch / question-set build in progress. Expected to be near-instant. */
    data object Loading : QuizUiState

    /**
     * [com.gimenes.alex.rickandmorty.core.domain.usecase.GetCharacterPoolUseCase] legitimately
     * returned an empty pool - cold start, no cache, no network. Distinct from [Empty]: this means
     * there was no data to work with at all, not merely "not enough variety in what we did get".
     */
    data object NetworkError : QuizUiState

    /**
     * The character pool came back non-empty but couldn't support a full 10-question round even
     * after retrying question generation - e.g. too few characters, or too little variety across
     * the quizable fields. See [QuizViewModel]'s question-generation retry strategy.
     */
    data object Empty : QuizUiState

    /**
     * A single question on screen, 1-indexed via [questionNumber] for display ("Question 3 of 10").
     * No running score is ever exposed here, matching the UX spec (score is Results-only).
     *
     * [correctOptionIndex] is always populated (the ViewModel always knows the answer), but callers
     * must only reveal it in the UI once [isLocked] is `true` - gating the reveal is the screen's
     * job, not a reason to hide the field behind a nullable that flips only when unlocked.
     */
    data class Question(
        val questionNumber: Int,
        val totalQuestions: Int,
        val questionText: String,
        val options: List<String>,
        val selectedOptionIndex: Int?,
        val correctOptionIndex: Int,
        val isLocked: Boolean,
    ) : QuizUiState

    /**
     * Final score + review of missed questions. [missedQuestions] is empty exactly when
     * [score] == [total] (a perfect run) - [QuizScreen] renders a distinct "perfect run" state for
     * that case rather than an unexplained empty list.
     */
    data class Results(
        val score: Int,
        val total: Int,
        val missedQuestions: List<MissedQuestion>,
    ) : QuizUiState
}

/** One missed question for the Results review list: what was asked, picked, and correct. */
data class MissedQuestion(
    val questionText: String,
    val chosenAnswer: String,
    val correctAnswer: String,
)

/**
 * The 3 quiz categories per LUMA's Category Select spec. Only [CHARACTER] is live in v1 (per NOVA's
 * scoping - this project only has Character data/use-cases wired); [EPISODE]/[LOCATION] render as
 * locked "Coming Soon" cards.
 */
enum class QuizCategory(val displayName: String, val isAvailable: Boolean) {
    CHARACTER(displayName = "Character", isAvailable = true),
    EPISODE(displayName = "Episode", isAvailable = false),
    LOCATION(displayName = "Location", isAvailable = false),
}

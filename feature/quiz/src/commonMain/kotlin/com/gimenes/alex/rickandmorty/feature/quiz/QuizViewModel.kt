package com.gimenes.alex.rickandmorty.feature.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gimenes.alex.rickandmorty.core.domain.model.Character
import com.gimenes.alex.rickandmorty.core.domain.model.TriviaQuestion
import com.gimenes.alex.rickandmorty.core.domain.usecase.GenerateTriviaQuestionUseCase
import com.gimenes.alex.rickandmorty.core.domain.usecase.GetCharacterPoolUseCase
import com.gimenes.alex.rickandmorty.core.domain.usecase.RecordQuizResultUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Drives the full Trivia Quiz flow (issue #15) as a single state machine - see [QuizUiState]'s
 * kdoc for why this is one ViewModel-owned sealed state rather than a nested nav graph.
 *
 * This is the first place in the project that actually wires domain use cases into a screen's
 * behavior (issues #10/#11/#13 landed the use cases themselves but nothing consumed them yet).
 */
class QuizViewModel(
    private val getCharacterPoolUseCase: GetCharacterPoolUseCase,
    private val generateTriviaQuestionUseCase: GenerateTriviaQuestionUseCase,
    private val recordQuizResultUseCase: RecordQuizResultUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.CategorySelect())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    /** In-flight quiz attempt state, `null` outside of Loading/Question/Results. */
    private var session: QuizSession? = null

    /** Selects (or, for locked categories, is a no-op for) a category on the Category Select screen. */
    fun selectCategory(category: QuizCategory) {
        if (!category.isAvailable) return
        val current = _uiState.value as? QuizUiState.CategorySelect ?: return
        _uiState.value = current.copy(selectedCategory = category)
    }

    /** "Start Quiz" - builds a fresh 10-question round for [category] and shows question 1. */
    fun startQuiz(category: QuizCategory) {
        if (!category.isAvailable) return
        loadQuiz(category)
    }

    /** "Retry" from [QuizUiState.NetworkError] / [QuizUiState.Empty] - re-runs the same load. */
    fun retry() {
        val category = session?.category ?: return loadFailedRetryFallback()
        loadQuiz(category)
    }

    /** "Replay" from [QuizUiState.Results] - fresh pool, fresh 10 questions, same category. */
    fun replay() {
        val category = session?.category ?: QuizCategory.CHARACTER
        loadQuiz(category)
    }

    /** Returns to Category Select - used as the secondary "back" action from error/empty states. */
    fun backToCategorySelect() {
        session = null
        _uiState.value = QuizUiState.CategorySelect()
    }

    /**
     * Locks in [optionIndex] as the answer to the current question. No-op if already locked or if
     * not currently showing a question - guards against a double-tap racing the lock.
     */
    fun selectAnswer(optionIndex: Int) {
        val current = _uiState.value as? QuizUiState.Question ?: return
        if (current.isLocked) return
        val activeSession = session ?: return

        val question = activeSession.questions[activeSession.currentIndex]
        val outcome = QuestionOutcome(
            question = question,
            selectedIndex = optionIndex,
        )
        val updatedSession = activeSession.copy(outcomes = activeSession.outcomes + outcome)
        session = updatedSession

        _uiState.value = current.copy(
            selectedOptionIndex = optionIndex,
            isLocked = true,
        )
    }

    /** "Next" - advances to the next question, or finalizes the quiz into Results after question 10. */
    fun nextQuestion() {
        val activeSession = session ?: return
        val nextIndex = activeSession.currentIndex + 1

        if (nextIndex >= activeSession.questions.size) {
            finishQuiz(activeSession)
            return
        }

        val updatedSession = activeSession.copy(currentIndex = nextIndex)
        session = updatedSession
        _uiState.value = updatedSession.toQuestionUiState()
    }

    private fun finishQuiz(activeSession: QuizSession) {
        viewModelScope.launch {
            val answers = activeSession.outcomes.map { it.isCorrect }
            recordQuizResultUseCase(answers)

            val score = activeSession.outcomes.count { it.isCorrect }
            val total = activeSession.outcomes.size
            val missed = activeSession.outcomes
                .filterNot { it.isCorrect }
                .map { outcome ->
                    MissedQuestion(
                        questionText = outcome.question.questionText,
                        chosenAnswer = outcome.question.options[outcome.selectedIndex],
                        correctAnswer = outcome.question.correctAnswer,
                    )
                }

            _uiState.value = QuizUiState.Results(score = score, total = total, missedQuestions = missed)
        }
    }

    private fun loadQuiz(category: QuizCategory) {
        session = null
        _uiState.value = QuizUiState.Loading

        viewModelScope.launch {
            val pool = getCharacterPoolUseCase().first()

            if (pool.isEmpty()) {
                _uiState.value = QuizUiState.NetworkError
                return@launch
            }

            val questions = buildQuestionSet(pool)
            if (questions == null) {
                _uiState.value = QuizUiState.Empty
                return@launch
            }

            val newSession = QuizSession(category = category, questions = questions)
            session = newSession
            _uiState.value = newSession.toQuestionUiState()
        }
    }

    /** Used only if [retry] is called with no prior session (shouldn't normally happen from the UI). */
    private fun loadFailedRetryFallback() {
        loadQuiz(QuizCategory.CHARACTER)
    }

    /**
     * Builds [QUESTIONS_PER_QUIZ] questions from [pool], retrying each individual question's
     * generation up to [MAX_GENERATION_ATTEMPTS_PER_QUESTION] times before giving up on the whole
     * round.
     *
     * [GenerateTriviaQuestionUseCase] can return `null` on an unlucky field/subject pick even
     * against a perfectly healthy pool (see its kdoc) - a single bad random draw shouldn't fail an
     * otherwise-generatable round, so a few retries against the *same* pool (each with a fresh
     * random draw, since [GenerateTriviaQuestionUseCase] defaults to [kotlin.random.Random.Default])
     * are attempted first. Only once a question fails every attempt is the pool treated as genuinely
     * too small/homogeneous to support a full round, surfacing [QuizUiState.Empty] to the caller.
     *
     * ### Why [MAX_GENERATION_ATTEMPTS_PER_QUESTION] is 20, not the "e.g. 5" from the issue
     * [CharacterStatus] only has 3 possible values, so `QuizField.STATUS` can *never* find 3 distinct
     * distractor values no matter how varied [pool] is - it structurally requires 4 distinct option
     * values (1 correct + 3 distractors) from a field whose entire value space has cardinality 3. In
     * practice that means roughly 1 in 4 random field picks is guaranteed to return `null`, so a
     * too-small retry budget would surface spurious [QuizUiState.Empty] results on an otherwise
     * perfectly healthy pool purely from bad luck. Generation is pure/CPU-only with no I/O, so a
     * generous retry count costs nothing observable; 20 attempts drives the odds of a healthy pool
     * failing a single question down to roughly 1 in a trillion (0.25^20), while a pool that's
     * genuinely too small/homogeneous (the real [QuizUiState.Empty] case) still fails fast regardless
     * of the attempt count, since every attempt against it is equally hopeless.
     */
    private fun buildQuestionSet(pool: List<Character>): List<TriviaQuestion>? {
        val questions = mutableListOf<TriviaQuestion>()
        repeat(QUESTIONS_PER_QUIZ) {
            val question = generateQuestionWithRetry(pool) ?: return null
            questions += question
        }
        return questions
    }

    private fun generateQuestionWithRetry(pool: List<Character>): TriviaQuestion? {
        repeat(MAX_GENERATION_ATTEMPTS_PER_QUESTION) {
            generateTriviaQuestionUseCase(pool)?.let { return it }
        }
        return null
    }

    private fun QuizSession.toQuestionUiState(): QuizUiState.Question {
        val question = questions[currentIndex]
        return QuizUiState.Question(
            questionNumber = currentIndex + 1,
            totalQuestions = questions.size,
            questionText = question.questionText,
            options = question.options,
            selectedOptionIndex = null,
            correctOptionIndex = question.correctAnswerIndex,
            isLocked = false,
        )
    }

    companion object {
        const val QUESTIONS_PER_QUIZ = 10
        const val MAX_GENERATION_ATTEMPTS_PER_QUESTION = 20
    }
}

/** One completed question's outcome, kept until the quiz finishes to build the score/review. */
private data class QuestionOutcome(
    val question: TriviaQuestion,
    val selectedIndex: Int,
) {
    val isCorrect: Boolean get() = selectedIndex == question.correctAnswerIndex
}

/** In-flight quiz attempt: the fixed question set plus progress and answers so far. */
private data class QuizSession(
    val category: QuizCategory,
    val questions: List<TriviaQuestion>,
    val currentIndex: Int = 0,
    val outcomes: List<QuestionOutcome> = emptyList(),
)

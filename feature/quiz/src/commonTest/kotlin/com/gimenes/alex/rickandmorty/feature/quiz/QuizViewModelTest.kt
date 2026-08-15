package com.gimenes.alex.rickandmorty.feature.quiz

import com.gimenes.alex.rickandmorty.core.domain.usecase.GenerateTriviaQuestionUseCase
import com.gimenes.alex.rickandmorty.core.domain.usecase.GetCharacterPoolUseCase
import com.gimenes.alex.rickandmorty.core.domain.usecase.RecordQuizResultUseCase
import com.gimenes.alex.rickandmorty.feature.quiz.fake.FakeCharacterRepository
import com.gimenes.alex.rickandmorty.feature.quiz.fake.FakeGameStateRepository
import com.gimenes.alex.rickandmorty.feature.quiz.fake.healthyCharacterPool
import com.gimenes.alex.rickandmorty.feature.quiz.fake.insufficientCharacterPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * [QuizViewModel] tests (issue #15), run against hand-rolled fakes
 * ([FakeCharacterRepository]/[FakeGameStateRepository]) - same spirit as the `core:domain` use-case
 * fakes from issues #10/#13, duplicated locally per that package's kdoc.
 *
 * [Dispatchers.setMain] is overridden with an [UnconfinedTestDispatcher] so that `viewModelScope`
 * launches (backed by `Dispatchers.Main.immediate`) execute synchronously as soon as they're
 * started - none of the fakes have real suspension points, so every ViewModel call in these tests
 * completes its state transition before the call returns, with no need for `runTest`/`advanceUntilIdle`.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class QuizViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        pool: List<com.gimenes.alex.rickandmorty.core.domain.model.Character> = healthyCharacterPool(),
        gameStateRepository: FakeGameStateRepository = FakeGameStateRepository(),
    ): QuizViewModel {
        val characterRepository = FakeCharacterRepository(pool)
        return QuizViewModel(
            getCharacterPoolUseCase = GetCharacterPoolUseCase(characterRepository),
            generateTriviaQuestionUseCase = GenerateTriviaQuestionUseCase(),
            recordQuizResultUseCase = RecordQuizResultUseCase(gameStateRepository),
        )
    }

    @Test
    fun `starting a quiz builds a 10-question set and shows question 1 of 10`() {
        val viewModel = buildViewModel()

        viewModel.selectCategory(QuizCategory.CHARACTER)
        viewModel.startQuiz(QuizCategory.CHARACTER)

        val state = assertIs<QuizUiState.Question>(viewModel.uiState.value)
        assertEquals(1, state.questionNumber)
        assertEquals(10, state.totalQuestions)
        assertEquals(4, state.options.size)
        assertTrue(state.correctOptionIndex in state.options.indices)
        assertEquals(null, state.selectedOptionIndex)
        assertEquals(false, state.isLocked)
    }

    @Test
    fun `answering and tapping next advances through all 10 questions and ends at results`() {
        val viewModel = buildViewModel()
        viewModel.startQuiz(QuizCategory.CHARACTER)

        for (expectedQuestionNumber in 1..10) {
            val state = assertIs<QuizUiState.Question>(viewModel.uiState.value)
            assertEquals(expectedQuestionNumber, state.questionNumber)
            assertEquals(10, state.totalQuestions)
            assertEquals(false, state.isLocked)

            // Answer correctly every time - only used here to drive navigation forward, not to
            // assert on scoring (see the dedicated score-tallying test below).
            viewModel.selectAnswer(state.correctOptionIndex)

            val lockedState = assertIs<QuizUiState.Question>(viewModel.uiState.value)
            assertEquals(true, lockedState.isLocked)
            assertEquals(state.correctOptionIndex, lockedState.selectedOptionIndex)

            viewModel.nextQuestion()
        }

        val results = assertIs<QuizUiState.Results>(viewModel.uiState.value)
        assertEquals(10, results.score)
        assertEquals(10, results.total)
        assertTrue(results.missedQuestions.isEmpty())
    }

    @Test
    fun `score tallying is correct across a mixed sequence of correct and incorrect answers`() {
        val viewModel = buildViewModel()
        viewModel.startQuiz(QuizCategory.CHARACTER)

        // Deterministic mixed pattern: answer questions 1,2,4,5,7,8,9 correctly (7) and
        // 3,6,10 incorrectly (3), regardless of which specific question/option content the
        // pool happened to generate.
        val incorrectQuestionNumbers = setOf(3, 6, 10)

        repeat(10) {
            val state = assertIs<QuizUiState.Question>(viewModel.uiState.value)
            val chosenIndex = if (state.questionNumber in incorrectQuestionNumbers) {
                wrongOptionIndex(state)
            } else {
                state.correctOptionIndex
            }
            viewModel.selectAnswer(chosenIndex)
            viewModel.nextQuestion()
        }

        val results = assertIs<QuizUiState.Results>(viewModel.uiState.value)
        assertEquals(7, results.score)
        assertEquals(10, results.total)
        assertEquals(3, results.missedQuestions.size)
    }

    @Test
    fun `persisted result via RecordQuizResultUseCase matches what results displays`() {
        val gameStateRepository = FakeGameStateRepository()
        val viewModel = buildViewModel(gameStateRepository = gameStateRepository)
        viewModel.startQuiz(QuizCategory.CHARACTER)

        val incorrectQuestionNumbers = setOf(2, 5, 9, 10)
        repeat(10) {
            val state = assertIs<QuizUiState.Question>(viewModel.uiState.value)
            val chosenIndex = if (state.questionNumber in incorrectQuestionNumbers) {
                wrongOptionIndex(state)
            } else {
                state.correctOptionIndex
            }
            viewModel.selectAnswer(chosenIndex)
            viewModel.nextQuestion()
        }

        val results = assertIs<QuizUiState.Results>(viewModel.uiState.value)
        val persisted = gameStateRepository.history.single()

        assertEquals(results.score, persisted.score)
        assertEquals(results.total, persisted.total)
        assertEquals(6, results.score)
    }

    @Test
    fun `an insufficient character pool produces the Empty state instead of crashing or hanging`() {
        val viewModel = buildViewModel(pool = insufficientCharacterPool())

        viewModel.startQuiz(QuizCategory.CHARACTER)

        assertEquals(QuizUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `an empty character pool produces the NetworkError state`() {
        val viewModel = buildViewModel(pool = emptyList())

        viewModel.startQuiz(QuizCategory.CHARACTER)

        assertEquals(QuizUiState.NetworkError, viewModel.uiState.value)
    }

    @Test
    fun `backToCategorySelect resets to an unselected category picker`() {
        val viewModel = buildViewModel(pool = emptyList())
        viewModel.startQuiz(QuizCategory.CHARACTER)
        assertEquals(QuizUiState.NetworkError, viewModel.uiState.value)

        viewModel.backToCategorySelect()

        assertEquals(QuizUiState.CategorySelect(), viewModel.uiState.value)
    }

    /** Picks any option index other than the correct one - always valid since options has 4 entries. */
    private fun wrongOptionIndex(state: QuizUiState.Question): Int =
        state.options.indices.first { it != state.correctOptionIndex }
}

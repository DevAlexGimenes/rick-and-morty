package com.gimenes.alex.rickandmorty.feature.guesscharacter

import com.gimenes.alex.rickandmorty.core.domain.model.Streak
import com.gimenes.alex.rickandmorty.core.domain.usecase.GenerateGuessCharacterRoundUseCase
import com.gimenes.alex.rickandmorty.core.domain.usecase.GetCharacterPoolUseCase
import com.gimenes.alex.rickandmorty.core.domain.usecase.RecordGuessOutcomeUseCase
import com.gimenes.alex.rickandmorty.feature.guesscharacter.fake.FakeCharacterRepository
import com.gimenes.alex.rickandmorty.feature.guesscharacter.fake.FakeGameStateRepository
import com.gimenes.alex.rickandmorty.feature.guesscharacter.fake.healthyCharacterPool
import com.gimenes.alex.rickandmorty.feature.guesscharacter.fake.insufficientCharacterPool
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
 * [GuessCharacterViewModel] tests (issue #16), run against hand-rolled fakes
 * ([FakeCharacterRepository]/[FakeGameStateRepository]) - same spirit as
 * `feature:quiz`'s [com.gimenes.alex.rickandmorty.feature.quiz.QuizViewModelTest] (issue #15).
 *
 * [Dispatchers.setMain] is overridden with an [UnconfinedTestDispatcher] so `viewModelScope`
 * launches execute eagerly. Unlike Quiz, [GuessCharacterViewModel] has real suspension points
 * ([kotlinx.coroutines.delay] for the correct/incorrect feedback pacing - see that class's kdoc),
 * so tests explicitly advance the shared [UnconfinedTestDispatcher]'s virtual clock via
 * `dispatcher.scheduler.advanceUntilIdle()` after any action that triggers a delay, rather than
 * relying on everything completing synchronously.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GuessCharacterViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        pool: List<com.gimenes.alex.rickandmorty.core.domain.model.Character> = healthyCharacterPool(),
        gameStateRepository: FakeGameStateRepository = FakeGameStateRepository(),
    ): GuessCharacterViewModel {
        val characterRepository = FakeCharacterRepository(pool)
        return GuessCharacterViewModel(
            getCharacterPoolUseCase = GetCharacterPoolUseCase(characterRepository),
            generateGuessCharacterRoundUseCase = GenerateGuessCharacterRoundUseCase(),
            recordGuessOutcomeUseCase = RecordGuessOutcomeUseCase(gameStateRepository),
            gameStateRepository = gameStateRepository,
        )
    }

    @Test
    fun `starting a session loads a round and displays the target correctly`() {
        val viewModel = buildViewModel()

        val state = assertIs<GuessCharacterUiState.Round>(viewModel.uiState.value)
        assertEquals(0, state.streak)
        assertEquals(4, state.options.size)
        assertEquals(4, state.options.distinctBy { it.id }.size)
        assertTrue(state.correctOptionIndex in state.options.indices)
        assertEquals(state.target, state.options[state.correctOptionIndex])
        assertEquals(null, state.selectedOptionIndex)
        assertEquals(false, state.isLocked)
    }

    @Test
    fun `correct answer increments the streak and auto-advances to the next round`() {
        val viewModel = buildViewModel()
        val first = assertIs<GuessCharacterUiState.Round>(viewModel.uiState.value)

        viewModel.selectAnswer(first.correctOptionIndex)

        // Immediately after locking (before the feedback delay elapses), the round is locked and
        // marked correct, but hasn't advanced yet.
        val locked = assertIs<GuessCharacterUiState.Round>(viewModel.uiState.value)
        assertEquals(true, locked.isLocked)
        assertEquals(first.correctOptionIndex, locked.selectedOptionIndex)

        dispatcher.scheduler.advanceUntilIdle()

        val next = assertIs<GuessCharacterUiState.Round>(viewModel.uiState.value)
        assertEquals(1, next.streak)
        assertEquals(false, next.isLocked)
        assertEquals(null, next.selectedOptionIndex)
    }

    @Test
    fun `incorrect answer resets the streak, ends the run, and reflects the persisted best streak`() {
        val gameStateRepository = FakeGameStateRepository()
        val viewModel = buildViewModel(gameStateRepository = gameStateRepository)
        val first = assertIs<GuessCharacterUiState.Round>(viewModel.uiState.value)

        viewModel.selectAnswer(wrongOptionIndex(first))
        dispatcher.scheduler.advanceUntilIdle()

        val results = assertIs<GuessCharacterUiState.RunEnded>(viewModel.uiState.value)
        assertEquals(0, results.finalStreak)
        assertEquals(0, gameStateRepository.currentStreak?.current)
        assertEquals(0, results.bestStreakEver)
        assertEquals(false, results.isNewBest)
    }

    @Test
    fun `a mixed sequence of correct, correct, correct, incorrect ends with streak 0 and best at least 3`() {
        val gameStateRepository = FakeGameStateRepository()
        val viewModel = buildViewModel(gameStateRepository = gameStateRepository)

        repeat(3) {
            val state = assertIs<GuessCharacterUiState.Round>(viewModel.uiState.value)
            viewModel.selectAnswer(state.correctOptionIndex)
            dispatcher.scheduler.advanceUntilIdle()
        }

        val beforeMiss = assertIs<GuessCharacterUiState.Round>(viewModel.uiState.value)
        assertEquals(3, beforeMiss.streak)

        viewModel.selectAnswer(wrongOptionIndex(beforeMiss))
        dispatcher.scheduler.advanceUntilIdle()

        val results = assertIs<GuessCharacterUiState.RunEnded>(viewModel.uiState.value)
        assertEquals(3, results.finalStreak)
        assertEquals(true, results.isNewBest)
        assertTrue(results.bestStreakEver >= 3)

        // Persisted repository state: current streak reset to 0, best streak retained at >= 3.
        assertEquals(0, gameStateRepository.currentStreak?.current)
        assertTrue((gameStateRepository.currentStreak?.best ?: 0) >= 3)
    }

    @Test
    fun `run ended shows the persisted best-streak-ever, not just this run's peak, when a prior run set it higher`() {
        val gameStateRepository = FakeGameStateRepository(initialStreak = Streak(current = 0, best = 9, updatedAt = 1))
        val viewModel = buildViewModel(gameStateRepository = gameStateRepository)

        val first = assertIs<GuessCharacterUiState.Round>(viewModel.uiState.value)
        viewModel.selectAnswer(wrongOptionIndex(first))
        dispatcher.scheduler.advanceUntilIdle()

        val results = assertIs<GuessCharacterUiState.RunEnded>(viewModel.uiState.value)
        assertEquals(0, results.finalStreak)
        assertEquals(9, results.bestStreakEver)
        assertEquals(false, results.isNewBest)
    }

    @Test
    fun `manual exit mid-streak ends the run without resetting the persisted current streak to 0 first`() {
        val gameStateRepository = FakeGameStateRepository()
        val viewModel = buildViewModel(gameStateRepository = gameStateRepository)

        repeat(2) {
            val state = assertIs<GuessCharacterUiState.Round>(viewModel.uiState.value)
            viewModel.selectAnswer(state.correctOptionIndex)
            dispatcher.scheduler.advanceUntilIdle()
        }

        val active = assertIs<GuessCharacterUiState.Round>(viewModel.uiState.value)
        assertEquals(2, active.streak)

        viewModel.endRunManually()
        dispatcher.scheduler.advanceUntilIdle()

        val results = assertIs<GuessCharacterUiState.RunEnded>(viewModel.uiState.value)
        assertEquals(2, results.finalStreak)
        assertEquals(true, results.isNewBest)
        // Nothing was discarded: the 2 correct guesses were already persisted as they happened.
        assertEquals(2, gameStateRepository.currentStreak?.current)
    }

    @Test
    fun `an insufficient character pool produces the Empty state instead of crashing or hanging`() {
        val viewModel = buildViewModel(pool = insufficientCharacterPool())

        assertEquals(GuessCharacterUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `an empty character pool produces the NetworkError state`() {
        val viewModel = buildViewModel(pool = emptyList())

        assertEquals(GuessCharacterUiState.NetworkError, viewModel.uiState.value)
    }

    @Test
    fun `play again from Run Ended starts a fresh run with streak visually reset to 0`() {
        val gameStateRepository = FakeGameStateRepository()
        val viewModel = buildViewModel(gameStateRepository = gameStateRepository)
        val first = assertIs<GuessCharacterUiState.Round>(viewModel.uiState.value)

        viewModel.selectAnswer(first.correctOptionIndex)
        dispatcher.scheduler.advanceUntilIdle()
        val second = assertIs<GuessCharacterUiState.Round>(viewModel.uiState.value)
        viewModel.selectAnswer(wrongOptionIndex(second))
        dispatcher.scheduler.advanceUntilIdle()
        assertIs<GuessCharacterUiState.RunEnded>(viewModel.uiState.value)

        viewModel.playAgain()

        val freshRound = assertIs<GuessCharacterUiState.Round>(viewModel.uiState.value)
        assertEquals(0, freshRound.streak)
    }

    /** Picks any option index other than the correct one - always valid since options has 4 entries. */
    private fun wrongOptionIndex(state: GuessCharacterUiState.Round): Int =
        state.options.indices.first { it != state.correctOptionIndex }
}

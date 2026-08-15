package com.gimenes.alex.rickandmorty.core.domain.usecase

import com.gimenes.alex.rickandmorty.core.domain.fake.FakeGameStateRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [RecordGuessOutcomeUseCase] tests (issue #13), run against [FakeGameStateRepository] rather than a
 * real [com.gimenes.alex.rickandmorty.core.domain.repository.GameStateRepository] implementation.
 *
 * Beyond confirming correct delegation (`true`/`false` route to the right repository method), one
 * test also drives a realistic correct/incorrect sequence through the use case and asserts on the
 * resulting current/best streak - the underlying arithmetic is issue #9's responsibility (see
 * [FakeGameStateRepository]'s kdoc), but this most directly demonstrates the acceptance criteria
 * ("best streak updates only when current exceeds prior best") at this use case's own layer.
 */
class RecordGuessOutcomeUseCaseTest {

    @Test
    fun `invoking with true increments the current streak via recordCorrectGuess`() = runTest {
        val repository = FakeGameStateRepository()
        val useCase = RecordGuessOutcomeUseCase(repository)

        useCase(true)

        val streak = repository.getStreak().first()
        assertEquals(1, streak?.current)
        assertEquals(1, streak?.best)
    }

    @Test
    fun `invoking with false routes to recordIncorrectGuess and resets the current streak`() = runTest {
        val repository = FakeGameStateRepository()
        val useCase = RecordGuessOutcomeUseCase(repository)

        useCase(true)
        useCase(true)
        useCase(false)

        val streak = repository.getStreak().first()
        assertEquals(0, streak?.current)
        assertEquals(2, streak?.best)
    }

    @Test
    fun `no rounds recorded yet leaves the streak null`() = runTest {
        val repository = FakeGameStateRepository()

        assertNull(repository.getStreak().first())
    }

    @Test
    fun `a realistic correct-incorrect sequence updates best streak only when current exceeds it`() =
        runTest {
            val repository = FakeGameStateRepository()
            val useCase = RecordGuessOutcomeUseCase(repository)

            useCase(true) // current=1, best=1
            useCase(true) // current=2, best=2
            useCase(false) // current=0, best=2 (unchanged)
            useCase(true) // current=1, best=2 (unchanged, below prior best)

            var streak = repository.getStreak().first()
            assertEquals(1, streak?.current)
            assertEquals(2, streak?.best)

            useCase(true) // current=2, best=2 (ties, not yet exceeded)
            useCase(true) // current=3, best=3 (exceeds prior best)

            streak = repository.getStreak().first()
            assertEquals(3, streak?.current)
            assertEquals(3, streak?.best)
        }
}

package com.gimenes.alex.rickandmorty.core.domain.usecase

import com.gimenes.alex.rickandmorty.core.domain.fake.FakeGameStateRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [RecordQuizResultUseCase] tests (issue #13), run against [FakeGameStateRepository] rather than a
 * real [com.gimenes.alex.rickandmorty.core.domain.repository.GameStateRepository] implementation.
 */
class RecordQuizResultUseCaseTest {

    @Test
    fun `mixed right and wrong sequence is scored correctly`() = runTest {
        val repository = FakeGameStateRepository()
        val useCase = RecordQuizResultUseCase(repository)

        useCase(listOf(true, false, true, true, false))

        val history = repository.getQuizHistory().first()
        assertEquals(1, history.size)
        assertEquals(3, history[0].score)
        assertEquals(5, history[0].total)
    }

    @Test
    fun `all correct sequence scores full marks`() = runTest {
        val repository = FakeGameStateRepository()
        val useCase = RecordQuizResultUseCase(repository)

        useCase(listOf(true, true, true, true))

        val history = repository.getQuizHistory().first()
        assertEquals(4, history[0].score)
        assertEquals(4, history[0].total)
    }

    @Test
    fun `all incorrect sequence scores zero`() = runTest {
        val repository = FakeGameStateRepository()
        val useCase = RecordQuizResultUseCase(repository)

        useCase(listOf(false, false, false))

        val history = repository.getQuizHistory().first()
        assertEquals(0, history[0].score)
        assertEquals(3, history[0].total)
    }

    @Test
    fun `empty answers list is recorded as a zero out of zero result without crashing`() = runTest {
        val repository = FakeGameStateRepository()
        val useCase = RecordQuizResultUseCase(repository)

        useCase(emptyList())

        val history = repository.getQuizHistory().first()
        assertEquals(1, history.size)
        assertEquals(0, history[0].score)
        assertEquals(0, history[0].total)
    }

    @Test
    fun `results are readable after recording via the repository's history and best score getters`() =
        runTest {
            val repository = FakeGameStateRepository()
            val useCase = RecordQuizResultUseCase(repository)

            useCase(listOf(true, false, true))
            useCase(listOf(true, true, true, true))
            useCase(listOf(false, false, true))

            val history = repository.getQuizHistory().first()
            assertEquals(listOf(1, 4, 2), history.map { it.score })
            assertEquals(listOf(3, 4, 3), history.map { it.total })

            val best = repository.getBestScore()
            assertEquals(4, best?.score)
        }
}

package com.gimenes.alex.rickandmorty.core.domain.usecase

import com.gimenes.alex.rickandmorty.core.domain.fake.FakeCharacterRepository
import com.gimenes.alex.rickandmorty.core.domain.fake.testCharacter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [GetCharacterPoolUseCase] tests (issue #10), run against [FakeCharacterRepository] rather than a
 * real [com.gimenes.alex.rickandmorty.core.domain.repository.CharacterRepository] implementation.
 */
class GetCharacterPoolUseCaseTest {

    @Test
    fun `characters with blank image or blank name are filtered out`() = runTest {
        val characters = listOf(
            testCharacter(id = 1),
            testCharacter(id = 2, image = ""),
            testCharacter(id = 3, name = ""),
            testCharacter(id = 4, image = "  "),
            testCharacter(id = 5, name = "  "),
            testCharacter(id = 6),
        )
        val useCase = GetCharacterPoolUseCase(FakeCharacterRepository(characters))

        val result = useCase().first()

        assertEquals(listOf(1, 6), result.map { it.id })
    }

    @Test
    fun `requesting a smaller pool than available returns exactly that many`() = runTest {
        val characters = (1..10).map { testCharacter(id = it) }
        val useCase = GetCharacterPoolUseCase(FakeCharacterRepository(characters))

        val result = useCase(poolSize = 3).first()

        assertEquals(3, result.size)
        assertEquals(listOf(1, 2, 3), result.map { it.id })
    }

    @Test
    fun `requesting more than available returns all available without padding or crashing`() = runTest {
        val characters = (1..5).map { testCharacter(id = it) }
        val useCase = GetCharacterPoolUseCase(FakeCharacterRepository(characters))

        val result = useCase(poolSize = 100).first()

        assertEquals(5, result.size)
        assertEquals(characters, result)
    }

    @Test
    fun `default pool size returns all available when fewer than default exist`() = runTest {
        val characters = (1..20).map { testCharacter(id = it) }
        val useCase = GetCharacterPoolUseCase(FakeCharacterRepository(characters))

        val result = useCase().first()

        assertEquals(20, result.size)
    }

    @Test
    fun `default pool size caps at 100 when more are available`() = runTest {
        val characters = (1..250).map { testCharacter(id = it) }
        val useCase = GetCharacterPoolUseCase(FakeCharacterRepository(characters))

        val result = useCase().first()

        assertEquals(GetCharacterPoolUseCase.DEFAULT_POOL_SIZE, result.size)
        assertTrue(result.all { it.id in 1..100 })
    }

    @Test
    fun `empty repository result yields an empty pool without throwing`() = runTest {
        val useCase = GetCharacterPoolUseCase(FakeCharacterRepository(emptyList()))

        val result = useCase().first()

        assertTrue(result.isEmpty())
    }
}

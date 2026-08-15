package com.gimenes.alex.rickandmorty.core.domain.usecase

import com.gimenes.alex.rickandmorty.core.domain.fake.testCharacter
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterGender
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterStatus
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [GenerateTriviaQuestionUseCase] tests (issue #11) - pure logic, no fakes/mocks needed since this
 * use case has no dependency beyond the [List] of [com.gimenes.alex.rickandmorty.core.domain.model.Character]
 * it's handed.
 */
class GenerateTriviaQuestionUseCaseTest {

    /** A varied, healthy pool: every field has 4+ distinct values across the characters. */
    private val healthyPool = listOf(
        testCharacter(
            id = 1,
            name = "Rick Sanchez",
            status = CharacterStatus.ALIVE,
            species = "Human",
            gender = CharacterGender.MALE,
            origin = "Earth (C-137)",
        ),
        testCharacter(
            id = 2,
            name = "Morty Smith",
            status = CharacterStatus.DEAD,
            species = "Alien",
            gender = CharacterGender.FEMALE,
            origin = "Abadango",
        ),
        testCharacter(
            id = 3,
            name = "Summer Smith",
            status = CharacterStatus.UNKNOWN,
            species = "Humanoid",
            gender = CharacterGender.GENDERLESS,
            origin = "Purge Planet",
        ),
        testCharacter(
            id = 4,
            name = "Beth Smith",
            status = CharacterStatus.ALIVE,
            species = "Robot",
            gender = CharacterGender.UNKNOWN,
            origin = "Nuptia 4",
        ),
        testCharacter(
            id = 5,
            name = "Jerry Smith",
            status = CharacterStatus.DEAD,
            species = "Mytholog",
            gender = CharacterGender.MALE,
            origin = "Signus 5 Expanse",
        ),
    )

    private val useCase = GenerateTriviaQuestionUseCase()

    @Test
    fun `same seed produces the exact same question for the same pool`() {
        val first = useCase(healthyPool, Random(42))
        val second = useCase(healthyPool, Random(42))

        assertEquals(first, second)
    }

    @Test
    fun `generated question has exactly 4 distinct options containing the correct answer once`() {
        val question = useCase(healthyPool, Random(7))

        assertNotNull(question)
        assertEquals(4, question.options.size)
        assertEquals(4, question.options.distinct().size)
        assertEquals(1, question.options.count { it == question.correctAnswer })
        assertTrue(question.correctAnswerIndex in question.options.indices)
    }

    @Test
    fun `pool where no field has enough distinct values returns null`() {
        val uniformPool = (1..5).map {
            testCharacter(
                id = it,
                name = "Clone $it",
                status = CharacterStatus.ALIVE,
                species = "Human",
                gender = CharacterGender.MALE,
                origin = "Earth (C-137)",
            )
        }

        // Regardless of which field/subject the seed happens to pick, every field is uniform
        // across the whole pool, so no (field, subject) combination can find 3 distinct
        // distractors - this must return null no matter the seed.
        repeat(20) { seed ->
            assertNull(useCase(uniformPool, Random(seed.toLong())))
        }
    }

    @Test
    fun `pool smaller than 4 characters returns null without crashing`() {
        val tinyPool = (1..3).map { testCharacter(id = it) }

        assertNull(useCase(tinyPool, Random(1)))
        assertNull(useCase(emptyList(), Random(1)))
    }

    @Test
    fun `many seeds against a healthy pool never produce duplicate options`() {
        repeat(50) { seed ->
            val question = useCase(healthyPool, Random(seed.toLong())) ?: return@repeat
            assertEquals(question.options.size, question.options.distinct().size)
            assertEquals(1, question.options.count { it == question.correctAnswer })
        }
    }
}

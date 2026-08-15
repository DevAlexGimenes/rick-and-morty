package com.gimenes.alex.rickandmorty.core.domain.usecase

import com.gimenes.alex.rickandmorty.core.domain.fake.testCharacter
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterStatus
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [GenerateGuessCharacterRoundUseCase] tests (issue #12) - pure logic, no fakes/mocks needed since
 * this use case has no dependency beyond the [List] of
 * [com.gimenes.alex.rickandmorty.core.domain.model.Character] it's handed.
 */
class GenerateGuessCharacterRoundUseCaseTest {

    /** A varied, healthy pool: plenty of species/status variety across the characters. */
    private val healthyPool = listOf(
        testCharacter(id = 1, name = "Rick Sanchez", status = CharacterStatus.ALIVE, species = "Human"),
        testCharacter(id = 2, name = "Morty Smith", status = CharacterStatus.DEAD, species = "Alien"),
        testCharacter(id = 3, name = "Summer Smith", status = CharacterStatus.UNKNOWN, species = "Humanoid"),
        testCharacter(id = 4, name = "Beth Smith", status = CharacterStatus.ALIVE, species = "Robot"),
        testCharacter(id = 5, name = "Jerry Smith", status = CharacterStatus.DEAD, species = "Mytholog"),
        testCharacter(id = 6, name = "Birdperson", status = CharacterStatus.UNKNOWN, species = "Bird-Person"),
    )

    private val useCase = GenerateGuessCharacterRoundUseCase()

    @Test
    fun `same seed produces the exact same round for the same pool`() {
        val first = useCase(healthyPool, Random(42))
        val second = useCase(healthyPool, Random(42))

        assertEquals(first, second)
    }

    @Test
    fun `generated round has exactly 4 distinct options containing the target once`() {
        val round = useCase(healthyPool, Random(7))

        assertNotNull(round)
        assertEquals(4, round.options.size)
        assertEquals(4, round.options.distinctBy { it.id }.size)
        assertEquals(1, round.options.count { it.id == round.target.id })
        assertTrue(round.correctOptionIndex in round.options.indices)
        assertEquals(round.target, round.options[round.correctOptionIndex])
    }

    @Test
    fun `target is present at the index correctOptionIndex claims across many seeds`() {
        repeat(50) { seed ->
            val round = useCase(healthyPool, Random(seed.toLong())) ?: return@repeat
            assertEquals(round.target.id, round.options[round.correctOptionIndex].id)
            assertEquals(round.options.size, round.options.distinctBy { it.id }.size)
        }
    }

    @Test
    fun `pool smaller than 4 characters returns null without crashing`() {
        val tinyPool = (1..3).map { testCharacter(id = it) }

        assertNull(useCase(tinyPool, Random(1)))
        assertNull(useCase(emptyList(), Random(1)))
    }

    @Test
    fun `falls back to random decoys when target has no same-species-or-status matches`() {
        // Target is the only ALIVE Human in the pool; everyone else is a distinct species and a
        // distinct (non-ALIVE) status, so the preferred tier is empty for this target - but the
        // pool overall still has >= 4 characters, so a round must still be produced by falling
        // back to the rest of the pool.
        val target = testCharacter(id = 1, name = "Target", status = CharacterStatus.ALIVE, species = "Human")
        val pool = listOf(
            target,
            testCharacter(id = 2, name = "Decoy A", status = CharacterStatus.DEAD, species = "Alien"),
            testCharacter(id = 3, name = "Decoy B", status = CharacterStatus.UNKNOWN, species = "Robot"),
            testCharacter(id = 4, name = "Decoy C", status = CharacterStatus.DEAD, species = "Mytholog"),
        )

        // Force this exact character to be picked as the target: with only 4 characters and a
        // pool.random(random) call as the very first draw, seed doesn't matter for *which*
        // element is picked deterministically across platforms, so instead assert over many seeds
        // that whenever this target is chosen, a round is still produced.
        var sawTargetChosen = false
        repeat(50) { seed ->
            val round = useCase(pool, Random(seed.toLong()))
            assertNotNull(round, "expected a round even when the preferred tier is empty")
            assertEquals(4, round.options.distinctBy { it.id }.size)
            if (round.target.id == target.id) sawTargetChosen = true
        }
        assertTrue(sawTargetChosen, "test setup issue: target was never selected across 50 seeds")
    }

    @Test
    fun `prefers same-species-or-status decoys over dissimilar ones when enough are available`() {
        val target = testCharacter(id = 1, name = "Target", status = CharacterStatus.ALIVE, species = "Human")
        val similar = listOf(
            testCharacter(id = 2, name = "Similar A", status = CharacterStatus.ALIVE, species = "Alien"),
            testCharacter(id = 3, name = "Similar B", status = CharacterStatus.DEAD, species = "Human"),
            testCharacter(id = 4, name = "Similar C", status = CharacterStatus.ALIVE, species = "Robot"),
        )
        val dissimilar = listOf(
            testCharacter(id = 5, name = "Dissimilar A", status = CharacterStatus.DEAD, species = "Alien"),
            testCharacter(id = 6, name = "Dissimilar B", status = CharacterStatus.UNKNOWN, species = "Robot"),
            testCharacter(id = 7, name = "Dissimilar C", status = CharacterStatus.DEAD, species = "Mytholog"),
        )
        val pool = listOf(target) + similar + dissimilar
        val similarIds = similar.map { it.id }.toSet()

        repeat(50) { seed ->
            val round = useCase(pool, Random(seed.toLong())) ?: return@repeat
            if (round.target.id != target.id) return@repeat

            val decoyIds = round.options.map { it.id }.filter { it != target.id }.toSet()
            assertEquals(
                similarIds,
                decoyIds,
                "expected all 3 same-species-or-status characters to be preferred over dissimilar ones",
            )
        }
    }
}

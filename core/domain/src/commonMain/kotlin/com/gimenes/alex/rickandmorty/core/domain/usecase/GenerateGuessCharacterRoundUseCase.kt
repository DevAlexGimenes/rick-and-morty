package com.gimenes.alex.rickandmorty.core.domain.usecase

import com.gimenes.alex.rickandmorty.core.domain.model.Character
import com.gimenes.alex.rickandmorty.core.domain.model.GuessCharacterRound
import kotlin.random.Random

/**
 * Generates a single "Guess the Character" [GuessCharacterRound] (issue #12): a target character
 * (shown to the player via [Character.image]) plus 3 decoy characters, all drawn from a pool of
 * characters the caller already has in hand (e.g. [GetCharacterPoolUseCase]'s output, issue #10).
 * This use case has no dependency on that use case, or any repository - it's pure logic over the
 * [List<Character>] it's handed, matching [GenerateTriviaQuestionUseCase]'s (issue #11) established
 * pattern for pure-logic use cases in this module.
 *
 * ### Algorithm
 * 1. Pick one character from [pool] at random as the round's [GuessCharacterRound.target].
 * 2. Partition the rest of the pool (everything but the target) into two tiers:
 *    - **Preferred**: characters sharing the target's [Character.species] *or*
 *      [Character.status] - per LUMA's UX spec, decoys that are categorically similar to the
 *      target make a round harder (and more interesting) than purely random ones.
 *    - **Fallback**: everything else.
 * 3. Fill the 3 decoy slots from the preferred tier first (shuffled, so which preferred
 *    characters get picked when there are more than 3 is itself randomized); if the preferred
 *    tier has fewer than 3 candidates, fill the remaining slots from the shuffled fallback tier.
 * 4. If the two tiers combined can't supply 3 distinct decoys, this (target) pick cannot produce a
 *    valid 4-option round - see "Failure" below.
 * 5. Otherwise combine the target with its 3 decoys, shuffle them together into
 *    [GuessCharacterRound.options], and record where the target landed as
 *    [GuessCharacterRound.correctOptionIndex].
 *
 * ### Failure
 * Returns `null` rather than throwing when [pool] can't support a round - either because it's too
 * small to safely pick a target at all ([pool] must have at least 4 characters), or because the
 * pool minus the target has fewer than 3 characters overall (preferred + fallback combined; this
 * can only happen if [pool] itself has fewer than 4 distinct characters, since the two tiers
 * partition the whole remaining pool). This deliberately does *not* retry with a different target:
 * for a healthy pool (the normal case - dozens of varied characters per [GetCharacterPoolUseCase])
 * this failure path is effectively unreachable, and a retry loop would add complexity without a
 * corresponding real benefit; a caller more concerned with success on a marginal pool can simply
 * call this again (with a fresh random draw) rather than this use case hiding that policy choice
 * internally.
 *
 * ### Determinism
 * All randomness is drawn from the injected [random] (defaulting to [Random.Default] for real use),
 * in a fixed order (target, preferred-tier shuffle, fallback-tier shuffle, final options shuffle) -
 * so a caller supplying a seeded `Random(seed)` gets the exact same [GuessCharacterRound] for the
 * exact same [pool] every time, which is what makes this deterministically testable.
 */
class GenerateGuessCharacterRoundUseCase {

    operator fun invoke(pool: List<Character>, random: Random = Random.Default): GuessCharacterRound? {
        // Need a target plus 3 other characters to have any chance at 3 distinct decoys; also
        // guards Collection.random() below from throwing on an empty pool.
        if (pool.size < 4) return null

        val target = pool.random(random)
        val rest = pool.filter { it.id != target.id }

        if (rest.size < 3) return null

        val (preferred, fallback) = rest.partition {
            it.species == target.species || it.status == target.status
        }

        val decoys = preferred.shuffled(random).take(3).let { chosen ->
            if (chosen.size < 3) {
                chosen + fallback.shuffled(random).take(3 - chosen.size)
            } else {
                chosen
            }
        }

        if (decoys.size < 3) return null

        val options = (decoys + target).shuffled(random)

        return GuessCharacterRound(
            target = target,
            options = options,
            correctOptionIndex = options.indexOfFirst { it.id == target.id },
        )
    }
}

package com.gimenes.alex.rickandmorty.core.domain.model

/**
 * A single generated "Guess the Character" round (issue #12): a [target] character (shown to the
 * player via [Character.image]) plus 3 plausible decoys, produced by
 * [com.gimenes.alex.rickandmorty.core.domain.usecase.GenerateGuessCharacterRoundUseCase].
 *
 * [options] always holds exactly 4 distinct [Character]s (by [Character.id]), including [target]
 * exactly once, in a shuffled order - see that use case's kdoc for how distinctness, count, and
 * decoy selection are guaranteed. [correctOptionIndex] unambiguously identifies which option is
 * [target], mirroring the index-based-correctness pattern
 * [com.gimenes.alex.rickandmorty.core.domain.model.TriviaQuestion] established in issue #11, so
 * there's never a question of matching a character back into [options] if two options happened to
 * look alike (e.g. same name).
 */
data class GuessCharacterRound(
    val target: Character,
    val options: List<Character>,
    val correctOptionIndex: Int,
) {

    /** Convenience accessor for the correct option, derived from [correctOptionIndex]. */
    val correctOption: Character
        get() = options[correctOptionIndex]
}

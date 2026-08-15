package com.gimenes.alex.rickandmorty.core.domain.usecase

import com.gimenes.alex.rickandmorty.core.domain.repository.GameStateRepository

/**
 * Records the outcome of a single Guess the Character round via [GameStateRepository] (issue #13).
 *
 * This is intentionally a thin dispatch and nothing more: the increment/reset/best-update streak
 * arithmetic already lives in [GameStateRepository] ([GameStateRepository.recordCorrectGuess] /
 * [GameStateRepository.recordIncorrectGuess], see that interface's kdoc for why it lives there and
 * not here or in the DAO). Reimplementing any of that logic in this use case would duplicate it
 * across two layers for no benefit - this exists purely so call sites (e.g. the guess-character
 * screen, per round) have a single, obviously-named entry point rather than needing to branch on
 * `correct` themselves.
 */
class RecordGuessOutcomeUseCase(
    private val gameStateRepository: GameStateRepository,
) {

    suspend operator fun invoke(correct: Boolean) {
        if (correct) {
            gameStateRepository.recordCorrectGuess()
        } else {
            gameStateRepository.recordIncorrectGuess()
        }
    }
}

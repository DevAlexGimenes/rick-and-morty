package com.gimenes.alex.rickandmorty.core.domain.usecase

import com.gimenes.alex.rickandmorty.core.domain.repository.GameStateRepository

/**
 * Scores a completed Trivia Quiz run and persists it via [GameStateRepository] (issue #13).
 *
 * ### Scoring
 * [GameStateRepository.recordQuizResult] expects the caller to already have a computed
 * `score`/`total` pair - nothing in the codebase before this issue turns a sequence of per-question
 * outcomes into that pair, so that computation is this use case's actual job. [answers] is the
 * ordered list of per-question outcomes for the completed quiz (`true` = answered correctly), and
 * the score is simply `answers.count { it }` against `total = answers.size`. This deliberately
 * doesn't take any richer per-question data (e.g. which option was picked) - nothing downstream
 * ([GameStateRepository], [com.gimenes.alex.rickandmorty.core.domain.model.QuizResult]) has a use
 * for more than the raw right/wrong tally.
 *
 * ### Empty answers
 * An empty [answers] list scores as `0/0` and is still recorded as-is, rather than being silently
 * skipped. A caller invoking this at all represents a quiz run that the caller considers "completed"
 * (however few questions it contained); this use case isn't in a position to second-guess that and
 * decide a 0-question run isn't worth a history entry - it stays a thin, unconditional wrapper
 * around [GameStateRepository.recordQuizResult], matching this repository's own "never throws"
 * contract. If a future issue decides 0-question runs shouldn't be recorded, that's a policy call
 * for whichever caller can actually observe *why* the list came back empty (e.g. the quiz screen
 * being abandoned early) - not something this use case should silently guess at.
 *
 * ### Why streak state isn't touched here
 * Trivia Quiz and the guess-character streak are two independent game modes/state (see
 * [GameStateRepository]'s kdoc): a completed quiz has no effect on [GameStateRepository.getStreak].
 * Per-round streak updates during Guess the Character are [RecordGuessOutcomeUseCase]'s job.
 */
class RecordQuizResultUseCase(
    private val gameStateRepository: GameStateRepository,
) {

    suspend operator fun invoke(answers: List<Boolean>) {
        val score = answers.count { it }
        val total = answers.size
        gameStateRepository.recordQuizResult(score = score, total = total)
    }
}

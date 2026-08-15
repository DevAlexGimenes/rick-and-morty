package com.gimenes.alex.rickandmorty.core.domain.model

/**
 * Framework-agnostic domain representation of the current guess-character streak state (issue #9).
 *
 * Mirrors `core:database`'s `StreakEntity` (issue #6) minus the Room-only singleton-row `id` - see
 * that entity's kdoc for [current]/[best]/[updatedAt]'s meaning. As with [QuizResult]/[Character],
 * the domain model carries none of the persistence shape's framework concerns.
 *
 * There is no "empty"/zero-value default instance here on purpose: `GameStateRepository.getStreak`
 * returns a nullable `Streak?` for the "no rounds played yet" case rather than a `Streak(0, 0, ...)`
 * sentinel, since there is no meaningful [updatedAt] to report before the first round is ever
 * recorded.
 */
data class Streak(
    val current: Int,
    val best: Int,
    val updatedAt: Long,
)

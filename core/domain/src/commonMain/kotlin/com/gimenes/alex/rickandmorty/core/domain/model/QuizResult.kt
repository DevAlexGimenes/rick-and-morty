package com.gimenes.alex.rickandmorty.core.domain.model

/**
 * Framework-agnostic domain representation of a single completed quiz run (issue #9).
 *
 * Mirrors `core:database`'s `QuizResultEntity` (issue #6) - [score]/[total] stay as separate raw
 * counts rather than a pre-computed percentage, and [completedAt] stays an epoch-millis timestamp,
 * for the same reasons documented on that entity's kdoc - but carries zero Room dependency, matching
 * [Character]'s split between domain model and persistence entity (issue #7). `category` is
 * intentionally not exposed here: nothing in this issue's contract (`GameStateRepository`) needs
 * callers to branch on it yet, and it can be added if/when a later issue actually surfaces multiple
 * quiz categories to the UI.
 */
data class QuizResult(
    val score: Int,
    val total: Int,
    val completedAt: Long,
)

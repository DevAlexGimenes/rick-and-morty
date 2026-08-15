package com.gimenes.alex.rickandmorty.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single completed trivia quiz run (issue #6).
 *
 * Append-only history: each row records the outcome of one finished quiz and is never updated
 * after creation - unlike [CharacterEntity]'s upsert-in-place cache semantics, this table only
 * ever grows via inserts. There is no account/player dimension (confirmed product scope: single
 * local player, no backend, no multiplayer), so a row is identified purely by its
 * auto-generated [id].
 *
 * [category] captures which trivia category the quiz was played in. v1 ships a single live
 * category ("character" quizzes, driven by [CharacterEntity] data), but the column is included
 * now rather than added in a later migration, since NOVA's product scope already anticipates
 * more categories being possible down the line and the column costs nothing to carry today.
 *
 * [score] and [total] are kept as separate raw counts (score out of total) rather than a
 * pre-computed percentage/ratio, so history display and "best score" queries (see
 * [QuizResultDao]) can each decide how to interpret them without losing precision.
 * [completedAt] is an epoch-millis timestamp, matching [CharacterEntity.cachedAt]'s convention
 * for freshness/ordering timestamps in this module.
 */
@Entity(tableName = "quiz_result")
data class QuizResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val score: Int,
    val total: Int,
    val completedAt: Long
)

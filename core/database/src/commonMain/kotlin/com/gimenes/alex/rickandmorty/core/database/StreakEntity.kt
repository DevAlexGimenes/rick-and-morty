package com.gimenes.alex.rickandmorty.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Current guess-character streak state (issue #6).
 *
 * Unlike [QuizResultEntity]'s append-only history, this is mutable *current state*, not a log:
 * there is exactly one active streak conceptually, because there is no account dimension
 * (single local player, no backend, no multiplayer - confirmed product scope). Modeled as a
 * single-row/singleton table, a common Room pattern for app-wide state - [id] is always
 * [SINGLETON_ID], so an [androidx.room.Upsert] against that fixed id always replaces the one
 * existing row instead of accumulating history.
 *
 * [currentStreak] is the in-progress correct-guess streak, reset to 0 on an incorrect guess and
 * incremented on a correct one; [bestStreak] is the highest [currentStreak] has ever reached.
 * The actual game rules (when to increment/reset, and whether a given update should raise
 * [bestStreak]) are deliberately *not* enforced here - see [StreakDao]'s kdoc for why. [updatedAt]
 * is an epoch-millis timestamp, matching [CharacterEntity.cachedAt] / [QuizResultEntity.completedAt].
 */
@Entity(tableName = "streak")
data class StreakEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val currentStreak: Int,
    val bestStreak: Int,
    val updatedAt: Long
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}

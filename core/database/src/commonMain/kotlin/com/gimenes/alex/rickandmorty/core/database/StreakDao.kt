package com.gimenes.alex.rickandmorty.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the single-row guess-character streak state ([StreakEntity], issue #6).
 *
 * Deliberately kept dumb/mechanical: [upsert] persists whatever [StreakEntity] it's given,
 * with no "only raise [StreakEntity.bestStreak] if the new value is actually higher" guard.
 * That business rule (increment-on-correct/reset-on-incorrect, and best-only-updates-if-greater)
 * is domain logic that belongs to the guess-character use case landing in issue #13, not to this
 * data-layer issue (#6, "local game-state schema + DAO" - schema and persistence only). Room can
 * express "only update if greater" as a conditional `UPDATE ... WHERE :value > bestStreak`
 * query, but baking that here would let data-layer code start encoding game rules, which is the
 * wrong layer for it; #13's use case reads the current state via [getStreak], computes the next
 * current/best values itself, and calls [upsert] with the result.
 *
 * [getStreak] is exposed as a [Flow] (nullable - no row exists until the first streak update is
 * ever persisted) rather than a suspend getter, since a live streak counter is a natural
 * "observe current state" UI use case and a `Flow` fits that without meaningfully complicating
 * this issue.
 */
@Dao
interface StreakDao {

    @Upsert
    suspend fun upsert(streak: StreakEntity)

    // Hardcoded to 0 rather than interpolating StreakEntity.SINGLETON_ID: Room's KSP processor
    // requires @Query strings to be resolvable as literal compile-time constants, and keeping
    // the literal here avoids any doubt about how that resolution handles a cross-file const
    // reference. Keep in sync with StreakEntity.SINGLETON_ID.
    @Query("SELECT * FROM streak WHERE id = 0 LIMIT 1")
    fun getStreak(): Flow<StreakEntity?>
}

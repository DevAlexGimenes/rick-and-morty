package com.gimenes.alex.rickandmorty.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the quiz result history ([QuizResultEntity], issue #6).
 *
 * Writes use plain [Insert] rather than [androidx.room.Upsert] like [CharacterDao] - a quiz
 * result is an append-only historical record (see [QuizResultEntity]'s kdoc), so there is no
 * "replace an existing row" case to support; every call to [insert] represents a newly
 * completed quiz run.
 *
 * "Best score" is defined here as the row with the highest raw [QuizResultEntity.score] -
 * the simplest reading of the acceptance criteria's literal wording ("returns ... best score").
 * An alternative (highest score/total *ratio*, so a 3/3 quiz would outrank a 9/10 one) was
 * considered but rejected for this issue: it's a reasonable product refinement but not what the
 * acceptance criteria asks for, and mixing quizzes of different lengths by ratio is itself a
 * product decision NOVA hasn't made explicitly - simplest-correct-thing now, revisit if a later
 * issue wants ratio-based ranking.
 */
@Dao
interface QuizResultDao {

    @Insert
    suspend fun insert(result: QuizResultEntity)

    /** Full history, most recently completed first. */
    @Query("SELECT * FROM quiz_result ORDER BY completedAt DESC")
    fun getHistory(): Flow<List<QuizResultEntity>>

    /**
     * The single highest-scoring run (see class kdoc for how "best" is defined), or `null` if no
     * quiz has been completed yet. Ties broken by most recent (`completedAt DESC`) so the result
     * is deterministic.
     */
    @Query("SELECT * FROM quiz_result ORDER BY score DESC, completedAt DESC LIMIT 1")
    suspend fun getBestScore(): QuizResultEntity?
}

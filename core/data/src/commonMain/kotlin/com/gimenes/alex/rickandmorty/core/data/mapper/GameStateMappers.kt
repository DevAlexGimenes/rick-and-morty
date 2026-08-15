package com.gimenes.alex.rickandmorty.core.data.mapper

import com.gimenes.alex.rickandmorty.core.database.QuizResultEntity
import com.gimenes.alex.rickandmorty.core.database.StreakEntity
import com.gimenes.alex.rickandmorty.core.domain.model.QuizResult
import com.gimenes.alex.rickandmorty.core.domain.model.Streak

/**
 * Entity -> domain mappers for [QuizResult]/[Streak] (issue #9), same rationale as
 * [CharacterMappers]: `core:domain` can't import `core:database`'s entities, so the translation
 * lives here in `core:data`.
 *
 * There are no domain -> entity directions here (unlike [CharacterMappers]'s `Character.toEntity`):
 * `GameStateRepositoryImpl` always constructs the entity itself when writing, since
 * [QuizResultEntity.id]/[StreakEntity.id] are persistence-only concerns (auto-generated primary key /
 * fixed singleton id respectively) that have no domain-model equivalent to map from.
 */

fun QuizResultEntity.toDomain(): QuizResult = QuizResult(
    score = score,
    total = total,
    completedAt = completedAt,
)

fun StreakEntity.toDomain(): Streak = Streak(
    current = currentStreak,
    best = bestStreak,
    updatedAt = updatedAt,
)

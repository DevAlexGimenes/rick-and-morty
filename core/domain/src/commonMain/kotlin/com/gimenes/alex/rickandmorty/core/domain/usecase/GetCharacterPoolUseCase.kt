package com.gimenes.alex.rickandmorty.core.domain.usecase

import com.gimenes.alex.rickandmorty.core.domain.model.Character
import com.gimenes.alex.rickandmorty.core.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Loads a shared pool of [Character]s for the game modes to draw from (issue #10) - both Trivia
 * Quiz and Guess the Character need a broad, unfiltered sample of characters rather than a
 * name/status/species-narrowed search result, so this simply asks [CharacterRepository] for an
 * unfiltered page and shapes the result into something a game mode can safely use.
 *
 * This is the first use-case class in `core:domain`; the convention established here - a small
 * class wrapping a repository call, exposed via `operator fun invoke` - is intended to be followed
 * by later use cases (issues #11/#12/#13) rather than reinvented per-feature.
 *
 * ### Filtering
 * A character is excluded from the pool if it's unusable for a game mode: [Character.image] or
 * [Character.name] is blank. The acceptance criteria describes this as "null image", but per
 * issue #7's design the domain model never uses a nullable `String?` for "missing" - it's always
 * an empty string - so blank/empty is the check that's actually consistent with how this codebase
 * already models "missing" throughout the DTO/entity/domain layers.
 *
 * ### Pool size
 * [poolSize] caps how many characters are returned *after* filtering, defaulting to
 * [DEFAULT_POOL_SIZE] (~100, per the UX spec for Guess the Character). If filtering plus whatever
 * the repository could supply leaves fewer than [poolSize] characters available, what's available
 * is returned as-is - this use case never throws or pads the result, matching
 * [CharacterRepository]'s own "never throws, returns what it has" contract.
 *
 * ### Offline behavior
 * No offline-specific handling lives here on purpose: [CharacterRepository.getCharacters] is
 * already a cold, single-emission-per-collection [Flow] that transparently falls back to its cache
 * on a network failure (issue #8). This use case only maps over that one emission, so the
 * cache-fallback behavior passes through unchanged.
 */
class GetCharacterPoolUseCase(
    private val characterRepository: CharacterRepository,
) {

    operator fun invoke(poolSize: Int = DEFAULT_POOL_SIZE): Flow<List<Character>> =
        characterRepository.getCharacters().map { characters ->
            characters
                .filter { it.name.isNotBlank() && it.image.isNotBlank() }
                .take(poolSize)
        }

    companion object {
        const val DEFAULT_POOL_SIZE = 100
    }
}

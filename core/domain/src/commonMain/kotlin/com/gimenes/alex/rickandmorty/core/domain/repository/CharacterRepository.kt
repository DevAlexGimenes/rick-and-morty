package com.gimenes.alex.rickandmorty.core.domain.repository

import com.gimenes.alex.rickandmorty.core.domain.model.Character
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterGender
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterStatus
import kotlinx.coroutines.flow.Flow

/**
 * Domain-facing contract for reading [Character] data (issue #8).
 *
 * This interface deliberately knows nothing about *how* the data is obtained: it must not
 * reference `core:network`'s `NetworkResult`/`NetworkError`/DTOs or `core:database`'s entities -
 * `core:domain` has zero dependency on either module (see [Character]'s kdoc for the same rule
 * applied to the model itself). The implementation living in `core:data`
 * (`CharacterRepositoryImpl`) is the thing that actually combines the Ktor client (issues #2/#3)
 * with the Room cache (issue #5).
 *
 * ### Why no domain-level error type
 * The acceptance criteria for this issue is explicit: a failed network call with a populated cache
 * must "return cached data without throwing" - i.e. from the caller's point of view, offline-with-
 * cache is a *success*, not a distinguishable failure. Modeling that honestly leads to two options:
 * expose a domain-owned `Result`/error sealed type parallel to `core:network`'s `NetworkResult`, or
 * simply let the contract be "give me the freshest data you can, best-effort" and treat
 * network failure as an internal, silent fallback to cache. The second is what's implemented here:
 * introducing a parallel error hierarchy at this layer would just be forwarding
 * `core:network`'s concerns one layer up without the acceptance criteria ever asking a caller to
 * branch on *why* the data might be stale - that's a call this repository is positioned to make on
 * the caller's behalf. If a future feature genuinely needs to tell "fresh from network" apart from
 * "stale from cache" (e.g. to show a "you're offline" banner), that's the point to revisit this,
 * not preemptively now.
 *
 * ### Why `Flow` and not a suspend function
 * [getCharacters] returns a [Flow] rather than a plain suspend function returning `List<Character>`
 * because it's the natural shape for a screen that collects and recomposes as data changes,
 * matching the rest of this codebase's Compose-based UI layer. It is a *cold*, single-emission-per-
 * collection flow (see `CharacterRepositoryImpl`'s kdoc) rather than a continuously-observing
 * cache stream - each collection triggers exactly one network attempt (with a cache fallback on
 * failure) and emits exactly one list, which keeps the contract simple to reason about and test
 * while still letting callers use `Flow`'s cancellation/composition tools.
 *
 * ### Filters
 * All filter parameters are optional and independent. When the underlying network call succeeds,
 * every parameter that was supplied is honored (the API supports partial matches on [name]/[type]).
 * When the network call fails and the repository falls back to the local cache, only [status] and
 * [species] can actually be honored - the offline cache (`CharacterDao.getFiltered`) only indexes
 * those two columns (see issue #5). This is a deliberate, documented limitation rather than an
 * oversight: building full offline-searchable name/type/gender indexing was out of scope for this
 * issue. Callers that filter by [name]/[type]/[gender] should be aware that an offline fallback may
 * return a broader result set than the same filter would produce online.
 */
interface CharacterRepository {

    /**
     * Fetches a page of characters, network-first with a cache write-through/fallback (see class
     * kdoc for the exact contract). Emits exactly one list per collection: never throws for a
     * network failure, even when the cache is also empty (that case simply emits an empty list -
     * the legitimate "offline without cache" scenario).
     */
    fun getCharacters(
        name: String? = null,
        status: CharacterStatus? = null,
        species: String? = null,
        type: String? = null,
        gender: CharacterGender? = null,
    ): Flow<List<Character>>

    /**
     * Fetches a single character by id, network-first with a cache write-through/fallback. Returns
     * `null` rather than throwing both when the network fails and nothing is cached for [id], and
     * when the network genuinely reports the character doesn't exist and there's no cached copy -
     * distinguishing "not found" from "offline" is left to the use-case/caller layer, which can
     * layer its own signal on top if it ever needs to.
     */
    suspend fun getCharacterById(id: Int): Character?
}

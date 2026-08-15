package com.gimenes.alex.rickandmorty.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Pagination metadata the Rick and Morty API attaches to every list ("info") response: total
 * item [count], total [pages], and the [next]/[prev] page URLs (`null` when there is no such
 * page - e.g. `prev` on page 1, `next` on the last page).
 */
@Serializable
data class PageInfoDto(
    val count: Int = 0,
    val pages: Int = 0,
    val next: String? = null,
    val prev: String? = null,
)

/**
 * Generic envelope for every paginated list endpoint the Rick and Morty API exposes
 * (`/character`, and later `/episode`, `/location`): a [PageInfoDto] plus the page's [results].
 * Kept generic rather than Character-specific so it can be reused as-is once the Episode/Location
 * API clients are added.
 */
@Serializable
data class PagedResponseDto<T>(
    val info: PageInfoDto = PageInfoDto(),
    val results: List<T> = emptyList(),
)

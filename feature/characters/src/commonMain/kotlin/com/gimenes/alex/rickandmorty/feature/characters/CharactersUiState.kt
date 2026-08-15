package com.gimenes.alex.rickandmorty.feature.characters

/**
 * Placeholder UI state for character browsing/detail. Not yet reachable from Home per LUMA's
 * current spec (Home only exposes the two game-mode cards) - scaffolded here because
 * :feature:characters is a distinct module per the architecture, likely to back
 * quiz/guess-character content and/or a future character browser.
 */
data class CharactersUiState(
    val isLoading: Boolean = false
)

package com.gimenes.alex.rickandmorty.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe top-level navigation destinations wired from Home (issue #40's migration off plain
 * string route constants). Each destination is its own `@Serializable` type per
 * `navigation-compose`'s type-safe routes API (`composable<T> { }` /
 * `navController.navigate(T)` / `navController.popBackStack<T>(...)` - see
 * `org.jetbrains.androidx.navigation:navigation-compose`'s docs, no version bump needed on this
 * project's pinned version), grouped under this [Routes] sealed interface purely for
 * discoverability/namespacing (`Routes.Home`, `Routes.Quiz`, ...) - it plays no role in
 * serialization itself, each nested type carries its own independent `@Serializable` route
 * definition rather than a shared polymorphic hierarchy.
 *
 * None of today's destinations take arguments, so each is a no-arg `data object`. A future
 * destination that needs one (e.g. a character-detail screen keyed by id) should follow this same
 * pattern but as a `@Serializable data class` with constructor properties instead, e.g.:
 * ```
 * @Serializable
 * data class CharacterDetail(val characterId: Int) : Routes
 * ```
 * rather than retrofitting a string-based route with manual argument parsing.
 */
sealed interface Routes {
    @Serializable
    data object Home : Routes

    @Serializable
    data object Quiz : Routes

    @Serializable
    data object GuessCharacter : Routes

    @Serializable
    data object Characters : Routes
}

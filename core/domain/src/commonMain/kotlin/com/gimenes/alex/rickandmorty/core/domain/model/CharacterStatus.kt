package com.gimenes.alex.rickandmorty.core.domain.model

/**
 * Type-safe representation of a character's life status.
 *
 * The Rick and Morty API only ever returns `"Alive"`, `"Dead"`, or `"unknown"` for this field, so
 * (unlike [Character.species], which has hundreds of open-ended values and stays a plain [String])
 * it's small and fixed enough to be worth modeling as an enum rather than leaving it stringly-typed
 * at the domain layer.
 *
 * [apiValue] doubles as the canonical wire/cache representation used both when parsing the raw
 * string coming from the DTO/entity layers ([fromRaw]) and when serializing back out to those layers
 * (e.g. `Character.toEntity()`), so the string form is defined in exactly one place.
 */
enum class CharacterStatus(val apiValue: String) {
    ALIVE("Alive"),
    DEAD("Dead"),
    UNKNOWN("unknown");

    companion object {
        /**
         * Parses the raw wire/cache value into a [CharacterStatus], case-insensitively matching it
         * against the known API values. Anything that isn't a recognized value - including the
         * literal `"unknown"`, an empty string, or a genuinely unexpected value - falls back to
         * [UNKNOWN] rather than throwing.
         */
        fun fromRaw(raw: String): CharacterStatus =
            entries.firstOrNull { it.apiValue.equals(raw, ignoreCase = true) } ?: UNKNOWN
    }
}

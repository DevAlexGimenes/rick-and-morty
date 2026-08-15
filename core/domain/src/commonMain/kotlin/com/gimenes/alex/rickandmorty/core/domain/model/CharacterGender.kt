package com.gimenes.alex.rickandmorty.core.domain.model

/**
 * Type-safe representation of a character's gender.
 *
 * As with [CharacterStatus], the API only ever returns `"Male"`, `"Female"`, `"Genderless"`, or
 * `"unknown"` for this field, so it's a small, fixed set worth modeling as an enum rather than a
 * raw [String].
 *
 * [apiValue] is the canonical wire/cache string for each constant, used by both [fromRaw] (parsing)
 * and the reverse mapping back to the DTO/entity layers.
 */
enum class CharacterGender(val apiValue: String) {
    MALE("Male"),
    FEMALE("Female"),
    GENDERLESS("Genderless"),
    UNKNOWN("unknown");

    companion object {
        /**
         * Parses the raw wire/cache value into a [CharacterGender], case-insensitively matching it
         * against the known API values, falling back to [UNKNOWN] for anything unrecognized
         * (including the literal `"unknown"` or an empty string).
         */
        fun fromRaw(raw: String): CharacterGender =
            entries.firstOrNull { it.apiValue.equals(raw, ignoreCase = true) } ?: UNKNOWN
    }
}

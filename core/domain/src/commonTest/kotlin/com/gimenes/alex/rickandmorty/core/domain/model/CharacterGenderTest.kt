package com.gimenes.alex.rickandmorty.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CharacterGenderTest {

    @Test
    fun `fromRaw maps known values case-insensitively`() {
        assertEquals(CharacterGender.MALE, CharacterGender.fromRaw("Male"))
        assertEquals(CharacterGender.MALE, CharacterGender.fromRaw("male"))
        assertEquals(CharacterGender.FEMALE, CharacterGender.fromRaw("Female"))
        assertEquals(CharacterGender.FEMALE, CharacterGender.fromRaw("female"))
        assertEquals(CharacterGender.GENDERLESS, CharacterGender.fromRaw("Genderless"))
        assertEquals(CharacterGender.GENDERLESS, CharacterGender.fromRaw("genderless"))
    }

    @Test
    fun `fromRaw maps literal unknown to UNKNOWN`() {
        assertEquals(CharacterGender.UNKNOWN, CharacterGender.fromRaw("unknown"))
        assertEquals(CharacterGender.UNKNOWN, CharacterGender.fromRaw("Unknown"))
    }

    @Test
    fun `fromRaw maps empty and unexpected values to UNKNOWN`() {
        assertEquals(CharacterGender.UNKNOWN, CharacterGender.fromRaw(""))
        assertEquals(CharacterGender.UNKNOWN, CharacterGender.fromRaw("something else entirely"))
    }

    @Test
    fun `apiValue round-trips through fromRaw`() {
        for (gender in CharacterGender.entries) {
            assertEquals(gender, CharacterGender.fromRaw(gender.apiValue))
        }
    }
}

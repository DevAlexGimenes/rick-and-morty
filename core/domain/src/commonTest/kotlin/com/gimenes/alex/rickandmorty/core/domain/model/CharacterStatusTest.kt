package com.gimenes.alex.rickandmorty.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CharacterStatusTest {

    @Test
    fun `fromRaw maps known values case-insensitively`() {
        assertEquals(CharacterStatus.ALIVE, CharacterStatus.fromRaw("Alive"))
        assertEquals(CharacterStatus.ALIVE, CharacterStatus.fromRaw("alive"))
        assertEquals(CharacterStatus.ALIVE, CharacterStatus.fromRaw("ALIVE"))
        assertEquals(CharacterStatus.DEAD, CharacterStatus.fromRaw("Dead"))
        assertEquals(CharacterStatus.DEAD, CharacterStatus.fromRaw("dead"))
    }

    @Test
    fun `fromRaw maps literal unknown to UNKNOWN`() {
        assertEquals(CharacterStatus.UNKNOWN, CharacterStatus.fromRaw("unknown"))
        assertEquals(CharacterStatus.UNKNOWN, CharacterStatus.fromRaw("Unknown"))
    }

    @Test
    fun `fromRaw maps empty and unexpected values to UNKNOWN`() {
        assertEquals(CharacterStatus.UNKNOWN, CharacterStatus.fromRaw(""))
        assertEquals(CharacterStatus.UNKNOWN, CharacterStatus.fromRaw("something else entirely"))
    }

    @Test
    fun `apiValue round-trips through fromRaw`() {
        for (status in CharacterStatus.entries) {
            assertEquals(status, CharacterStatus.fromRaw(status.apiValue))
        }
    }
}

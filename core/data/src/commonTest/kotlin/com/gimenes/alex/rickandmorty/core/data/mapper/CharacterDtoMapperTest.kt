package com.gimenes.alex.rickandmorty.core.data.mapper

import com.gimenes.alex.rickandmorty.core.domain.model.CharacterGender
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterStatus
import com.gimenes.alex.rickandmorty.core.network.dto.CharacterDto
import com.gimenes.alex.rickandmorty.core.network.dto.LocationReferenceDto
import kotlin.test.Test
import kotlin.test.assertEquals

class CharacterDtoMapperTest {

    private val rickDto = CharacterDto(
        id = 1,
        name = "Rick Sanchez",
        status = "Alive",
        species = "Human",
        type = "",
        gender = "Male",
        origin = LocationReferenceDto(name = "Earth (C-137)", url = "https://rickandmortyapi.com/api/location/1"),
        location = LocationReferenceDto(name = "Citadel of Ricks", url = "https://rickandmortyapi.com/api/location/3"),
        image = "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
        episode = listOf(
            "https://rickandmortyapi.com/api/episode/1",
            "https://rickandmortyapi.com/api/episode/2",
        ),
        url = "https://rickandmortyapi.com/api/character/1",
        created = "2017-11-04T18:48:46.250Z",
    )

    @Test
    fun `maps full field set for a realistic fixture`() {
        val domain = rickDto.toDomain()

        assertEquals(1, domain.id)
        assertEquals("Rick Sanchez", domain.name)
        assertEquals(CharacterStatus.ALIVE, domain.status)
        assertEquals("Human", domain.species)
        assertEquals("", domain.type)
        assertEquals(CharacterGender.MALE, domain.gender)
        assertEquals("Earth (C-137)", domain.origin)
        assertEquals("Citadel of Ricks", domain.location)
        assertEquals("https://rickandmortyapi.com/api/character/avatar/1.jpeg", domain.image)
        assertEquals(
            listOf(
                "https://rickandmortyapi.com/api/episode/1",
                "https://rickandmortyapi.com/api/episode/2",
            ),
            domain.episode,
        )
        assertEquals("https://rickandmortyapi.com/api/character/1", domain.url)
        assertEquals("2017-11-04T18:48:46.250Z", domain.created)
    }

    @Test
    fun `maps known status values`() {
        assertEquals(CharacterStatus.ALIVE, rickDto.copy(status = "Alive").toDomain().status)
        assertEquals(CharacterStatus.DEAD, rickDto.copy(status = "Dead").toDomain().status)
    }

    @Test
    fun `maps unknown, empty, and unexpected status values to UNKNOWN`() {
        assertEquals(CharacterStatus.UNKNOWN, rickDto.copy(status = "unknown").toDomain().status)
        assertEquals(CharacterStatus.UNKNOWN, rickDto.copy(status = "").toDomain().status)
        assertEquals(CharacterStatus.UNKNOWN, rickDto.copy(status = "Zombie").toDomain().status)
    }

    @Test
    fun `maps known gender values`() {
        assertEquals(CharacterGender.MALE, rickDto.copy(gender = "Male").toDomain().gender)
        assertEquals(CharacterGender.FEMALE, rickDto.copy(gender = "Female").toDomain().gender)
        assertEquals(CharacterGender.GENDERLESS, rickDto.copy(gender = "Genderless").toDomain().gender)
    }

    @Test
    fun `maps unknown, empty, and unexpected gender values to UNKNOWN`() {
        assertEquals(CharacterGender.UNKNOWN, rickDto.copy(gender = "unknown").toDomain().gender)
        assertEquals(CharacterGender.UNKNOWN, rickDto.copy(gender = "").toDomain().gender)
        assertEquals(CharacterGender.UNKNOWN, rickDto.copy(gender = "Alien").toDomain().gender)
    }

    @Test
    fun `flattens origin and location reference to their name`() {
        val dto = rickDto.copy(
            origin = LocationReferenceDto(name = "unknown", url = ""),
            location = LocationReferenceDto(name = "unknown", url = ""),
        )

        val domain = dto.toDomain()

        assertEquals("unknown", domain.origin)
        assertEquals("unknown", domain.location)
    }

    @Test
    fun `maps a fully blank-default dto without throwing`() {
        val domain = CharacterDto(id = 42).toDomain()

        assertEquals(42, domain.id)
        assertEquals("", domain.name)
        assertEquals(CharacterStatus.UNKNOWN, domain.status)
        assertEquals(CharacterGender.UNKNOWN, domain.gender)
        assertEquals("", domain.origin)
        assertEquals("", domain.location)
        assertEquals(emptyList(), domain.episode)
    }
}

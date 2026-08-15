package com.gimenes.alex.rickandmorty.core.data.mapper

import com.gimenes.alex.rickandmorty.core.database.CharacterEntity
import com.gimenes.alex.rickandmorty.core.domain.model.Character
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterGender
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class CharacterEntityMapperTest {

    private val rickEntity = CharacterEntity(
        id = 1,
        name = "Rick Sanchez",
        status = "Alive",
        species = "Human",
        type = "",
        gender = "Male",
        originName = "Earth (C-137)",
        locationName = "Citadel of Ricks",
        imageUrl = "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
        cachedAt = 1_700_000_000_000L,
    )

    @Test
    fun `entity toDomain maps full field set`() {
        val domain = rickEntity.toDomain()

        assertEquals(1, domain.id)
        assertEquals("Rick Sanchez", domain.name)
        assertEquals(CharacterStatus.ALIVE, domain.status)
        assertEquals("Human", domain.species)
        assertEquals("", domain.type)
        assertEquals(CharacterGender.MALE, domain.gender)
        assertEquals("Earth (C-137)", domain.origin)
        assertEquals("Citadel of Ricks", domain.location)
        assertEquals("https://rickandmortyapi.com/api/character/avatar/1.jpeg", domain.image)
    }

    @Test
    fun `entity toDomain maps known status and gender values`() {
        assertEquals(CharacterStatus.DEAD, rickEntity.copy(status = "Dead").toDomain().status)
        assertEquals(CharacterGender.FEMALE, rickEntity.copy(gender = "Female").toDomain().gender)
        assertEquals(CharacterGender.GENDERLESS, rickEntity.copy(gender = "Genderless").toDomain().gender)
    }

    @Test
    fun `entity toDomain maps unknown, empty, and unexpected status and gender to UNKNOWN`() {
        assertEquals(CharacterStatus.UNKNOWN, rickEntity.copy(status = "unknown").toDomain().status)
        assertEquals(CharacterStatus.UNKNOWN, rickEntity.copy(status = "").toDomain().status)
        assertEquals(CharacterStatus.UNKNOWN, rickEntity.copy(status = "Zombie").toDomain().status)

        assertEquals(CharacterGender.UNKNOWN, rickEntity.copy(gender = "unknown").toDomain().gender)
        assertEquals(CharacterGender.UNKNOWN, rickEntity.copy(gender = "").toDomain().gender)
        assertEquals(CharacterGender.UNKNOWN, rickEntity.copy(gender = "Alien").toDomain().gender)
    }

    @Test
    fun `domain toEntity maps full field set and serializes enums back to API casing`() {
        val domain = Character(
            id = 1,
            name = "Rick Sanchez",
            status = CharacterStatus.ALIVE,
            species = "Human",
            type = "",
            gender = CharacterGender.MALE,
            origin = "Earth (C-137)",
            location = "Citadel of Ricks",
            image = "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
            episode = listOf("https://rickandmortyapi.com/api/episode/1"),
            url = "https://rickandmortyapi.com/api/character/1",
            created = "2017-11-04T18:48:46.250Z",
        )

        val entity = domain.toEntity(cachedAt = 1_700_000_000_000L)

        assertEquals(1, entity.id)
        assertEquals("Rick Sanchez", entity.name)
        assertEquals("Alive", entity.status)
        assertEquals("Human", entity.species)
        assertEquals("", entity.type)
        assertEquals("Male", entity.gender)
        assertEquals("Earth (C-137)", entity.originName)
        assertEquals("Citadel of Ricks", entity.locationName)
        assertEquals("https://rickandmortyapi.com/api/character/avatar/1.jpeg", entity.imageUrl)
        assertEquals(1_700_000_000_000L, entity.cachedAt)
    }

    @Test
    fun `domain toEntity serializes UNKNOWN status and gender back to the literal unknown string`() {
        val domain = Character(
            id = 2,
            name = "Unknown Guy",
            status = CharacterStatus.UNKNOWN,
            species = "",
            type = "",
            gender = CharacterGender.UNKNOWN,
            origin = "",
            location = "",
            image = "",
            episode = emptyList(),
            url = "",
            created = "",
        )

        val entity = domain.toEntity(cachedAt = 0L)

        assertEquals("unknown", entity.status)
        assertEquals("unknown", entity.gender)
    }

    @Test
    fun `entity to domain to entity round-trips the fields the entity actually carries`() {
        val roundTripped = rickEntity.toDomain().toEntity(cachedAt = rickEntity.cachedAt)

        assertEquals(rickEntity, roundTripped)
    }
}

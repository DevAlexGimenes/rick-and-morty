package com.gimenes.alex.rickandmorty.core.domain.fake

import com.gimenes.alex.rickandmorty.core.domain.model.Character
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterGender
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterStatus
import com.gimenes.alex.rickandmorty.core.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Hand-rolled fake of [CharacterRepository] for testing use cases that depend on it (issue #10).
 *
 * Use cases in `core:domain` must not depend on `core:data`'s real `CharacterRepositoryImpl` or its
 * network/DB test infrastructure, so this fakes the interface directly rather than reusing the
 * `core:data` fakes (e.g. `FakeCharacterDao`), which fake Room, not the domain contract.
 */
class FakeCharacterRepository(
    private val characters: List<Character> = emptyList(),
) : CharacterRepository {

    override fun getCharacters(
        name: String?,
        status: CharacterStatus?,
        species: String?,
        type: String?,
        gender: CharacterGender?,
    ): Flow<List<Character>> = flowOf(characters)

    override suspend fun getCharacterById(id: Int): Character? =
        characters.firstOrNull { it.id == id }
}

/**
 * Builds a minimal, valid [Character] for tests, with every field overridable so a test can create
 * "broken" fixtures (blank name/image) by only overriding what it needs.
 */
fun testCharacter(
    id: Int,
    name: String = "Character $id",
    image: String = "https://example.com/$id.jpeg",
    status: CharacterStatus = CharacterStatus.ALIVE,
    species: String = "Human",
    type: String = "",
    gender: CharacterGender = CharacterGender.UNKNOWN,
    origin: String = "Earth",
    location: String = "Earth",
    episode: List<String> = emptyList(),
    url: String = "https://rickandmortyapi.com/api/character/$id",
    created: String = "2017-11-04T18:48:46.250Z",
): Character = Character(
    id = id,
    name = name,
    status = status,
    species = species,
    type = type,
    gender = gender,
    origin = origin,
    location = location,
    image = image,
    episode = episode,
    url = url,
    created = created,
)

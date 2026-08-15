package com.gimenes.alex.rickandmorty.feature.guesscharacter.fake

import com.gimenes.alex.rickandmorty.core.domain.model.Character
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterGender
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterStatus
import com.gimenes.alex.rickandmorty.core.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Hand-rolled fake of [CharacterRepository] for testing
 * [com.gimenes.alex.rickandmorty.feature.guesscharacter.GuessCharacterViewModel] (issue #16) -
 * mirrors `feature:quiz`'s own local fake (issue #15), duplicated locally because commonTest
 * sources aren't shared across modules in this project's Gradle setup.
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
 * A varied, healthy pool - plenty of species/status variety so
 * [com.gimenes.alex.rickandmorty.core.domain.usecase.GenerateGuessCharacterRoundUseCase] can
 * always produce a round regardless of which target it randomly picks.
 */
fun healthyCharacterPool(): List<Character> = listOf(
    testCharacter(id = 1, name = "Rick Sanchez", status = CharacterStatus.ALIVE, species = "Human"),
    testCharacter(id = 2, name = "Morty Smith", status = CharacterStatus.DEAD, species = "Alien"),
    testCharacter(id = 3, name = "Summer Smith", status = CharacterStatus.UNKNOWN, species = "Humanoid"),
    testCharacter(id = 4, name = "Beth Smith", status = CharacterStatus.ALIVE, species = "Robot"),
    testCharacter(id = 5, name = "Jerry Smith", status = CharacterStatus.DEAD, species = "Mytholog"),
    testCharacter(id = 6, name = "Birdperson", status = CharacterStatus.UNKNOWN, species = "Bird-Person"),
)

/**
 * A pool too small for [com.gimenes.alex.rickandmorty.core.domain.usecase.GenerateGuessCharacterRoundUseCase]
 * to ever build a round from (it requires at least 4 characters) - used to drive
 * [com.gimenes.alex.rickandmorty.feature.guesscharacter.GuessCharacterUiState.Empty].
 */
fun insufficientCharacterPool(): List<Character> = listOf(
    testCharacter(id = 1, name = "Rick Sanchez"),
    testCharacter(id = 2, name = "Morty Smith"),
    testCharacter(id = 3, name = "Summer Smith"),
)

/** Builds a minimal, valid [Character] for tests, with every field overridable. */
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

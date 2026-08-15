package com.gimenes.alex.rickandmorty.feature.quiz.fake

import com.gimenes.alex.rickandmorty.core.domain.model.Character
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterGender
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterStatus
import com.gimenes.alex.rickandmorty.core.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Hand-rolled fake of [CharacterRepository] for testing [com.gimenes.alex.rickandmorty.feature.quiz.QuizViewModel]
 * (issue #15) - mirrors `core:domain`'s own `FakeCharacterRepository` test fixture (issue #10),
 * duplicated locally because commonTest sources aren't shared across modules in this project's
 * Gradle setup.
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
 * A varied, "healthy" pool where every quizable field (status/species/gender/origin) has enough
 * distinct values that [com.gimenes.alex.rickandmorty.core.domain.usecase.GenerateTriviaQuestionUseCase]
 * can always produce a question regardless of which field/subject/seed it randomly picks - see that
 * use case's own test fixture for the same shape. Kept small (5 characters) but fully distinct per
 * field so question generation is deterministic in outcome (always succeeds) even though the
 * specific field/subject/option order isn't.
 */
fun healthyCharacterPool(): List<Character> = listOf(
    testCharacter(
        id = 1,
        name = "Rick Sanchez",
        status = CharacterStatus.ALIVE,
        species = "Human",
        gender = CharacterGender.MALE,
        origin = "Earth (C-137)",
    ),
    testCharacter(
        id = 2,
        name = "Morty Smith",
        status = CharacterStatus.DEAD,
        species = "Alien",
        gender = CharacterGender.FEMALE,
        origin = "Abadango",
    ),
    testCharacter(
        id = 3,
        name = "Summer Smith",
        status = CharacterStatus.UNKNOWN,
        species = "Humanoid",
        gender = CharacterGender.GENDERLESS,
        origin = "Purge Planet",
    ),
    testCharacter(
        id = 4,
        name = "Beth Smith",
        status = CharacterStatus.ALIVE,
        species = "Robot",
        gender = CharacterGender.UNKNOWN,
        origin = "Nuptia 4",
    ),
    testCharacter(
        id = 5,
        name = "Jerry Smith",
        status = CharacterStatus.DEAD,
        species = "Mytholog",
        gender = CharacterGender.MALE,
        origin = "Signus 5 Expanse",
    ),
)

/**
 * A pool too small for [com.gimenes.alex.rickandmorty.core.domain.usecase.GenerateTriviaQuestionUseCase]
 * to ever build a question from (it requires at least 4 characters) - used to drive
 * [com.gimenes.alex.rickandmorty.feature.quiz.QuizUiState.Empty].
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

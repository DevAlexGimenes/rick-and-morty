package com.gimenes.alex.rickandmorty.core.domain.usecase

import com.gimenes.alex.rickandmorty.core.domain.model.Character
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterGender
import com.gimenes.alex.rickandmorty.core.domain.model.CharacterStatus
import com.gimenes.alex.rickandmorty.core.domain.model.TriviaQuestion
import kotlin.random.Random

/**
 * Generates a single "Character" category [TriviaQuestion] (issue #11) - a fact about one
 * character (status/species/gender/origin) plus 3 plausible wrong options, all drawn from a pool
 * of characters the caller already has in hand (e.g. [GetCharacterPoolUseCase]'s output, issue
 * #10). This use case has no dependency on that use case, or any repository - it's pure logic over
 * the [List<Character>] it's handed, matching NOVA/LUMA's v1 scope of Character-fact-only trivia
 * questions with 4 multiple-choice options.
 *
 * ### Algorithm
 * 1. Pick one of the four quizable fields ([QuizField]) at random.
 * 2. Pick one character from [pool] at random as the question's subject; that character's value
 *    for the chosen field becomes the correct answer, and its name drives the question text.
 * 3. Collect the *distinct* values the rest of the pool has for that field, excluding the subject
 *    itself and excluding anything equal to the correct answer (so a distractor can never silently
 *    equal the correct answer just because another character happens to share it).
 * 4. If fewer than 3 distinct distractor values are available, this (field, subject) combination
 *    cannot produce a valid 4-option question - see "Failure" below.
 * 5. Otherwise pick 3 of those distinct values at random, shuffle them together with the correct
 *    answer, and build the [TriviaQuestion].
 *
 * ### Failure
 * Returns `null` rather than throwing when [pool] can't support a question - either because it's
 * too small to safely pick a subject at all, or because the randomly chosen field/subject
 * combination didn't yield 3 distinct distractors (e.g. every character in the pool shares the same
 * species). This deliberately does *not* retry with a different field or subject: for a healthy
 * pool (the normal case - dozens of varied characters per [GetCharacterPoolUseCase]) the odds of an
 * unlucky pick are negligible, and a retry loop would add complexity without a corresponding real
 * benefit; a pool that's genuinely too narrow to quiz on will fail the same way on any field, so a
 * caller more concerned with success on a marginal pool can simply call this again (with a fresh
 * random draw) rather than this use case hiding that policy choice internally.
 *
 * ### Determinism
 * All randomness is drawn from the injected [random] (defaulting to [Random.Default] for real use),
 * in a fixed order (field, subject, distractor selection, final shuffle) - so a caller supplying a
 * seeded `Random(seed)` gets the exact same [TriviaQuestion] for the exact same [pool] every time,
 * which is what makes this deterministically testable.
 *
 * ### Option text
 * [Character.species] and [Character.origin] are already presentable display strings, used as-is.
 * [Character.status] and [Character.gender] are enums; their [CharacterStatus.apiValue] /
 * [CharacterGender.apiValue] are presentable for every value except [CharacterStatus.UNKNOWN] /
 * [CharacterGender.UNKNOWN], whose `apiValue` is the lowercase `"unknown"` (chosen to match the raw
 * API string, not for display). [displayValue] capitalizes that one case so it reads as "Unknown"
 * like a normal option rather than visually standing out from the other capitalized values.
 */
class GenerateTriviaQuestionUseCase {

    operator fun invoke(pool: List<Character>, random: Random = Random.Default): TriviaQuestion? {
        // Need at least a subject plus 3 other characters to have any chance at 3 distinct
        // distractors; also guards Collection.random() below from throwing on an empty pool.
        if (pool.size < 4) return null

        val field = QuizField.entries.random(random)
        val subject = pool.random(random)
        val correctAnswer = field.valueOf(subject)

        val distractorCandidates = pool
            .asSequence()
            .filter { it.id != subject.id }
            .map(field::valueOf)
            .filter { it != correctAnswer }
            .distinct()
            .toList()

        if (distractorCandidates.size < 3) return null

        val distractors = distractorCandidates.shuffled(random).take(3)
        val options = (distractors + correctAnswer).shuffled(random)

        return TriviaQuestion(
            questionText = field.questionText(subject.name),
            options = options,
            correctAnswerIndex = options.indexOf(correctAnswer),
        )
    }

    /**
     * The four Character-category facts this use case can quiz on, each knowing how to read its
     * own value off a [Character] and how to phrase its question text.
     */
    private enum class QuizField {
        STATUS {
            override fun valueOf(character: Character) = character.status.displayValue()
            override fun questionText(name: String) = "What is $name's status?"
        },
        SPECIES {
            override fun valueOf(character: Character) = character.species
            override fun questionText(name: String) = "What species is $name?"
        },
        GENDER {
            override fun valueOf(character: Character) = character.gender.displayValue()
            override fun questionText(name: String) = "What is $name's gender?"
        },
        ORIGIN {
            override fun valueOf(character: Character) = character.origin
            override fun questionText(name: String) = "Where is $name from?"
        },
        ;

        abstract fun valueOf(character: Character): String
        abstract fun questionText(name: String): String
    }
}

private fun CharacterStatus.displayValue(): String = apiValue.replaceFirstChar { it.uppercase() }

private fun CharacterGender.displayValue(): String = apiValue.replaceFirstChar { it.uppercase() }

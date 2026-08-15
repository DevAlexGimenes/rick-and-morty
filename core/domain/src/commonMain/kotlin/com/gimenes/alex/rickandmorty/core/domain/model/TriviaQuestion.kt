package com.gimenes.alex.rickandmorty.core.domain.model

/**
 * A single generated multiple-choice trivia question about one [Character] fact (issue #11),
 * produced by
 * [com.gimenes.alex.rickandmorty.core.domain.usecase.GenerateTriviaQuestionUseCase].
 *
 * [options] always holds exactly 4 distinct, presentable strings (see that use case's kdoc for how
 * distinctness and count are guaranteed), and [correctAnswerIndex] unambiguously identifies which
 * one is correct - an index rather than a duplicate "correct answer" string field, so there's never
 * a question of matching a value back into [options] if two options happened to look alike.
 */
data class TriviaQuestion(
    val questionText: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
) {

    /** Convenience accessor for the correct option's text, derived from [correctAnswerIndex]. */
    val correctAnswer: String
        get() = options[correctAnswerIndex]
}

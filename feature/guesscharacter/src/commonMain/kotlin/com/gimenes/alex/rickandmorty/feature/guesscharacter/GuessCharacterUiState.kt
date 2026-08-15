package com.gimenes.alex.rickandmorty.feature.guesscharacter

import com.gimenes.alex.rickandmorty.core.domain.model.Character

/**
 * The full Guess the Character flow (issue #16): Loading -> Round (Character Reveal + Choices,
 * looping) -> Feedback -> ... -> Run Ended -> Play Again / Home, modeled as a single sealed
 * [GuessCharacterUiState] driven entirely by [GuessCharacterViewModel] - mirroring
 * [com.gimenes.alex.rickandmorty.feature.quiz.QuizUiState]'s (issue #15) rationale for why this is
 * one ViewModel-owned state machine rather than a nested navigation graph. See that file's kdoc for
 * the full reasoning; it applies unchanged here.
 *
 * Unlike Trivia Quiz there is no `CategorySelect` step - Guess the Character has exactly one mode,
 * so the flow starts straight at [Loading].
 *
 * Answer Feedback is, again, not its own variant - it is [Round] with [Round.isLocked] set to
 * `true`, for the same reason [com.gimenes.alex.rickandmorty.feature.quiz.QuizUiState.Question]
 * folds feedback into itself rather than duplicating every round field across two variants.
 *
 * The mid-play "manual exit" confirmation dialog is deliberately *not* modeled as a state here - see
 * [GuessCharacterScreen]'s kdoc for why that's kept as transient, screen-local UI state instead.
 */
sealed interface GuessCharacterUiState {

    /** Character pool fetch / first round build in progress. Expected to be near-instant. */
    data object Loading : GuessCharacterUiState

    /**
     * [com.gimenes.alex.rickandmorty.core.domain.usecase.GetCharacterPoolUseCase] legitimately
     * returned an empty pool - cold start, no cache, no network. Distinct from [Empty]: this means
     * there was no data to work with at all, not merely "not enough variety in what we did get".
     * Mirrors [com.gimenes.alex.rickandmorty.feature.quiz.QuizUiState.NetworkError].
     */
    data object NetworkError : GuessCharacterUiState

    /**
     * The character pool came back non-empty but couldn't support even a single round (needs >= 4
     * characters, see [com.gimenes.alex.rickandmorty.core.domain.usecase.GenerateGuessCharacterRoundUseCase])
     * even after retrying - see [GuessCharacterViewModel]'s round-generation retry strategy. Mirrors
     * [com.gimenes.alex.rickandmorty.feature.quiz.QuizUiState.Empty].
     */
    data object Empty : GuessCharacterUiState

    /**
     * A single round on screen: guess [target] from [options] (always 4 distinct characters,
     * including [target] once - see
     * [com.gimenes.alex.rickandmorty.core.domain.model.GuessCharacterRound]).
     *
     * [streak] is the *live, persisted* current streak for this run - already reflecting every
     * correct guess so far (each one is persisted via
     * [com.gimenes.alex.rickandmorty.core.domain.usecase.RecordGuessOutcomeUseCase] as it happens,
     * per the UX spec's "streak is already persisted incrementally" note), so it's always safe to
     * display and never needs a separate "optimistic" local counter.
     *
     * [correctOptionIndex] is always populated, but - matching Trivia's convention - callers must
     * only reveal it in the UI once [isLocked] is `true`.
     */
    data class Round(
        val streak: Int,
        val target: Character,
        val options: List<Character>,
        val correctOptionIndex: Int,
        val selectedOptionIndex: Int?,
        val isLocked: Boolean,
    ) : GuessCharacterUiState

    /**
     * The run is over - triggered either by a miss (see [GuessCharacterViewModel.selectAnswer]) or
     * a manual mid-streak exit (see [GuessCharacterViewModel.endRunManually]). Neither path loses
     * any data: every correct/incorrect guess was already persisted as it happened.
     *
     * [finalStreak] is the *peak* streak this run reached (i.e. the streak count going into the
     * round that ended the run) - not the post-reset current streak, which would always render as
     * an anticlimactic 0 on a miss. [bestStreakEver] is the persisted best-streak-ever across all
     * runs (from [com.gimenes.alex.rickandmorty.core.domain.repository.GameStateRepository.getStreak]),
     * which by construction is always >= [finalStreak]. [isNewBest] is `true` when this run's peak
     * exceeded whatever the best-ever was *before* this run started, i.e. this run is the reason
     * [bestStreakEver] is what it is.
     */
    data class RunEnded(
        val finalStreak: Int,
        val bestStreakEver: Int,
        val isNewBest: Boolean,
    ) : GuessCharacterUiState
}

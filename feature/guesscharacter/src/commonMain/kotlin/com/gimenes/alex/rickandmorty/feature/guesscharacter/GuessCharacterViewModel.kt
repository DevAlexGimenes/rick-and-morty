package com.gimenes.alex.rickandmorty.feature.guesscharacter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gimenes.alex.rickandmorty.core.domain.model.Character
import com.gimenes.alex.rickandmorty.core.domain.model.GuessCharacterRound
import com.gimenes.alex.rickandmorty.core.domain.repository.GameStateRepository
import com.gimenes.alex.rickandmorty.core.domain.usecase.GenerateGuessCharacterRoundUseCase
import com.gimenes.alex.rickandmorty.core.domain.usecase.GetCharacterPoolUseCase
import com.gimenes.alex.rickandmorty.core.domain.usecase.RecordGuessOutcomeUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Drives the full Guess the Character flow (issue #16) as a single state machine - see
 * [GuessCharacterUiState]'s kdoc for why this is one ViewModel-owned sealed state rather than a
 * nested nav graph, mirroring [com.gimenes.alex.rickandmorty.feature.quiz.QuizViewModel] (issue
 * #15).
 *
 * ### Pacing (deliberately different from Trivia)
 * Trivia is untimed and always waits for an explicit "Next" tap. Guess the Character is meant to
 * feel fast/arcade-like per LUMA's spec:
 * - **Correct**: a brief feedback flash ([CORRECT_FEEDBACK_DELAY_MS]) then the round advances
 *   automatically - no tap required. [selectAnswer] owns this timing via a plain [delay] in a
 *   `viewModelScope` coroutine, which keeps it both deterministic and trivially testable (a test
 *   can advance a [kotlinx.coroutines.test.TestCoroutineScheduler] rather than needing to fake a
 *   UI-layer `LaunchedEffect`/timer).
 * - **Incorrect**: the streak breaks (persisted via [recordGuessOutcomeUseCase]) and the correct
 *   answer is revealed for a longer, deliberate beat ([INCORRECT_FEEDBACK_DELAY_MS]) before
 *   auto-transitioning to [GuessCharacterUiState.RunEnded] - no forced tap either. A short
 *   auto-transition (rather than requiring an explicit "See Results" tap) was chosen to match the
 *   arcade pacing LUMA's spec calls for; the miss reveal itself is not rushed (see the delay
 *   constant), only the hand-off into Run Ended.
 *
 * ### Reduced motion
 * This ViewModel's delays are the actual pacing/timing logic, not a visual animation - they still
 * run under a reduced-motion preference exactly as described above (the pause itself is part of the
 * game feel, not just an animation flourish). What *should* additionally shorten/skip under
 * reduced-motion is the round-to-round transition *animation* in [GuessCharacterScreen] - Compose
 * Multiplatform has no uniform cross-platform reduced-motion signal today (unlike Android's
 * `Settings.Global.ANIMATOR_DURATION_SCALE`), so that piece is left as a documented gap rather than
 * faked; see the `TODO` on [GuessCharacterScreen]'s round transition.
 */
class GuessCharacterViewModel(
    private val getCharacterPoolUseCase: GetCharacterPoolUseCase,
    private val generateGuessCharacterRoundUseCase: GenerateGuessCharacterRoundUseCase,
    private val recordGuessOutcomeUseCase: RecordGuessOutcomeUseCase,
    private val gameStateRepository: GameStateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GuessCharacterUiState>(GuessCharacterUiState.Loading)
    val uiState: StateFlow<GuessCharacterUiState> = _uiState.asStateFlow()

    /** In-flight run state, `null` outside of an active [GuessCharacterUiState.Round] loop. */
    private var session: GuessCharacterSession? = null

    init {
        startSession()
    }

    /** "Retry" from [GuessCharacterUiState.NetworkError] / [GuessCharacterUiState.Empty]. */
    fun retry() {
        startSession()
    }

    /** "Play Again" from [GuessCharacterUiState.RunEnded] - fresh pool, fresh round, streak = 0. */
    fun playAgain() {
        startSession()
    }

    /**
     * Locks in [optionIndex] as the guess for the current round. No-op if already locked or if not
     * currently showing a round - guards against a double-tap racing the lock, matching
     * [com.gimenes.alex.rickandmorty.feature.quiz.QuizViewModel.selectAnswer].
     */
    fun selectAnswer(optionIndex: Int) {
        val current = _uiState.value as? GuessCharacterUiState.Round ?: return
        if (current.isLocked) return
        val activeSession = session ?: return

        val isCorrect = optionIndex == current.correctOptionIndex
        _uiState.value = current.copy(selectedOptionIndex = optionIndex, isLocked = true)

        viewModelScope.launch {
            recordGuessOutcomeUseCase(isCorrect)

            if (isCorrect) {
                delay(CORRECT_FEEDBACK_DELAY_MS)
                advanceToNextRound(activeSession, newStreak = current.streak + 1)
            } else {
                delay(INCORRECT_FEEDBACK_DELAY_MS)
                // The streak going into the round that was just missed is this run's peak - the
                // persisted current streak has already reset to 0 via recordGuessOutcomeUseCase
                // above, but that's not what Run Ended should headline (see RunEnded's kdoc).
                finishRun(peakStreak = current.streak, sessionAtEnd = activeSession)
            }
        }
    }

    /**
     * Ends the run from a manual mid-streak exit (the confirmation itself is screen-local UI state -
     * see [GuessCharacterScreen]'s kdoc). No-op outside of an active round. Nothing is discarded:
     * every prior correct/incorrect guess this run was already persisted as it happened, so this
     * only needs to compute what Run Ended should display, exactly like the miss path above except
     * the live streak isn't reset (a manual exit is not an incorrect guess).
     */
    fun endRunManually() {
        val current = _uiState.value as? GuessCharacterUiState.Round ?: return
        val activeSession = session ?: return

        viewModelScope.launch {
            finishRun(peakStreak = current.streak, sessionAtEnd = activeSession)
        }
    }

    private fun startSession() {
        session = null
        _uiState.value = GuessCharacterUiState.Loading

        viewModelScope.launch {
            val pool = getCharacterPoolUseCase().first()

            if (pool.isEmpty()) {
                _uiState.value = GuessCharacterUiState.NetworkError
                return@launch
            }

            val round = generateRoundWithRetry(pool)
            if (round == null) {
                _uiState.value = GuessCharacterUiState.Empty
                return@launch
            }

            val bestStreakAtStart = gameStateRepository.getStreak().first()?.best ?: 0
            session = GuessCharacterSession(pool = pool, bestStreakAtStart = bestStreakAtStart)
            _uiState.value = round.toRoundUiState(streak = 0)
        }
    }

    private suspend fun advanceToNextRound(activeSession: GuessCharacterSession, newStreak: Int) {
        val round = generateRoundWithRetry(activeSession.pool)
        if (round == null) {
            // The pool that produced every prior round in this run should essentially never stop
            // being able to produce one (it doesn't shrink mid-run) - this is a defensive fallback,
            // not an expected path.
            session = null
            _uiState.value = GuessCharacterUiState.Empty
            return
        }
        _uiState.value = round.toRoundUiState(streak = newStreak)
    }

    private suspend fun finishRun(peakStreak: Int, sessionAtEnd: GuessCharacterSession) {
        session = null
        val bestStreakEver = maxOf(gameStateRepository.getStreak().first()?.best ?: 0, peakStreak)
        _uiState.value = GuessCharacterUiState.RunEnded(
            finalStreak = peakStreak,
            bestStreakEver = bestStreakEver,
            isNewBest = peakStreak > sessionAtEnd.bestStreakAtStart,
        )
    }

    /**
     * Builds one round from [pool], retrying up to [MAX_GENERATION_ATTEMPTS] times before giving
     * up - same defensive pattern as
     * [com.gimenes.alex.rickandmorty.feature.quiz.QuizViewModel.generateQuestionWithRetry].
     *
     * Unlike [com.gimenes.alex.rickandmorty.core.domain.usecase.GenerateTriviaQuestionUseCase],
     * [GenerateGuessCharacterRoundUseCase]'s current failure condition (pool.size < 4) is
     * structural, not luck-of-the-random-draw - so retrying the exact same too-small pool will never
     * succeed on a later attempt. The retry is kept anyway, both to mirror #15's established
     * resilience pattern and as a cheap safety net if that use case's decoy-selection logic ever
     * grows a randomness-dependent failure mode in the future; a genuinely too-small pool still
     * fails fast regardless of the attempt count.
     */
    private fun generateRoundWithRetry(pool: List<Character>): GuessCharacterRound? {
        repeat(MAX_GENERATION_ATTEMPTS) {
            generateGuessCharacterRoundUseCase(pool)?.let { return it }
        }
        return null
    }

    private fun GuessCharacterRound.toRoundUiState(streak: Int): GuessCharacterUiState.Round =
        GuessCharacterUiState.Round(
            streak = streak,
            target = target,
            options = options,
            correctOptionIndex = correctOptionIndex,
            selectedOptionIndex = null,
            isLocked = false,
        )

    companion object {
        const val MAX_GENERATION_ATTEMPTS = 5
        const val CORRECT_FEEDBACK_DELAY_MS = 700L
        const val INCORRECT_FEEDBACK_DELAY_MS = 1300L
    }
}

/** In-flight Guess the Character run: the fixed pool for this run plus the best streak at start. */
private data class GuessCharacterSession(
    val pool: List<Character>,
    val bestStreakAtStart: Int,
)

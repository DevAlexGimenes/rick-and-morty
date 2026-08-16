package com.gimenes.alex.rickandmorty

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gimenes.alex.rickandmorty.core.designsystem.motion.rememberReducedMotionEnabled
import com.gimenes.alex.rickandmorty.core.designsystem.theme.RickAndMortyTheme
import com.gimenes.alex.rickandmorty.feature.characters.CharactersScreen
import com.gimenes.alex.rickandmorty.feature.guesscharacter.GuessCharacterScreen
import com.gimenes.alex.rickandmorty.feature.quiz.QuizScreen
import com.gimenes.alex.rickandmorty.home.HomeScreen
import com.gimenes.alex.rickandmorty.navigation.Routes

/** Duration of the Home <-> Quiz / Home <-> GuessCharacter screen transition, per LUMA's spec. */
private const val SCREEN_TRANSITION_DURATION_MS = 250

/** Vertical slide distance of the Home <-> Quiz / Home <-> GuessCharacter transition, per LUMA's spec. */
private val SCREEN_TRANSITION_SLIDE_DISTANCE = 8.dp

/**
 * Root composable. Koin's Compose context is set up automatically by startKoin() (called from
 * [com.gimenes.alex.rickandmorty.di.initKoin] before this is composed) - no KoinContext wrapper
 * needed as of Koin 4.1+.
 */
@Composable
fun App() {
    RickAndMortyTheme {
        val navController = rememberNavController()

        // `by` here matters: `reducedMotion` becomes a delegated property backed by this [State],
        // so every read of it - including from inside the non-@Composable enterTransition/
        // exitTransition closures below, which `navigation-compose` invokes at navigation time,
        // not at composition time - re-reads the platform's *current* value rather than closing
        // over a value frozen when the NavHost graph was built. See
        // [rememberReducedMotionEnabled]'s kdoc.
        val reducedMotion by rememberReducedMotionEnabled()
        val slideOffsetPx = with(LocalDensity.current) { SCREEN_TRANSITION_SLIDE_DISTANCE.roundToPx() }

        NavHost(navController = navController, startDestination = Routes.Home) {
            composable<Routes.Home>(
                exitTransition = { fadeSlideExit(reducedMotion, -slideOffsetPx) },
                popEnterTransition = { fadeSlideEnter(reducedMotion, -slideOffsetPx) },
            ) {
                HomeScreen(
                    onQuizClick = { navController.navigate(Routes.Quiz) },
                    onGuessCharacterClick = { navController.navigate(Routes.GuessCharacter) }
                )
            }
            composable<Routes.Quiz>(
                enterTransition = { fadeSlideEnter(reducedMotion, slideOffsetPx) },
                popExitTransition = { fadeSlideExit(reducedMotion, slideOffsetPx) },
            ) {
                QuizScreen(
                    onExit = {
                        navController.popBackStack(Routes.Home, inclusive = false)
                    }
                )
            }
            composable<Routes.GuessCharacter>(
                enterTransition = { fadeSlideEnter(reducedMotion, slideOffsetPx) },
                popExitTransition = { fadeSlideExit(reducedMotion, slideOffsetPx) },
            ) {
                GuessCharacterScreen(
                    onExit = {
                        navController.popBackStack(Routes.Home, inclusive = false)
                    }
                )
            }
            composable<Routes.Characters> {
                CharactersScreen()
            }
        }
    }
}

/**
 * Home -> Quiz / Home -> GuessCharacter (and their reverse, pop back to Home): a crossfade + 8dp
 * vertical slide, ~250ms, per LUMA's motion spec. [offsetPx] carries both magnitude and direction
 * (positive slides up into place from below, negative slides down into place from above), letting
 * one function serve both the forward-navigation "slide up" and the pop "slide down" cases.
 *
 * Returns [EnterTransition.None] under [reducedMotion] - the mandatory static/instant fallback
 * matching this project's established accessibility pattern for every other animation in the
 * codebase (see [rememberReducedMotionEnabled]'s kdoc).
 */
private fun fadeSlideEnter(reducedMotion: Boolean, offsetPx: Int): EnterTransition {
    if (reducedMotion) return EnterTransition.None
    return fadeIn(animationSpec = tween(SCREEN_TRANSITION_DURATION_MS)) +
        slideInVertically(animationSpec = tween(SCREEN_TRANSITION_DURATION_MS)) { offsetPx }
}

/** Exit-side counterpart of [fadeSlideEnter] - see its kdoc. */
private fun fadeSlideExit(reducedMotion: Boolean, offsetPx: Int): ExitTransition {
    if (reducedMotion) return ExitTransition.None
    return fadeOut(animationSpec = tween(SCREEN_TRANSITION_DURATION_MS)) +
        slideOutVertically(animationSpec = tween(SCREEN_TRANSITION_DURATION_MS)) { offsetPx }
}

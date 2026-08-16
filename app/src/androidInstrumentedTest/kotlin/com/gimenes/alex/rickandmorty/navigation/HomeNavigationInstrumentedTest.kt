package com.gimenes.alex.rickandmorty.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gimenes.alex.rickandmorty.home.HomeScreen
import org.junit.Rule
import org.junit.Test

/**
 * Covers issue #17's "basic navigation test" acceptance criterion for the graph wired in
 * `App.kt`: Home's two [com.gimenes.alex.rickandmorty.core.designsystem.component.ModeCard]s
 * navigate to [Routes.Quiz] / [Routes.GuessCharacter], and each destination's exit path pops the
 * back stack to [Routes.Home] (`navController.popBackStack(Routes.Home, inclusive = false)`,
 * exactly as `App.kt` wires it).
 *
 * Updated for issue #40's type-safe routes migration (`Routes` is now a sealed interface of
 * `@Serializable data object`s, navigated with `composable<T>` / `navController.navigate(T)`
 * rather than string route constants) - the navigation behavior under test is unchanged, only the
 * route *type* is. Per-destination transitions (also added in #40) aren't exercised here; that's
 * a visual concern, not a navigation-graph correctness one, and this test deliberately stays a
 * fast, non-visual graph test - see below.
 *
 * This deliberately does **not** compose the real `QuizScreen`/`GuessCharacterScreen`: both pull
 * a `koinViewModel()` backed by live network/database Koin modules (see feature:quiz's
 * `QuizModule` / feature:guesscharacter's `GuessCharacterModule`), which would make this an
 * unpredictable, network-dependent test rather than the basic navigation-graph test this issue
 * asks for - and each feature module already owns testing its own internal screen behavior.
 * Instead, this builds a [NavHost] with the exact same [Routes] types and the exact same exit
 * wiring as `App.kt`, standing in a minimal fake composable at the Quiz/GuessCharacter
 * destinations (a label to identify the destination + a button that invokes the same
 * `popBackStack` exit callback `App.kt` wires to each screen's real `onExit`). That's enough to
 * prove the navigation graph itself - route wiring, forward navigation from Home, and the
 * back-stack-popping exit behavior - is wired correctly.
 */
class HomeNavigationInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Composable
    private fun TestNavGraph() {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = Routes.Home) {
            composable<Routes.Home> {
                HomeScreen(
                    onQuizClick = { navController.navigate(Routes.Quiz) },
                    onGuessCharacterClick = { navController.navigate(Routes.GuessCharacter) },
                )
            }
            composable<Routes.Quiz> {
                FakeExitableDestination(
                    label = "Quiz Destination",
                    onExit = { navController.popBackStack(Routes.Home, inclusive = false) },
                )
            }
            composable<Routes.GuessCharacter> {
                FakeExitableDestination(
                    label = "Guess Character Destination",
                    onExit = { navController.popBackStack(Routes.Home, inclusive = false) },
                )
            }
        }
    }

    @Test
    fun tappingTriviaQuizCard_navigatesToQuizDestination() {
        composeTestRule.setContent { TestNavGraph() }

        composeTestRule.onNodeWithText("Trivia Quiz").performClick()

        composeTestRule.onNodeWithText("Quiz Destination").assertExists()
    }

    @Test
    fun tappingGuessCharacterCard_navigatesToGuessCharacterDestination() {
        composeTestRule.setContent { TestNavGraph() }

        composeTestRule.onNodeWithText("Guess the Character").performClick()

        composeTestRule.onNodeWithText("Guess Character Destination").assertExists()
    }

    @Test
    fun exitingQuiz_returnsToHome() {
        composeTestRule.setContent { TestNavGraph() }

        composeTestRule.onNodeWithText("Trivia Quiz").performClick()
        composeTestRule.onNodeWithText("Exit").performClick()

        // "Pick a game mode" was R5's (issue #41) pre-hero-redesign subtitle and no longer exists;
        // "Rick and Morty" is the hero headline's plain line, present exactly once on Home both
        // before and after the redesign, so it remains a reliable "we're back on Home" marker.
        composeTestRule.onNodeWithText("Rick and Morty").assertExists()
    }

    @Test
    fun exitingGuessCharacter_returnsToHome() {
        composeTestRule.setContent { TestNavGraph() }

        composeTestRule.onNodeWithText("Guess the Character").performClick()
        composeTestRule.onNodeWithText("Exit").performClick()

        composeTestRule.onNodeWithText("Rick and Morty").assertExists()
    }
}

@Composable
private fun FakeExitableDestination(label: String, onExit: () -> Unit) {
    Column {
        Text(label)
        Button(onClick = onExit) { Text("Exit") }
    }
}

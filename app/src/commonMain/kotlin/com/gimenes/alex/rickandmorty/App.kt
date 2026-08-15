package com.gimenes.alex.rickandmorty

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gimenes.alex.rickandmorty.core.designsystem.theme.RickAndMortyTheme
import com.gimenes.alex.rickandmorty.feature.characters.CharactersScreen
import com.gimenes.alex.rickandmorty.feature.guesscharacter.GuessCharacterScreen
import com.gimenes.alex.rickandmorty.feature.quiz.QuizScreen
import com.gimenes.alex.rickandmorty.home.HomeScreen
import com.gimenes.alex.rickandmorty.navigation.Routes
import org.koin.compose.KoinContext

@Composable
fun App() {
    RickAndMortyTheme {
        KoinContext {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = Routes.HOME) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onQuizClick = { navController.navigate(Routes.QUIZ) },
                        onGuessCharacterClick = { navController.navigate(Routes.GUESS_CHARACTER) }
                    )
                }
                composable(Routes.QUIZ) {
                    QuizScreen()
                }
                composable(Routes.GUESS_CHARACTER) {
                    GuessCharacterScreen()
                }
                composable(Routes.CHARACTERS) {
                    CharactersScreen()
                }
            }
        }
    }
}

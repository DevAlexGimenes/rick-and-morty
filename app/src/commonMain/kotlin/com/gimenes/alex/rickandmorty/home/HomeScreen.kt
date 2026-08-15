package com.gimenes.alex.rickandmorty.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gimenes.alex.rickandmorty.core.designsystem.component.ModeCard
import com.gimenes.alex.rickandmorty.core.designsystem.theme.RickAndMortyExtendedTheme

/**
 * Home: two tappable mode cards (Trivia Quiz, Guess the Character) per LUMA's spec. No
 * functionality behind them yet in this pass beyond navigating to the placeholder screens.
 */
@Composable
fun HomeScreen(
    onQuizClick: () -> Unit,
    onGuessCharacterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(RickAndMortyExtendedTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(RickAndMortyExtendedTheme.spacing.md)
        ) {
            Text(
                text = "Rick and Morty",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Pick a game mode",
                style = MaterialTheme.typography.bodyLarge
            )
            ModeCard(
                title = "Trivia Quiz",
                description = "Answer questions about the multiverse.",
                onClick = onQuizClick
            )
            ModeCard(
                title = "Guess the Character",
                description = "Identify characters from the show.",
                onClick = onGuessCharacterClick
            )
        }
    }
}

package com.gimenes.alex.rickandmorty.feature.guesscharacter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

/**
 * Placeholder screen for the Guess the Character mode. This pass wires navigation and the
 * module boundary only - the actual guessing game is a follow-up implementation pass.
 */
@Composable
fun GuessCharacterScreen(
    modifier: Modifier = Modifier,
    viewModel: GuessCharacterViewModel = koinViewModel()
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Guess the Character - coming soon",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

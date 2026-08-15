package com.gimenes.alex.rickandmorty.feature.quiz

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

/**
 * Placeholder screen for the Trivia Quiz mode. This pass wires navigation and the module
 * boundary only - question flow, scoring and streak UI are a follow-up implementation pass.
 */
@Composable
fun QuizScreen(
    modifier: Modifier = Modifier,
    viewModel: QuizViewModel = koinViewModel()
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Trivia Quiz - coming soon",
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

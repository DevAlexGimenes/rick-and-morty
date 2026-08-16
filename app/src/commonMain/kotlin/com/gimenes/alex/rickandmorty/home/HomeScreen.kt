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
import androidx.compose.ui.unit.dp
import com.gimenes.alex.rickandmorty.core.designsystem.component.CharacterSilhouetteGlyph
import com.gimenes.alex.rickandmorty.core.designsystem.component.ModeCard
import com.gimenes.alex.rickandmorty.core.designsystem.component.PortalRingGlyph
import com.gimenes.alex.rickandmorty.core.designsystem.component.QuestionMarkGlyph
import com.gimenes.alex.rickandmorty.core.designsystem.theme.RickAndMortyExtendedTheme

/** Header glyph diameter for [ModeCard]'s compact header row per LUMA's R3 spec (~40-48dp). */
private val MODE_CARD_GLYPH_SIZE = 44.dp

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
                onClick = onQuizClick,
                icon = {
                    PortalRingGlyph(
                        size = MODE_CARD_GLYPH_SIZE,
                        content = { QuestionMarkGlyph() },
                    )
                }
            )
            ModeCard(
                title = "Guess the Character",
                description = "Identify characters from the show.",
                onClick = onGuessCharacterClick,
                icon = {
                    PortalRingGlyph(
                        size = MODE_CARD_GLYPH_SIZE,
                        content = { CharacterSilhouetteGlyph() },
                    )
                }
            )
        }
    }
}

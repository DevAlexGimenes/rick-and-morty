package com.gimenes.alex.rickandmorty.feature.guesscharacter

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GuessCharacterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GuessCharacterUiState())
    val uiState: StateFlow<GuessCharacterUiState> = _uiState.asStateFlow()
}

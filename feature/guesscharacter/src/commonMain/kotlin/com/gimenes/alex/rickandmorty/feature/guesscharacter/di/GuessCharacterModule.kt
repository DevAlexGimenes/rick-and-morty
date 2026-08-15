package com.gimenes.alex.rickandmorty.feature.guesscharacter.di

import com.gimenes.alex.rickandmorty.feature.guesscharacter.GuessCharacterViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val guessCharacterModule = module {
    viewModel { GuessCharacterViewModel() }
}

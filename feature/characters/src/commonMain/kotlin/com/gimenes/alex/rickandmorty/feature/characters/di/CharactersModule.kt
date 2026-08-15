package com.gimenes.alex.rickandmorty.feature.characters.di

import com.gimenes.alex.rickandmorty.feature.characters.CharactersViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val charactersModule = module {
    viewModel { CharactersViewModel() }
}

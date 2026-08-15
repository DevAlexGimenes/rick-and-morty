package com.gimenes.alex.rickandmorty.di

import com.gimenes.alex.rickandmorty.core.network.di.networkModule
import com.gimenes.alex.rickandmorty.feature.characters.di.charactersModule
import com.gimenes.alex.rickandmorty.feature.guesscharacter.di.guessCharacterModule
import com.gimenes.alex.rickandmorty.feature.quiz.di.quizModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module

/** Modules shared by every platform. Platform entry points may append platform-specific ones. */
val sharedAppModules: List<Module> = listOf(
    networkModule,
    charactersModule,
    quizModule,
    guessCharacterModule
)

private var koinStarted = false

/** Idempotent Koin bootstrap, safe to call from both the Android and iOS entry points. */
fun initKoin(platformModules: List<Module> = emptyList()) {
    if (koinStarted) return
    koinStarted = true
    startKoin {
        modules(sharedAppModules + platformModules)
    }
}

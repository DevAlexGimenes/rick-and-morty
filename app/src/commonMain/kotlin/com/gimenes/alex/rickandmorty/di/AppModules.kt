package com.gimenes.alex.rickandmorty.di

import com.gimenes.alex.rickandmorty.core.database.di.databaseModule
import com.gimenes.alex.rickandmorty.core.network.di.networkModule
import com.gimenes.alex.rickandmorty.feature.characters.di.charactersModule
import com.gimenes.alex.rickandmorty.feature.guesscharacter.di.guessCharacterModule
import com.gimenes.alex.rickandmorty.feature.quiz.di.quizModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/** Modules shared by every platform. Platform entry points may append platform-specific ones. */
val sharedAppModules: List<Module> = listOf(
    networkModule,
    databaseModule,
    charactersModule,
    quizModule,
    guessCharacterModule
)

private var koinStarted = false

/**
 * Idempotent Koin bootstrap, safe to call from both the Android and iOS entry points.
 *
 * [appDeclaration] lets each platform entry point configure the underlying [org.koin.core.KoinApplication]
 * before modules are registered - Android uses it to supply `androidContext(this)`, which
 * [databaseModule] needs to open the Room database file; iOS doesn't need it.
 */
fun initKoin(
    platformModules: List<Module> = emptyList(),
    appDeclaration: KoinAppDeclaration = {}
) {
    if (koinStarted) return
    koinStarted = true
    startKoin {
        appDeclaration()
        modules(sharedAppModules + platformModules)
    }
}

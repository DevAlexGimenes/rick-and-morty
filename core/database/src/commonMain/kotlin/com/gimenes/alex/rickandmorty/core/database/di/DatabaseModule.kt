package com.gimenes.alex.rickandmorty.core.database.di

import com.gimenes.alex.rickandmorty.core.database.RickAndMortyDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Provides the platform-built [RickAndMortyDatabase] singleton. Android needs the app
 * [android.content.Context] to open the database file, iOS doesn't - so the actual builder
 * call is platform-specific (see the `.android.kt` / `.ios.kt` actuals), following the same
 * expect/actual split already used for [com.gimenes.alex.rickandmorty.core.database.RickAndMortyDatabaseConstructor].
 */
expect val platformDatabaseModule: Module

/**
 * DI wiring for this module. The shared database instance plus per-DAO bindings are provided
 * here - [characterDao][com.gimenes.alex.rickandmorty.core.database.CharacterDao] is the first
 * of these (issue #5); the game-state DAO (issue #6) will be added alongside it.
 */
val databaseModule: Module = module {
    includes(platformDatabaseModule)
    single { get<RickAndMortyDatabase>().characterDao() }
}

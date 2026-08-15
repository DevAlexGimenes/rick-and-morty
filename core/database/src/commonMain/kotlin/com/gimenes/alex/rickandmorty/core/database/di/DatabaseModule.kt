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
 * DI wiring for this module. Only the shared database instance is bound for now (issue #4 is
 * infrastructure-only) - per-DAO bindings (e.g. a real character cache/game-state DAO) will be
 * added alongside their `@Dao` interfaces in issues #5/#6. [spikeItemDao] is bound here purely
 * because it backs the current placeholder entity used to satisfy Room's non-empty entities
 * requirement (see [RickAndMortyDatabase]).
 */
val databaseModule: Module = module {
    includes(platformDatabaseModule)
    single { get<RickAndMortyDatabase>().spikeItemDao() }
}

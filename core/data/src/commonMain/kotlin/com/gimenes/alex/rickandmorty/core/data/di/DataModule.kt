package com.gimenes.alex.rickandmorty.core.data.di

import com.gimenes.alex.rickandmorty.core.data.repository.CharacterRepositoryImpl
import com.gimenes.alex.rickandmorty.core.data.repository.GameStateRepositoryImpl
import com.gimenes.alex.rickandmorty.core.domain.repository.CharacterRepository
import com.gimenes.alex.rickandmorty.core.domain.repository.GameStateRepository
import org.koin.dsl.module

/**
 * DI wiring for `core:data` (issue #8, extended by issue #9): binds the domain-facing
 * [CharacterRepository]/[GameStateRepository] to their [CharacterRepositoryImpl]/
 * [GameStateRepositoryImpl] implementations, which Koin assembles from the
 * [com.gimenes.alex.rickandmorty.core.network.di.networkModule] and
 * [com.gimenes.alex.rickandmorty.core.database.di.databaseModule] singletons - both of those
 * modules must be included alongside this one (see `app`'s `sharedAppModules`). Note
 * [GameStateRepositoryImpl] only actually needs the database module (it has no network
 * dependency), but wiring both modules alongside `dataModule` is already required for
 * [CharacterRepositoryImpl], so there's nothing extra for callers of this module to do.
 */
val dataModule = module {
    single<CharacterRepository> { CharacterRepositoryImpl(get(), get()) }
    single<GameStateRepository> { GameStateRepositoryImpl(get(), get()) }
}

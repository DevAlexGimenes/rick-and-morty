package com.gimenes.alex.rickandmorty.core.data.di

import com.gimenes.alex.rickandmorty.core.data.repository.CharacterRepositoryImpl
import com.gimenes.alex.rickandmorty.core.domain.repository.CharacterRepository
import org.koin.dsl.module

/**
 * DI wiring for `core:data` (issue #8): binds the domain-facing [CharacterRepository] to its
 * [CharacterRepositoryImpl], which Koin assembles from the [com.gimenes.alex.rickandmorty.core.network.di.networkModule]
 * and [com.gimenes.alex.rickandmorty.core.database.di.databaseModule] singletons - both of those
 * modules must be included alongside this one (see `app`'s `sharedAppModules`).
 */
val dataModule = module {
    single<CharacterRepository> { CharacterRepositoryImpl(get(), get()) }
}

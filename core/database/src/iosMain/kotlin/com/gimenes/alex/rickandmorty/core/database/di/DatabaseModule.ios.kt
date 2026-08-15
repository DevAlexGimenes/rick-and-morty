package com.gimenes.alex.rickandmorty.core.database.di

import com.gimenes.alex.rickandmorty.core.database.RickAndMortyDatabase
import com.gimenes.alex.rickandmorty.core.database.getRickAndMortyDatabaseBuilder
import com.gimenes.alex.rickandmorty.core.database.withDefaults
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDatabaseModule: Module = module {
    single<RickAndMortyDatabase> {
        getRickAndMortyDatabaseBuilder().withDefaults()
    }
}

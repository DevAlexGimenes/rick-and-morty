package com.gimenes.alex.rickandmorty.core.network.di

import com.gimenes.alex.rickandmorty.core.network.api.CharacterApiService
import com.gimenes.alex.rickandmorty.core.network.createHttpClient
import org.koin.dsl.module

val networkModule = module {
    single { createHttpClient() }
    single { CharacterApiService(get()) }
}

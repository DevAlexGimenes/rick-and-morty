package com.gimenes.alex.rickandmorty

import android.app.Application
import com.gimenes.alex.rickandmorty.di.initKoin
import org.koin.android.ext.koin.androidContext

class RickAndMortyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@RickAndMortyApplication)
        }
    }
}

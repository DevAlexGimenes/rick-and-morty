package com.gimenes.alex.rickandmorty

import android.app.Application
import com.gimenes.alex.rickandmorty.di.initKoin

class RickAndMortyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}

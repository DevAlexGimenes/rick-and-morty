package com.gimenes.alex.rickandmorty.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getRickAndMortyDatabaseBuilder(context: Context): RoomDatabase.Builder<RickAndMortyDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath(RICK_AND_MORTY_DB_FILE_NAME)
    return Room.databaseBuilder<RickAndMortyDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}

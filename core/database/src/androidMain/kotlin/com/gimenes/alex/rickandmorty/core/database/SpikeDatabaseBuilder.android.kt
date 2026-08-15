package com.gimenes.alex.rickandmorty.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getSpikeDatabaseBuilder(context: Context): RoomDatabase.Builder<SpikeDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath(SPIKE_DB_FILE_NAME)
    return Room.databaseBuilder<SpikeDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}

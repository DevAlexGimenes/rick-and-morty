package com.gimenes.alex.rickandmorty.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun getRickAndMortyDatabaseBuilder(): RoomDatabase.Builder<RickAndMortyDatabase> {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    val dbFilePath = requireNotNull(documentDirectory?.path) + "/$RICK_AND_MORTY_DB_FILE_NAME"
    return Room.databaseBuilder<RickAndMortyDatabase>(name = dbFilePath)
}

package com.gimenes.alex.rickandmorty.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Trivial entity used to validate that Room 2.7+ Kotlin Multiplatform, with the Bundled
 * SQLite driver and KSP code generation, works cleanly with Kotlin 2.2.10 / AGP 9.2.1
 * before the real schema is built out. See [SpikeDatabase] and [SpikeItemDao].
 */
@Entity(tableName = "spike_item")
data class SpikeItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)

package com.gimenes.alex.rickandmorty.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpikeItemDao {
    @Insert
    suspend fun insert(item: SpikeItemEntity): Long

    @Query("SELECT * FROM spike_item ORDER BY id ASC")
    fun observeAll(): Flow<List<SpikeItemEntity>>

    @Query("SELECT * FROM spike_item ORDER BY id ASC")
    suspend fun getAll(): List<SpikeItemEntity>
}

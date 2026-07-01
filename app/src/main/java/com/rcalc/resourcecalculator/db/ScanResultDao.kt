package com.rcalc.resourcecalculator.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanResultDao {
    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<ScanResultEntity>>

    @Insert
    suspend fun insert(entity: ScanResultEntity): Long

    @Delete
    suspend fun delete(entity: ScanResultEntity)

    @Query("DELETE FROM scan_results")
    suspend fun deleteAll()
}

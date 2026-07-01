package com.rcalc.resourcecalculator.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_results")
data class ScanResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val fromItemsFormatted: String,
    val totalResourcesFormatted: String,
    val resultImagePath: String?,
    val rawJson: String?
)

package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversions")
data class ConversionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val localImagePath: String?,
    val extractedText: String,
    val format: String,
    val extractionType: String,
    val timestamp: Long = System.currentTimeMillis()
)

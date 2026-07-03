package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversionDao {
    @Query("SELECT * FROM conversions ORDER BY timestamp DESC")
    fun getAllConversions(): Flow<List<ConversionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversion(conversion: ConversionEntity): Long

    @Update
    suspend fun updateConversion(conversion: ConversionEntity)

    @Query("DELETE FROM conversions WHERE id = :id")
    suspend fun deleteConversionById(id: Int)

    @Query("DELETE FROM conversions")
    suspend fun clearAllConversions()
}

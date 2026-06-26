package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeDao {
    @Query("SELECT * FROM anime_watchlist ORDER BY savedAt DESC")
    fun getWatchlist(): Flow<List<AnimeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(anime: AnimeEntity)

    @Query("DELETE FROM anime_watchlist WHERE malId = :malId")
    suspend fun deleteById(malId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM anime_watchlist WHERE malId = :malId)")
    fun isSavedFlow(malId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM anime_watchlist WHERE malId = :malId)")
    suspend fun isSaved(malId: Int): Boolean
}

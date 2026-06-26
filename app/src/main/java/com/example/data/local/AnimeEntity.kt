package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anime_watchlist")
data class AnimeEntity(
    @PrimaryKey val malId: Int,
    val title: String,
    val imageUrl: String,
    val synopsis: String,
    val score: Double,
    val episodes: Int,
    val genres: String,
    val savedAt: Long = System.currentTimeMillis()
)

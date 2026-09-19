package com.openmusic.search.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val source: String,
    val thumbnailUrl: String?,
    val durationSeconds: Long?,
    val streamUrl: String?,
    val downloadUrl: String?,
    val originalUrl: String,
    val mediaType: String,
    val timestamp: Long = System.currentTimeMillis()
)

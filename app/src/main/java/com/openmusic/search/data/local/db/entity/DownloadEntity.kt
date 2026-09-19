package com.openmusic.search.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val title: String,
    val source: String,
    val url: String,
    val filePath: String?,
    val mimeType: String?,
    val status: String, // WAITING, DOWNLOADING, PAUSED, COMPLETED, FAILED
    val progress: Int = 0,
    val totalBytes: Long? = null,
    val downloadedBytes: Long = 0,
    val speedBytesPerSec: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

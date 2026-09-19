package com.openmusic.search.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.openmusic.search.data.local.db.entity.DownloadEntity
import com.openmusic.search.data.local.db.entity.FavoriteEntity
import com.openmusic.search.data.local.db.entity.SearchHistoryEntity

@Database(
    entities = [SearchHistoryEntity::class, FavoriteEntity::class, DownloadEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun downloadDao(): DownloadDao
}

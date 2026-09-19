package com.openmusic.search.di

import android.content.Context
import androidx.room.Room
import com.openmusic.search.data.local.db.AppDatabase
import com.openmusic.search.data.local.db.DownloadDao
import com.openmusic.search.data.local.db.FavoriteDao
import com.openmusic.search.data.local.db.SearchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "openmusic.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSearchHistoryDao(db: AppDatabase): SearchHistoryDao = db.searchHistoryDao()

    @Provides
    fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideDownloadDao(db: AppDatabase): DownloadDao = db.downloadDao()
}

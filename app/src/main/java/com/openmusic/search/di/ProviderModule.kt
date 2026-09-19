package com.openmusic.search.di

import com.openmusic.search.data.provider.InternetArchiveProvider
import com.openmusic.search.data.provider.WikimediaProvider
import com.openmusic.search.data.provider.YoutubeProvider
import com.openmusic.search.domain.provider.SearchProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {

    @Binds
    @IntoSet
    abstract fun bindInternetArchiveProvider(impl: InternetArchiveProvider): SearchProvider

    @Binds
    @IntoSet
    abstract fun bindWikimediaProvider(impl: WikimediaProvider): SearchProvider

    @Binds
    @IntoSet
    abstract fun bindYoutubeProvider(impl: YoutubeProvider): SearchProvider
}

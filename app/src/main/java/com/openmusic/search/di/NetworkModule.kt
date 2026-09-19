package com.openmusic.search.di

import com.openmusic.search.BuildConfig
import com.openmusic.search.data.remote.api.InternetArchiveApi
import com.openmusic.search.data.remote.api.WikimediaApi
import com.openmusic.search.data.remote.api.YoutubeApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideInternetArchiveApi(client: OkHttpClient, moshi: Moshi): InternetArchiveApi =
        Retrofit.Builder()
            .baseUrl("https://archive.org/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(InternetArchiveApi::class.java)

    @Provides
    @Singleton
    fun provideWikimediaApi(client: OkHttpClient, moshi: Moshi): WikimediaApi =
        Retrofit.Builder()
            .baseUrl("https://commons.wikimedia.org/w/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WikimediaApi::class.java)

    @Provides
    @Singleton
    fun provideYoutubeApi(client: OkHttpClient, moshi: Moshi): YoutubeApi =
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(YoutubeApi::class.java)

    @Provides
    @Singleton
    @Named("YOUTUBE_API_KEY")
    fun provideYoutubeApiKey(): String = BuildConfig.YOUTUBE_API_KEY

    @Provides
    @Singleton
    @Named("FREESOUND_API_KEY")
    fun provideFreesoundApiKey(): String = BuildConfig.FREESOUND_API_KEY

    @Provides
    @Singleton
    @Named("JAMENDO_CLIENT_ID")
    fun provideJamendoClientId(): String = BuildConfig.JAMENDO_CLIENT_ID
}

package com.openmusic.search.di

import com.openmusic.search.BuildConfig
import com.openmusic.search.data.remote.api.InternetArchiveApi
import com.openmusic.search.data.remote.api.JamendoApi
import com.openmusic.search.data.remote.api.WikimediaApi
import com.openmusic.search.data.remote.api.YoutubeApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
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
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
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
    fun provideJamendoApi(client: OkHttpClient, moshi: Moshi): JamendoApi =
        Retrofit.Builder()
            .baseUrl("https://api.jamendo.com/v3.0/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(JamendoApi::class.java)

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
    fun provideJamendoClientId(): String =
        BuildConfig.JAMENDO_CLIENT_ID.ifBlank { "63c716e0" }  // Jamendo 官方公开 demo key
}

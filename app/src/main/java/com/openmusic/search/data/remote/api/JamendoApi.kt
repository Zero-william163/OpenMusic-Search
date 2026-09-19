package com.openmusic.search.data.remote.api

import com.openmusic.search.data.remote.dto.JamendoSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface JamendoApi {

    /**
     * Jamendo tracks 搜索 API。
     * 返回的 audio 字段直接是 MP3 直链（CDN），可直接播放/下载。
     * https://jamendo.com/api
     */
    @GET("tracks")
    suspend fun search(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "jsonpretty",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("search") search: String,
        @Query("fuzzy") fuzzy: Boolean = true,
        @Query("adult_content") adultContent: Boolean = false,
        @Query("include") include: String = "musicinfo"
    ): JamendoSearchResponse
}

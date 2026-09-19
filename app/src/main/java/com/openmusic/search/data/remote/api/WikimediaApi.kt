package com.openmusic.search.data.remote.api

import com.openmusic.search.data.remote.dto.WikimediaInfoResponse
import com.openmusic.search.data.remote.dto.WikimediaSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WikimediaApi {

    @GET("api.php")
    suspend fun search(
        @Query("action") action: String = "query",
        @Query("list") list: String = "search",
        @Query("srnamespace") namespace: Int = 6,
        @Query("srsearch") query: String,
        @Query("srlimit") limit: Int = 20,
        @Query("sroffset") offset: Int = 0,
        @Query("format") format: String = "json",
        @Query("formatversion") formatVersion: Int = 2
    ): WikimediaSearchResponse

    @GET("api.php")
    suspend fun fileInfo(
        @Query("action") action: String = "query",
        @Query("titles") titles: String,
        @Query("prop") prop: String = "imageinfo",
        @Query("iiprop") iiProp: String = "url|size|mime|duration",
        @Query("format") format: String = "json",
        @Query("formatversion") formatVersion: Int = 2
    ): WikimediaInfoResponse
}

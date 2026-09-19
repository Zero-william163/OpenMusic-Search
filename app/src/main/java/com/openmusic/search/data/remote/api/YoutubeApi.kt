package com.openmusic.search.data.remote.api

import com.openmusic.search.data.remote.dto.YoutubeSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface YoutubeApi {

    @GET("youtube/v3/search")
    suspend fun search(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("videoCategoryId") categoryId: String = "10", // Music
        @Query("maxResults") maxResults: Int = 20,
        @Query("pageToken") pageToken: String? = null,
        @Query("key") key: String
    ): YoutubeSearchResponse
}

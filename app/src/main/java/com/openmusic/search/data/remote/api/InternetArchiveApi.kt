package com.openmusic.search.data.remote.api

import com.openmusic.search.data.remote.dto.IaMetadataResponse
import com.openmusic.search.data.remote.dto.IaSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface InternetArchiveApi {

    @GET("advancedsearch.php")
    suspend fun search(
        @Query("q") query: String,
        @Query("fl[]", encoded = true) fields: List<String> = listOf(
            "identifier", "title", "creator", "date", "mediatype", "description"
        ),
        @Query("rows") rows: Int = 20,
        @Query("page") page: Int = 1,
        @Query("output") output: String = "json"
    ): IaSearchResponse

    @GET("metadata/{identifier}")
    suspend fun metadata(@Path("identifier") identifier: String): IaMetadataResponse
}

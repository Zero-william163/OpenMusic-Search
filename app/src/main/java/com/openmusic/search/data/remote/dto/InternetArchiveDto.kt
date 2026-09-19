package com.openmusic.search.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class IaSearchResponse(
    val response: IaSearchResponseData? = null
)

@JsonClass(generateAdapter = true)
data class IaSearchResponseData(
    val numFound: Int = 0,
    val docs: List<IaSearchDoc> = emptyList()
)

@JsonClass(generateAdapter = true)
data class IaSearchDoc(
    val identifier: String? = null,
    val title: String? = null,
    val creator: List<String>? = null,
    val date: String? = null,
    val mediatype: String? = null,
    val description: List<String>? = null
)

package com.openmusic.search.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WikimediaSearchResponse(
    val query: WikimediaSearchQuery? = null
)

@JsonClass(generateAdapter = true)
data class WikimediaSearchQuery(
    val search: List<WikimediaSearchDoc> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WikimediaSearchDoc(
    val title: String? = null,
    val snippet: String? = null,
    val wordcount: Int? = null
)

@JsonClass(generateAdapter = true)
data class WikimediaInfoResponse(
    val query: WikimediaInfoQuery? = null
)

@JsonClass(generateAdapter = true)
data class WikimediaInfoQuery(
    val pages: Map<String, WikimediaPage> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class WikimediaPage(
    val title: String? = null,
    val imageinfo: List<WikimediaImageInfo> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WikimediaImageInfo(
    val url: String? = null,
    val mime: String? = null,
    val size: Long? = null,
    val duration: Double? = null,
    val thumburl: String? = null
)

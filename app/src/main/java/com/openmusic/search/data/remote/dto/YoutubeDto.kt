package com.openmusic.search.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YoutubeSearchResponse(
    val items: List<YoutubeItem> = emptyList(),
    val nextPageToken: String? = null
)

@JsonClass(generateAdapter = true)
data class YoutubeItem(
    val id: YoutubeId? = null,
    val snippet: YoutubeSnippet? = null
)

@JsonClass(generateAdapter = true)
data class YoutubeId(
    val videoId: String? = null,
    val kind: String? = null
)

@JsonClass(generateAdapter = true)
data class YoutubeSnippet(
    val title: String? = null,
    val channelTitle: String? = null,
    val publishedAt: String? = null,
    val thumbnails: YoutubeThumbnails? = null
)

@JsonClass(generateAdapter = true)
data class YoutubeThumbnails(
    val medium: YoutubeThumbnail? = null,
    val high: YoutubeThumbnail? = null
)

@JsonClass(generateAdapter = true)
data class YoutubeThumbnail(
    val url: String? = null
)

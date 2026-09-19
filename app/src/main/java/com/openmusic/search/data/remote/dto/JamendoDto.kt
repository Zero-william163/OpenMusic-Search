package com.openmusic.search.data.remote.dto

import com.squareup.moshi.JsonClass

/** Jamendo tracks 搜索响应 */
@JsonClass(generateAdapter = true)
data class JamendoSearchResponse(
    val headers: JamendoHeaders? = null,
    val results: List<JamendoTrack> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JamendoHeaders(
    val status: String? = null,
    val code: Int? = null,
    val count: Int? = null
)

@JsonClass(generateAdapter = true)
data class JamendoTrack(
    val id: String? = null,
    val name: String? = null,
    val duration: Int? = null,        // 秒
    val artist_id: String? = null,
    val artist_name: String? = null,
    val album_name: String? = null,
    val licenseurl: String? = null,
    val audio: String? = null,        // MP3 直链
    val audiodownload: String? = null, // 下载直链
    val audiodownload_allowed: Boolean? = null,
    val image: String? = null,        // 封面
    val image_small: String? = null,
    val rating: Float? = null,
    val genre: String? = null,
    val album_image: String? = null
)

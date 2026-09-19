package com.openmusic.search.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class IaMetadataResponse(
    val metadata: IaMetadata? = null,
    val files: List<IaFile> = emptyList()
)

@JsonClass(generateAdapter = true)
data class IaMetadata(
    val identifier: String? = null,
    val title: String? = null,
    val creator: Any? = null,
    val date: String? = null
)

@JsonClass(generateAdapter = true)
data class IaFile(
    val name: String? = null,
    val format: String? = null,
    val length: String? = null,    // duration in seconds (string)
    val size: String? = null,      // file size in bytes (string)
    val bitrate: String? = null,   // kbps (string)
    val title: String? = null,
    val creator: String? = null
)

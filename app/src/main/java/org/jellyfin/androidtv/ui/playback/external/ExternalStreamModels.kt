package org.jellyfin.androidtv.ui.playback.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ExternalStreamsResponse(
    val strms: List<ExternalStream>
)

@Serializable
data class ExternalStream(
    val title: String = "",
    val provider: String = "",
    val quality: String = "",
    val lang: String = "",
    val ainfo: String = "",
    val vinfo: String = "",
    val url: String = "",
    val size: String = "",
    val id: String = "",
    val sid: String = "",
    val sinfo: Boolean = false,
    val linfo: List<String> = emptyList(),
    val bitrate: String = "",
    val filename: String = "",
    val episode: String? = null,
    @SerialName("stream_info") val streamInfo: StreamInfo? = null,
    val notifications: JsonElement? = null,
    val subs: JsonElement? = null,
    val headers: List<String> = emptyList()
)

@Serializable
data class StreamInfo(
    val video: VideoInfo? = null,
    val audio: AudioInfo? = null,
    val filename: String? = null,
    val langs: Map<String, Int>? = null,
    val streams: JsonElement? = null, // Mixed types: [["lc", 2, "CZ"]]
    @SerialName("HEVC") val hevc: Int? = null,
    val fps: JsonElement? = null, // Can be Int or Float
    val fvideo: String? = null,
    val faudio: String? = null,
    val flags: Int? = null
)

@Serializable
data class VideoInfo(
    val codec: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val aspect: String? = null,
    val ratio: String? = null,
    val duration: Int? = null
)

@Serializable
data class AudioInfo(
    val codec: String? = null,
    val channels: Int? = null
)

@Serializable
data class ResolveResponse(
    val status: String,
    @SerialName("sc_id") val scId: String? = null,
    @SerialName("stream_index") val streamIndex: Int? = null,
    val url: String? = null
)

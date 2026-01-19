package org.jellyfin.androidtv.ui.playback.external

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

class ExternalStreamApi(
    private val client: OkHttpClient,
    private val baseUrl: String = "http://192.168.2.9:3000"
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getStreams(scid: String): Result<ExternalStreamsResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/streams/$scid")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("HTTP error: ${response.code}")
                val body = response.body?.string() ?: throw Exception("Empty response")
                Timber.d("ExternalStreamApi: getStreams response: $body")
                json.decodeFromString<ExternalStreamsResponse>(body)
            }
        }.onFailure { Timber.e(it, "ExternalStreamApi: getStreams failed") }
    }

    suspend fun resolveStream(scid: String, streamIndex: Int): Result<ResolveResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/resolve/$scid?stream=$streamIndex")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("HTTP error: ${response.code}")
                val body = response.body?.string() ?: throw Exception("Empty response")
                Timber.d("ExternalStreamApi: resolveStream response: $body")
                json.decodeFromString<ResolveResponse>(body)
            }
        }.onFailure { Timber.e(it, "ExternalStreamApi: resolveStream failed") }
    }
}

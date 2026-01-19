package org.jellyfin.androidtv.ui.playback.external

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

class ExternalStreamApi(
    // Use 10.0.2.2 for Android emulator (maps to host localhost)
    // Use actual IP (e.g., 192.168.x.x) for real devices
    private val baseUrl: String = "http://192.168.2.9:3000"
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getStreams(scid: String): Result<ExternalStreamsResponse> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/streams/$scid")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Timber.d("ExternalStreamApi: getStreams response: $response")
                val parsed = json.decodeFromString<ExternalStreamsResponse>(response)
                Result.success(parsed)
            } else {
                Result.failure(Exception("HTTP error: $responseCode"))
            }
        } catch (e: Exception) {
            Timber.e(e, "ExternalStreamApi: getStreams failed")
            Result.failure(e)
        }
    }

    suspend fun resolveStream(scid: String, streamIndex: Int): Result<ResolveResponse> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/resolve/$scid?stream=$streamIndex")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Timber.d("ExternalStreamApi: resolveStream response: $response")
                val parsed = json.decodeFromString<ResolveResponse>(response)
                Result.success(parsed)
            } else {
                Result.failure(Exception("HTTP error: $responseCode"))
            }
        } catch (e: Exception) {
            Timber.e(e, "ExternalStreamApi: resolveStream failed")
            Result.failure(e)
        }
    }
}

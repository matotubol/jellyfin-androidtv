package org.jellyfin.androidtv.ui.playback.external

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

sealed interface StreamSelectorState {
    data object Loading : StreamSelectorState
    data class Loaded(
        val streams: List<ExternalStream>,
        val resolvingIndex: Int? = null
    ) : StreamSelectorState
    data class Resolving(val index: Int) : StreamSelectorState
    data class Resolved(val url: String) : StreamSelectorState
    data class Error(val message: String) : StreamSelectorState
}

class ExternalStreamSelectorViewModel(
    private val api: ExternalStreamApi
) : ViewModel() {

    private val _state = MutableStateFlow<StreamSelectorState>(StreamSelectorState.Loading)
    val state: StateFlow<StreamSelectorState> = _state.asStateFlow()

    private var currentScid: String? = null

    fun loadStreams(scid: String) {
        if (currentScid == scid && _state.value is StreamSelectorState.Loaded) {
            return // Already loaded
        }
        currentScid = scid
        _state.value = StreamSelectorState.Loading

        viewModelScope.launch {
            api.getStreams(scid)
                .onSuccess { response ->
                    _state.value = StreamSelectorState.Loaded(response.strms)
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to load streams")
                    _state.value = StreamSelectorState.Error(
                        error.message ?: "Failed to load streams"
                    )
                }
        }
    }

    fun resolveStream(streamIndex: Int) {
        val currentState = _state.value
        if (currentState !is StreamSelectorState.Loaded) return
        if (currentState.resolvingIndex != null) return // Already resolving

        val scid = currentScid ?: return

        _state.value = currentState.copy(resolvingIndex = streamIndex)

        viewModelScope.launch {
            api.resolveStream(scid, streamIndex)
                .onSuccess { response ->
                    val url = response.url
                    if (response.status == "ok" && url != null) {
                        Timber.i("Stream resolved: $url")
                        _state.value = StreamSelectorState.Resolved(url)
                    } else {
                        _state.value = StreamSelectorState.Error("Failed to resolve stream")
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to resolve stream")
                    _state.value = StreamSelectorState.Error(
                        error.message ?: "Failed to resolve stream"
                    )
                }
        }
    }

    fun reset() {
        currentScid = null
        _state.value = StreamSelectorState.Loading
    }
}

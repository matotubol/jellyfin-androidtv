package org.jellyfin.androidtv.ui.playback.external

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.koin.android.ext.android.inject
import timber.log.Timber

class ExternalStreamSelectorFragment : Fragment() {
    companion object {
        const val EXTRA_SCID = "scid"
        const val EXTRA_POSITION = "position"
    }

    private val navigationRepository by inject<NavigationRepository>()
    private val videoQueueManager by inject<VideoQueueManager>()
    private val userPreferences by inject<UserPreferences>()

    private var dialogVisible by mutableStateOf(true)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        JellyfinTheme {
            val scid = arguments?.getString(EXTRA_SCID) ?: ""
            val position = arguments?.getInt(EXTRA_POSITION)

            ExternalStreamSelectorDialog(
                visible = dialogVisible,
                scid = scid,
                onDismissRequest = {
                    dialogVisible = false
                    navigationRepository.goBack()
                },
                onStreamResolved = { url ->
                    Timber.i("Stream resolved with URL: $url")
                    dialogVisible = false

                    // Continue to the actual video player
                    if (userPreferences[UserPreferences.playbackRewriteVideoEnabled]) {
                        navigationRepository.navigate(Destinations.videoPlayerNew(position), replace = true)
                    } else {
                        navigationRepository.navigate(Destinations.videoPlayer(position), replace = true)
                    }
                }
            )
        }
    }
}

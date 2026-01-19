package org.jellyfin.androidtv.ui.playback.external

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalShapes
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.dialog.DialogBase
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.design.Tokens

sealed class StreamSelectorState {
    data object Loading : StreamSelectorState()
    data class Loaded(val streams: List<ExternalStream>) : StreamSelectorState()
    data class Resolving(val streamIndex: Int) : StreamSelectorState()
    data class Error(val message: String) : StreamSelectorState()
}

@Composable
fun ExternalStreamSelectorDialog(
    visible: Boolean,
    scid: String,
    onDismissRequest: () -> Unit,
    onStreamResolved: (url: String) -> Unit,
    api: ExternalStreamApi = remember { ExternalStreamApi() }
) {
    var state by remember { mutableStateOf<StreamSelectorState>(StreamSelectorState.Loading) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(visible, scid) {
        if (visible && scid.isNotEmpty()) {
            state = StreamSelectorState.Loading
            api.getStreams(scid).fold(
                onSuccess = { response ->
                    state = StreamSelectorState.Loaded(response.strms)
                },
                onFailure = { error ->
                    state = StreamSelectorState.Error(error.message ?: "Failed to load streams")
                }
            )
        }
    }

    DialogBase(
        visible = visible,
        onDismissRequest = onDismissRequest,
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            modifier = Modifier
                .padding(Tokens.Space.spaceMd)
                .clip(LocalShapes.current.large)
                .background(JellyfinTheme.colorScheme.surface)
                .width(450.dp)
                .fillMaxHeight()
                .focusRestorer()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Select Stream",
                    style = JellyfinTheme.typography.listHeader.copy(
                        color = JellyfinTheme.colorScheme.listHeader
                    ),
                    modifier = Modifier.padding(Tokens.Space.spaceMd)
                )

                when (val currentState = state) {
                    is StreamSelectorState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Loading streams...",
                                style = JellyfinTheme.typography.listHeadline.copy(
                                    color = JellyfinTheme.colorScheme.listHeadline
                                )
                            )
                        }
                    }

                    is StreamSelectorState.Loaded -> {
                        val firstItemFocusRequester = remember { FocusRequester() }

                        LaunchedEffect(Unit) {
                            firstItemFocusRequester.requestFocus()
                        }

                        LazyColumn(
                            modifier = Modifier
                                .padding(horizontal = Tokens.Space.spaceSm)
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(
                                items = currentState.streams,
                                key = { _: Int, stream: ExternalStream -> stream.sid.ifEmpty { stream.hashCode().toString() } }
                            ) { index: Int, stream: ExternalStream ->
                                StreamItem(
                                    stream = stream,
                                    focusRequester = if (index == 0) firstItemFocusRequester else null,
                                    onClick = {
                                        scope.launch {
                                            state = StreamSelectorState.Resolving(index)
                                            api.resolveStream(scid, index).fold(
                                                onSuccess = { response ->
                                                    if (response.status == "ok" && response.url != null) {
                                                        onStreamResolved(response.url)
                                                    } else {
                                                        state = StreamSelectorState.Error("Failed to resolve stream")
                                                    }
                                                },
                                                onFailure = { error ->
                                                    state = StreamSelectorState.Error(error.message ?: "Failed to resolve stream")
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    is StreamSelectorState.Resolving -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Resolving stream ${currentState.streamIndex}...",
                                style = JellyfinTheme.typography.listHeadline.copy(
                                    color = JellyfinTheme.colorScheme.listHeadline
                                )
                            )
                        }
                    }

                    is StreamSelectorState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentState.message,
                                style = JellyfinTheme.typography.listHeadline.copy(
                                    color = JellyfinTheme.colorScheme.listHeadline
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StreamItem(
    stream: ExternalStream,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    ListButton(
        onClick = onClick,
        modifier = focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
        headingContent = {
            // Languages as simple text (SK, EN, CZ)
            val langs = stream.streamInfo?.langs?.keys?.joinToString("  ") ?: stream.lang
            Text(
                text = langs,
                style = JellyfinTheme.typography.listHeadline.copy(
                    color = JellyfinTheme.colorScheme.listHeadline,
                    fontWeight = FontWeight.SemiBold
                )
            )
        },
        captionContent = {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Technical info row: 1080p • H264 • EAC3 2.0
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quality badge (only colored element)
                    QualityBadge(quality = stream.quality)

                    // Separator
                    Text(
                        text = "•",
                        style = JellyfinTheme.typography.listCaption.copy(
                            color = JellyfinTheme.colorScheme.listCaption.copy(alpha = 0.4f)
                        )
                    )

                    // Video codec
                    stream.streamInfo?.video?.codec?.let { codec ->
                        Text(
                            text = codec.uppercase(),
                            style = JellyfinTheme.typography.listCaption.copy(
                                color = JellyfinTheme.colorScheme.listCaption.copy(alpha = 0.7f)
                            )
                        )
                    }

                    // Audio codec with channels
                    val audioInfo = formatAudioInfo(stream.streamInfo?.audio)
                    if (audioInfo.isNotBlank()) {
                        Text(
                            text = "•",
                            style = JellyfinTheme.typography.listCaption.copy(
                                color = JellyfinTheme.colorScheme.listCaption.copy(alpha = 0.4f)
                            )
                        )
                        Text(
                            text = audioInfo,
                            style = JellyfinTheme.typography.listCaption.copy(
                                color = JellyfinTheme.colorScheme.listCaption.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                // Size and provider row
                Text(
                    text = "${stream.size} • ${stream.provider}",
                    style = JellyfinTheme.typography.listCaption.copy(
                        color = JellyfinTheme.colorScheme.listCaption.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

@Composable
private fun QualityBadge(quality: String) {
    // Muted, subtle colors
    val backgroundColor = when {
        quality.contains("4K", ignoreCase = true) || quality.contains("2160", ignoreCase = true) -> Color(0xFF7D5A50)
        quality.contains("1080", ignoreCase = true) -> Color(0xFF4A6572)
        quality.contains("720", ignoreCase = true) -> Color(0xFF5D6D5A)
        else -> Color(0xFF5A5A5A)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = quality,
            style = JellyfinTheme.typography.listCaption.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun InfoTag(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = JellyfinTheme.typography.listOverline.copy(
                color = color,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp
            )
        )
    }
}

private fun formatAudioInfo(audio: AudioInfo?): String {
    if (audio == null) return ""
    val codec = audio.codec?.uppercase() ?: return ""
    val channels = when (audio.channels) {
        2 -> "2.0"
        6 -> "5.1"
        8 -> "7.1"
        else -> audio.channels?.toString() ?: ""
    }
    return if (channels.isNotEmpty()) "$codec $channels" else codec
}

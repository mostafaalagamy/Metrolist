package com.moxxaxx.innertube.models.response

import com.moxxaxx.innertube.models.NavigationEndpoint
import com.moxxaxx.innertube.models.PlaylistPanelRenderer
import com.moxxaxx.innertube.models.Tabs
import kotlinx.serialization.Serializable

@Serializable
data class NextResponse(
    val contents: Contents,
    val continuationContents: ContinuationContents?,
    val currentVideoEndpoint: NavigationEndpoint?,
) {
    @Serializable
    data class Contents(
        val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer,
    ) {
        @Serializable
        data class SingleColumnMusicWatchNextResultsRenderer(
            val tabbedRenderer: TabbedRenderer,
        ) {
            @Serializable
            data class TabbedRenderer(
                val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer,
            ) {
                @Serializable
                data class WatchNextTabbedResultsRenderer(
                    val tabs: List<Tabs.Tab>,
                )
            }
        }
    }

    @Serializable
    data class ContinuationContents(
        val playlistPanelContinuation: PlaylistPanelRenderer,
    )
}

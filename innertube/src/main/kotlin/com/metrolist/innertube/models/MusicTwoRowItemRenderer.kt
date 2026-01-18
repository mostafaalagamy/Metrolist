package com.metrolist.innertube.models

import com.metrolist.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ALBUM
import com.metrolist.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ARTIST
import com.metrolist.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_AUDIOBOOK
import com.metrolist.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_PLAYLIST
import kotlinx.serialization.Serializable

/**
 * Two row: a big thumbnail, a title, and a subtitle
 * Used in [GridRenderer] and [MusicCarouselShelfRenderer]
 * Item type: song, video, album, playlist, artist
 */
@Serializable
data class MusicTwoRowItemRenderer(
    val title: Runs,
    val subtitle: Runs?,
    val subtitleBadges: List<Badges>?,
    val menu: Menu?,
    val thumbnailRenderer: ThumbnailRenderer,
    val navigationEndpoint: NavigationEndpoint,
    val thumbnailOverlay: MusicResponsiveListItemRenderer.Overlay?,
) {
    val isSong: Boolean
        get() = navigationEndpoint.endpoint is WatchEndpoint
    
    val isPlaylist: Boolean
        get() {
            // Check by pageType first
            val pageType = navigationEndpoint.browseEndpoint
                ?.browseEndpointContextSupportedConfigs
                ?.browseEndpointContextMusicConfig
                ?.pageType
            if (pageType == MUSIC_PAGE_TYPE_PLAYLIST) return true
            
            // Fallback: check by browseId pattern for playlists without pageType
            val browseId = navigationEndpoint.browseEndpoint?.browseId
            if (browseId != null) {
                val id = browseId.removePrefix("VL")
                if (id.startsWith("PL") ||      // User playlists
                    id.startsWith("OL") ||      // Official playlists
                    id.startsWith("RD") ||      // Radio/Mix playlists
                    id.startsWith("UC") ||      // Channel playlists
                    id.startsWith("UU") ||      // Channel uploads
                    id.startsWith("LL") ||      // Liked videos
                    id.startsWith("WL") ||      // Watch later
                    id.startsWith("FL") ||      // Favorites
                    id.startsWith("ML") ||      // Mix playlists
                    id.startsWith("LM")) {      // Liked music
                    return true
                }
            }
            return false
        }
    
    val isAlbum: Boolean
        get() =
            navigationEndpoint.browseEndpoint
                ?.browseEndpointContextSupportedConfigs
                ?.browseEndpointContextMusicConfig
                ?.pageType ==
                MUSIC_PAGE_TYPE_ALBUM ||
                navigationEndpoint.browseEndpoint
                    ?.browseEndpointContextSupportedConfigs
                    ?.browseEndpointContextMusicConfig
                    ?.pageType ==
                MUSIC_PAGE_TYPE_AUDIOBOOK
    val isArtist: Boolean
        get() =
            navigationEndpoint.browseEndpoint
                ?.browseEndpointContextSupportedConfigs
                ?.browseEndpointContextMusicConfig
                ?.pageType ==
                MUSIC_PAGE_TYPE_ARTIST

    val musicVideoType: String?
        get() =
            thumbnailOverlay
                ?.musicItemThumbnailOverlayRenderer
                ?.content
                ?.musicPlayButtonRenderer
                ?.playNavigationEndpoint
                ?.musicVideoType
                ?: navigationEndpoint.musicVideoType
}

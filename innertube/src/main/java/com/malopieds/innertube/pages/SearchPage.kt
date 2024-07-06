package com.malopieds.innertube.pages

import com.malopieds.innertube.models.Album
import com.malopieds.innertube.models.AlbumItem
import com.malopieds.innertube.models.Artist
import com.malopieds.innertube.models.ArtistItem
import com.malopieds.innertube.models.MusicResponsiveListItemRenderer
import com.malopieds.innertube.models.PlaylistItem
import com.malopieds.innertube.models.SongItem
import com.malopieds.innertube.models.YTItem
import com.malopieds.innertube.models.oddElements
import com.malopieds.innertube.models.splitBySeparator
import com.malopieds.innertube.utils.parseTime

data class SearchResult(
    val items: List<YTItem>,
    val continuation: String? = null,
)

object SearchPage {
    fun toYTItem(renderer: MusicResponsiveListItemRenderer): YTItem? {
        val secondaryLine =
            renderer.flexColumns
                .getOrNull(1)
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs
                ?.splitBySeparator()
                ?: return null
        return when {
            renderer.isSong -> {
                SongItem(
                    id = renderer.playlistItemData?.videoId ?: return null,
                    title =
                        renderer.flexColumns
                            .firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: return null,
                    artists =
                        secondaryLine.firstOrNull()?.oddElements()?.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId,
                            )
                        } ?: return null,
                    album =
                        secondaryLine.getOrNull(1)?.firstOrNull()?.takeIf { it.navigationEndpoint?.browseEndpoint != null }?.let {
                            Album(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId!!,
                            )
                        },
                    duration =
                        secondaryLine
                            .lastOrNull()
                            ?.firstOrNull()
                            ?.text
                            ?.parseTime(),
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    explicit =
                        renderer.badges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null,
                )
            }
            renderer.isArtist -> {
                ArtistItem(
                    id = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                    title =
                        renderer.flexColumns
                            .firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.text
                            ?: return null,
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    shuffleEndpoint =
                        renderer.menu
                            ?.menuRenderer
                            ?.items
                            ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint ?: return null,
                    radioEndpoint =
                        renderer.menu.menuRenderer.items
                            .find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint ?: return null,
                )
            }
            renderer.isAlbum -> {
                AlbumItem(
                    browseId = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                    playlistId =
                        renderer.overlay
                            ?.musicItemThumbnailOverlayRenderer
                            ?.content
                            ?.musicPlayButtonRenderer
                            ?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint
                            ?.playlistId
                            ?: return null,
                    title =
                        renderer.flexColumns
                            .firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: return null,
                    artists =
                        secondaryLine.getOrNull(1)?.oddElements()?.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId,
                            )
                        } ?: return null,
                    year =
                        secondaryLine
                            .getOrNull(2)
                            ?.firstOrNull()
                            ?.text
                            ?.toIntOrNull(),
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    explicit =
                        renderer.badges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null,
                )
            }
            renderer.isPlaylist -> {
                PlaylistItem(
                    id =
                        renderer.navigationEndpoint
                            ?.browseEndpoint
                            ?.browseId
                            ?.removePrefix("VL") ?: return null,
                    title =
                        renderer.flexColumns
                            .firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: return null,
                    author =
                        secondaryLine.firstOrNull()?.firstOrNull()?.let {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId,
                            )
                        } ?: return null,
                    songCountText =
                        renderer.flexColumns
                            .getOrNull(1)
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.lastOrNull()
                            ?.text ?: return null,
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    playEndpoint =
                        renderer.overlay
                            ?.musicItemThumbnailOverlayRenderer
                            ?.content
                            ?.musicPlayButtonRenderer
                            ?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint ?: return null,
                    shuffleEndpoint =
                        renderer.menu
                            ?.menuRenderer
                            ?.items
                            ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint ?: return null,
                    radioEndpoint =
                        renderer.menu.menuRenderer.items
                            .find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint ?: return null,
                )
            }
            else -> null
        }
    }
}

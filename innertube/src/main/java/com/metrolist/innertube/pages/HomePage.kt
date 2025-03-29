package com.metrolist.innertube.pages

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.BrowseEndpoint
import com.metrolist.innertube.models.MusicCarouselShelfRenderer
import com.metrolist.innertube.models.MusicResponsiveListItemRenderer
import com.metrolist.innertube.models.MusicTwoRowItemRenderer
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.oddElements

data class HomePage(
    val sections: List<Section>,
    val continuation: String? = null,
) {
    data class Section(
        val title: String,
        val label: String?,
        val thumbnail: String?,
        val endpoint: BrowseEndpoint?,
        val items: List<YTItem>,
        val sectionType: SectionType,
    ) {
        companion object {
            fun fromMusicCarouselShelfRenderer(renderer: MusicCarouselShelfRenderer): Section? {
                return Section(
                    title = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text ?: return null,
                    label = renderer.header.musicCarouselShelfBasicHeaderRenderer.strapline?.runs?.firstOrNull()?.text,
                    thumbnail = renderer.header.musicCarouselShelfBasicHeaderRenderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl(),
                    endpoint = renderer.header.musicCarouselShelfBasicHeaderRenderer.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint,
                    items = renderer.contents.mapNotNull {
                        it.musicTwoRowItemRenderer?.let { renderer ->
                             fromMusicTwoRowItemRenderer(
                                 renderer
                             )
                         } ?: it.musicResponsiveListItemRenderer?.let { renderer ->
                             fromMusicResponsiveListItemRenderer(
                                 renderer
                             )
                         }
                    }.ifEmpty {
                        return null
                    },
                     sectionType = if (renderer.contents.any { it.musicResponsiveListItemRenderer != null }) SectionType.GRID else SectionType.LIST,
                )
            }

            private fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
                return when {
                    renderer.isSong -> {
                        renderer.subtitle?.runs?.oddElements()?.drop(1)?.map {
                             Artist(
                                 name = it.text,
                                 id = it.navigationEndpoint?.browseEndpoint?.browseId
                             )
                         }?.let {
                             SongItem(
                                 id = renderer.navigationEndpoint.watchEndpoint?.videoId ?: return null,
                                 title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                                 artists = it,
                                 album = null,
                                 duration = null,
                                 thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                                 explicit = renderer.subtitleBadges?.find {
                                     it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                                 } != null
                             )
                         }
                    }

                    renderer.isAlbum -> {
                        AlbumItem(
                            browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                            playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint?.playlistId ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            artists = renderer.subtitle?.runs?.oddElements()?.drop(1)?.map {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                                )
                            },
                            year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                            explicit = renderer.subtitleBadges?.find {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } != null
                        )
                    }

                    renderer.isPlaylist -> {
                        // Playlist from YouTube Music
                        PlaylistItem(
                            id = renderer.navigationEndpoint.browseEndpoint?.browseId?.removePrefix("VL") ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            author = Artist(
                                name = renderer.subtitle?.runs?.lastOrNull()?.text ?: return null,
                                id = null
                            ),
                            songCountText = null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                            playEndpoint = renderer.thumbnailOverlay
                                ?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint ?: return null,
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                            radioEndpoint = renderer.menu.menuRenderer.items.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                        )
                    }

                    renderer.isArtist -> {
                        ArtistItem(
                            id = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                            title = renderer.title.runs?.lastOrNull()?.text ?: return null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                            radioEndpoint = renderer.menu.menuRenderer.items.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                        )
                    }

                    else -> null
                }
            }
            private fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): YTItem? {
                 return when {
                     renderer.isSong -> {
                         SongItem(
                             id = renderer.playlistItemData?.videoId ?: return null,
                             title = renderer.flexColumns.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text?: return null,
                             artists = listOfNotNull(renderer.flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.let {
                                 Artist(
                                     name = it.text,
                                     id = it.navigationEndpoint?.browseEndpoint?.browseId
                                 )
                             }),
                             album = null,
                             duration = null,
                             thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                             explicit = renderer.badges?.find {
                                 it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                             } != null
                         )
                     }
                     else -> null
                 }
            }
        }
    }

    enum class SectionType {
         LIST, GRID
    }

//    fun filterExplicit(enabled: Boolean = true) =
//        if (enabled) {
//            copy(sections = sections.map {
//                it.copy(items = it.items.filterExplicit())
//            })
//        } else this

}

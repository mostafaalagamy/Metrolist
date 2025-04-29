package com.metrolist.innertube.pages

import com.metrolist.innertube.models.Album
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
    val visitorData: String? = null
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
                val header = renderer.header?.musicCarouselShelfBasicHeaderRenderer ?: return null
                val title = header.title?.runs?.firstOrNull()?.text ?: return null
                
                // Validate contents
                if (renderer.contents.isEmpty()) return null
                
                val items = renderer.contents.mapNotNull {
                    it.musicTwoRowItemRenderer?.let { renderer ->
                        fromMusicTwoRowItemRenderer(renderer)
                    } ?: it.musicResponsiveListItemRenderer?.let { renderer ->
                        fromMusicResponsiveListItemRenderer(renderer)
                    }
                }.ifEmpty { return null }

                return Section(
                    title = title,
                    label = header.strapline?.runs?.firstOrNull()?.text,
                    thumbnail = header.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl(),
                    endpoint = header.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint?.copy(
                        params = header.moreContentButton.buttonRenderer.navigationEndpoint.browseEndpoint?.params ?: ""
                    ),
                    items = items,
                    sectionType = if (renderer.contents.any { it.musicResponsiveListItemRenderer != null }) 
                        SectionType.GRID else SectionType.LIST,
                )
            }

            private fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
                return when {
                    renderer.isSong -> {
                        val subtitleRuns = renderer.subtitle?.runs ?: return null
                        val (artistRuns, albumRuns) = subtitleRuns.partition { run ->
                            run.navigationEndpoint?.browseEndpoint?.browseId?.startsWith("UC") == true
                        }

                        val artists = artistRuns.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId
                            )
                        }.takeIf { it.isNotEmpty() } ?: return null

                        SongItem(
                            id = renderer.navigationEndpoint.watchEndpoint?.videoId ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            artists = artists,
                            album = albumRuns.firstOrNull {
                                it.navigationEndpoint?.browseEndpoint?.browseId?.startsWith("MPREb_") == true
                            }?.let { run ->
                                run.navigationEndpoint?.browseEndpoint?.let { endpoint ->
                                    Album(
                                        name = run.text,
                                        id = endpoint.browseId
                                    )
                                }
                            },
                            duration = null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                            explicit = renderer.subtitleBadges?.any {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } == true
                        )
                    }
                    renderer.isAlbum -> {
                        AlbumItem(
                            browseId = renderer.navigationEndpoint.browseEndpoint?.browseId
                                ?: return null,
                            playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint?.playlistId ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            artists = renderer.subtitle?.runs
                                ?.filterNot { it.text == "•" }
                                ?.filter { run ->
                                    run.navigationEndpoint?.browseEndpoint?.browseId?.startsWith("UC") == true
                                }?.map {
                                    Artist(
                                        name = it.text,
                                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                                    )
                                },
                            year = renderer.subtitle?.runs
                                ?.lastOrNull()
                                ?.text?.toIntOrNull(),
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                            explicit = renderer.subtitleBadges?.find {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } != null
                        )
                    }

                    renderer.isPlaylist -> {
                        PlaylistItem(
                            id = renderer.navigationEndpoint.browseEndpoint?.browseId?.removePrefix(
                                "VL"
                            ) ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            author = Artist(
                                name = renderer.subtitle?.runs?.lastOrNull()?.text ?: return null,
                                id = null
                            ),
                            songCountText = null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                            playEndpoint = renderer.thumbnailOverlay
                                ?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint ?: return null,
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                                ?: return null,
                            radioEndpoint = renderer.menu.menuRenderer.items.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                        )
                    }

                    renderer.isArtist -> {
                        ArtistItem(
                            id = renderer.navigationEndpoint.browseEndpoint?.browseId
                                ?: return null,
                            title = renderer.title.runs?.lastOrNull()?.text ?: return null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                                ?: return null,
                            radioEndpoint = renderer.menu.menuRenderer.items.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                                ?: return null,
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
                            title = renderer.flexColumns.firstOrNull()
                                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text
                                ?: return null,
                            artists = renderer.flexColumns.getOrNull(1)
                                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                                ?.mapNotNull { it ->
                                    it.takeIf { run ->
                                        run.navigationEndpoint?.browseEndpoint?.browseId != null
                                    }?.let {
                                        Artist(
                                            name = it.text,
                                            id = it.navigationEndpoint?.browseEndpoint?.browseId
                                        )
                                    }
                                } ?: emptyList(),
                            album = renderer.flexColumns.getOrNull(2)
                                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
                                ?.takeIf {
                                    it.navigationEndpoint?.browseEndpoint?.browseId != null
                                }?.let {
                                    it.navigationEndpoint?.browseEndpoint?.browseId?.let { it1 ->
                                        Album(
                                            name = it.text,
                                            id = it1
                                        )
                                    }
                                },
                            duration = null,
                            thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                            explicit = renderer.badges?.any {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } == true
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
}

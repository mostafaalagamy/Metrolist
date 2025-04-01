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
import com.metrolist.innertube.utils.getThumbnailUrl

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
                val navigationEndpoint = renderer.navigationEndpoint ?: return null
                
                return when {
                    renderer.isSong -> {
                        val subtitleRuns = renderer.subtitle?.runs ?: return null
                        val videoId = navigationEndpoint.watchEndpoint?.videoId ?: return null
                        val title = renderer.title.runs?.firstOrNull()?.text ?: return null
                        val thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null

                        val artists = subtitleRuns
                            .filter { it.navigationEndpoint?.browseEndpoint?.browseId != null }
                            .map {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                                )
                            }
                            .takeIf { it.isNotEmpty() } ?: return null

                        SongItem(
                            id = videoId,
                            title = title,
                            artists = artists,
                            album = subtitleRuns.lastOrNull { run ->
                                run.navigationEndpoint?.browseEndpoint?.browseId != null &&
                                        !artists.any { it.name == run.text }
                            }?.let {
                                it.navigationEndpoint?.browseEndpoint?.browseId?.let { id ->
                                    Album(name = it.text, id = id)
                                }
                            },
                            duration = null,
                            thumbnail = thumbnail,
                            explicit = renderer.subtitleBadges?.any {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } == true
                        )
                    }
                    
                    renderer.isAlbum -> {
                        val browseId = navigationEndpoint.browseEndpoint?.browseId ?: return null
                        val playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint?.playlistId ?: return null
                        val title = renderer.title.runs?.firstOrNull()?.text ?: return null
                        val thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null

                        AlbumItem(
                            browseId = browseId,
                            playlistId = playlistId,
                            title = title,
                            artists = renderer.subtitle?.runs?.oddElements()?.drop(1)?.mapNotNull {
                                it.navigationEndpoint?.browseEndpoint?.browseId?.let { id ->
                                    Artist(name = it.text, id = id)
                                }
                            },
                            year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                            thumbnail = thumbnail,
                            explicit = renderer.subtitleBadges?.any {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } == true
                        )
                    }

                    renderer.isPlaylist -> {
                        val browseId = navigationEndpoint.browseEndpoint?.browseId ?: return null
                        val title = renderer.title.runs?.firstOrNull()?.text ?: return null
                        val thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null
                        val playEndpoint = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchPlaylistEndpoint ?: return null

                        PlaylistItem(
                            id = browseId.removePrefix("VL"),
                            title = title,
                            author = Artist(
                                name = renderer.subtitle?.runs?.lastOrNull()?.text ?: return null,
                                id = null
                            ),
                            songCountText = null,
                            thumbnail = thumbnail,
                            playEndpoint = playEndpoint,
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                            radioEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                        )
                    }

                    renderer.isArtist -> {
                        val browseId = navigationEndpoint.browseEndpoint?.browseId ?: return null
                        val title = renderer.title.runs?.lastOrNull()?.text ?: return null
                        val thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null

                        ArtistItem(
                            id = browseId,
                            title = title,
                            thumbnail = thumbnail,
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                            radioEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                        )
                    }

                    else -> null
                }
            }

            private fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): YTItem? {
                if (renderer.flexColumns.isEmpty()) return null
                
                return when {
                    renderer.isSong -> {
                        val videoId = renderer.playlistItemData?.videoId ?: return null
                        val title = renderer.flexColumns.firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text
                            ?: return null
                        val thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null

                        SongItem(
                            id = videoId,
                            title = title,
                            artists = renderer.flexColumns.getOrNull(1)
                                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                                ?.mapNotNull { run ->
                                    run.navigationEndpoint?.browseEndpoint?.browseId?.let { id ->
                                        Artist(name = run.text, id = id)
                                    }
                                } ?: emptyList(),
                            album = renderer.flexColumns.getOrNull(2)
                                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
                                ?.takeIf { it.navigationEndpoint?.browseEndpoint?.browseId != null }
                                ?.let { run ->
                                    run.navigationEndpoint?.browseEndpoint?.browseId?.let { id ->
                                        Album(name = run.text, id = id)
                                    }
                                },
                            duration = null,
                            thumbnail = thumbnail,
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

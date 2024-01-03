package com.zionhuang.innertube.pages

import com.zionhuang.innertube.models.ArtistItem
import com.zionhuang.innertube.models.MusicCarouselShelfRenderer
import com.zionhuang.innertube.models.PlaylistItem
import com.zionhuang.innertube.models.YTItem

data class AlbumUtils(
    val name: String?,
    val thumbnailUrl: String?
)

data class RecommendationAlbumBundle(
    val recommendedAlbum: AlbumUtils,
    val recommendationAlbum: List<PlaylistItem>
)

data class HomeAlbumRecommendation(
    val albums: RecommendationAlbumBundle,
)

data class HomeArtistRecommendation(
    var listItem: List<YTItem>,
//    var playlists: RecommendationAlbumBundle,
//    var artists: List<ArtistItem>,
    val artistName: String
)

data class HomePlayList(
    val playlists: List<PlaylistItem>,
    val playlistName: String,
    val continuation: String?
) {
    companion object {
        fun fromMusicCarouselShelfRenderer(renderer: MusicCarouselShelfRenderer, continuation: String?): HomePlayList {
            return HomePlayList(
                playlistName = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs!![0].text,
                playlists = renderer.contents
                    .mapNotNull { it.musicTwoRowItemRenderer }
                    .mapNotNull {
                        ArtistItemsPage.fromMusicTwoRowItemRenderer(it) as? PlaylistItem
                    },
                continuation = continuation
            )
        }
    }
}
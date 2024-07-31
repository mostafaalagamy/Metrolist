package com.metrolist.innertube

import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.metrolist.innertube.models.WatchEndpoint
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

@Ignore("IDK Why GitHub Action always runs the test with error")
class YouTubeTest {
    private val youTube = YouTube

    @Test
    fun `Check 'player' endpoint`() = runBlocking {
        VIDEO_IDS.forEach { videoId ->
            val playerResponse = youTube.player(videoId).getOrThrow()
            assertTrue(playerResponse.playabilityStatus.status == "OK")
        }
    }

    @Test
    fun `Check playable stream`() = runBlocking {
        VIDEO_IDS.forEach { videoId ->
            val playerResponse = youTube.player(videoId).getOrThrow()
            val format = playerResponse.streamingData!!.adaptiveFormats[0]
            val url = format.url!!
            println(url)
            val response = HttpClient(OkHttp).get(url) {
                headers {
                    append("Range", "bytes=0-10")
                }
            }
            assertTrue(response.status.isSuccess())
        }
    }

    @Test
    fun `Check 'search' endpoint`() = runBlocking {
        // Top result with radio link
        val searchAllTypeResult = youTube.searchSummary("musi").getOrThrow()
        assertTrue(searchAllTypeResult.summaries.size > 1)
        for (filter in listOf(
            FILTER_SONG,
            FILTER_VIDEO,
            FILTER_ALBUM,
            FILTER_ARTIST,
            FILTER_FEATURED_PLAYLIST,
            FILTER_COMMUNITY_PLAYLIST
        )) {
            val searchResult = youTube.search(SEARCH_QUERY, filter).getOrThrow()
            assertTrue(searchResult.items.isNotEmpty())
        }
    }

    @Test
    fun `Check search continuation`() = runBlocking {
        var count = 5
        var searchResult = youTube.search(SEARCH_QUERY, FILTER_SONG).getOrThrow()
        while (searchResult.continuation != null && count > 0) {
            searchResult.items.forEach {
                println(it.title)
            }
            searchResult = youTube.searchContinuation(searchResult.continuation!!).getOrThrow()
            count -= 1
        }
        searchResult.items.forEach {
            println(it.title)
        }
    }

    @Test
    fun `Check 'get_search_suggestion' endpoint`() = runBlocking {
        val suggestions = youTube.searchSuggestions(SEARCH_QUERY).getOrThrow()
        assertTrue(suggestions.queries.isNotEmpty())
    }

    @Test
    fun `Check 'browse' endpoint`() = runBlocking {
        var artist = youTube.artist("UCI6B8NkZKqlFWoiC_xE-hzA").getOrThrow()
        assertTrue(artist.sections.isNotEmpty())
        artist = youTube.artist("UCy2RKLxIOMOfGld_yBYEBLw").getOrThrow() // Artist that contains audiobook
        assertTrue(artist.sections.isNotEmpty())
        val album = youTube.album("MPREb_oNAdr9eUOfS").getOrThrow()
        assertTrue(album.songs.isNotEmpty())
        val playlist = youTube.playlist("RDCLAK5uy_mHAEb33pqvgdtuxsemicZNu-5w6rLRweo").getOrThrow()
        assertTrue(playlist.songs.isNotEmpty())
    }

    @Test
    fun `Check 'next' endpoint`() = runBlocking {
        var nextResult = youTube.next(WatchEndpoint(videoId = "qivRUhepWVA", playlistId = "RDEMQWAKLFUHzBCn9nEsPHDYAw")).getOrThrow()
        assertTrue(nextResult.items.isNotEmpty())
        nextResult = youTube.next(WatchEndpoint(videoId = "jF4KKOsoyDs", playlistId = "PLaHh1PiehjvqOXm1J7b2QGy2iAvN84Azb")).getOrThrow()
        assertTrue(nextResult.items.isNotEmpty())
    }

    @Test
    fun `Check 'next' continuation`() = runBlocking {
        val endpoint = WatchEndpoint(videoId = "afXwxtQLvZM")
        var count = 5
        var nextResult = youTube.next(endpoint).getOrThrow()
        while (nextResult.continuation != null && count > 0) {
            nextResult.items.forEach {
                println(it.title)
            }
            nextResult = youTube.next(nextResult.endpoint, nextResult.continuation).getOrThrow()
            count -= 1
        }
        nextResult.items.forEach {
            println(it.title)
        }
    }

    @Test
    fun `Check 'get_queue' endpoint`() = runBlocking {
        var queue = youTube.queue(videoIds = VIDEO_IDS).getOrThrow()
        assertTrue(queue.isNotEmpty())
        queue = youTube.queue(playlistId = PLAYLIST_ID).getOrThrow()
        assertTrue(queue.isNotEmpty())
    }

    @Test
    fun `Browse playlist`() = runBlocking {
        // This playlist has 2900 songs
        val playlistId = "PLtAw-mgfCzRwduBTjBHknz5U4_ZM4n6qm"
        var count = 5
        val playlistPage = YouTube.playlist(playlistId).getOrThrow()
        var songs = playlistPage.songs
        var continuation = playlistPage.songsContinuation
        while (count > 0) {
            songs.forEach {
                println(it.id)
            }
            if (continuation == null) break
            val continuationPage = YouTube.playlistContinuation(continuation).getOrThrow()
            songs = continuationPage.songs
            continuation = continuationPage.continuation
            count--
        }
    }

    @Test
    fun lyrics() = runBlocking {
        val nextResult = YouTube.next(WatchEndpoint(videoId = "NCC6lI0GGy0")).getOrThrow()
        val lyrics = YouTube.lyrics(nextResult.lyricsEndpoint!!).getOrThrow()
        assertTrue(lyrics != null)
    }

    @Test
    fun related() = runBlocking {
        val relatedEndpoint = YouTube.next(WatchEndpoint(videoId = "Z6ji6kls_OA")).getOrThrow().relatedEndpoint!!
        val relatedPage = YouTube.related(relatedEndpoint).getOrThrow()
        assertTrue(relatedPage.songs.isNotEmpty())
    }

    @Test
    fun transcript() = runBlocking {
        val lyrics = YouTube.transcript("7G0ovtPqHnI").getOrThrow()
        assertTrue(lyrics.isNotEmpty())
    }

    companion object {
        private val VIDEO_IDS = listOf(
            "4H-N260cPCg",
            "jF4KKOsoyDs",
            "x8VYWazR5mE" // Login required
        )

        private const val PLAYLIST_ID = "RDAMVM_WVXrDmm-P0"

        private const val SEARCH_QUERY = "YOASOBI"
    }
}

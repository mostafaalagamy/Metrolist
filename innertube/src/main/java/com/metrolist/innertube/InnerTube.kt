package com.metrolist.innertube

import com.metrolist.innertube.models.Context
import com.metrolist.innertube.models.YouTubeClient
import com.metrolist.innertube.models.YouTubeLocale
import com.metrolist.innertube.models.body.*
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.innertube.utils.sha1
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.encodeBase64
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.net.Proxy
import java.util.*

/**
 * Provide access to InnerTube endpoints.
 * For making HTTP requests, not parsing response.
 */
class InnerTube {
    private var httpClient = createClient()

    var locale = YouTubeLocale(
        gl = Locale.getDefault().country,
        hl = Locale.getDefault().toLanguageTag()
    )
    var visitorData: String? = null
    var dataSyncId: String? = null
    var cookie: String? = null
        set(value) {
            field = value
            cookieMap = if (value == null) emptyMap() else parseCookieString(value)
        }
    private var cookieMap = emptyMap<String, String>()

    var proxy: Proxy? = null
        set(value) {
            field = value
            httpClient.close()
            httpClient = createClient()
        }

    var useLoginForBrowse: Boolean = false

    @OptIn(ExperimentalSerializationApi::class)
    private fun createClient() = HttpClient(OkHttp) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            })
        }

        install(ContentEncoding) {
            gzip(0.9F)
            deflate(0.8F)
        }

        if (proxy != null) {
            engine {
                proxy = this@InnerTube.proxy
            }
        }

        defaultRequest {
            url(YouTubeClient.API_URL_YOUTUBE_MUSIC)
        }
    }

    private fun HttpRequestBuilder.ytClient(client: YouTubeClient, setLogin: Boolean = false) {
        contentType(ContentType.Application.Json)
        headers {
            append("X-Goog-Api-Format-Version", "1")
            append("X-YouTube-Client-Name", client.clientId /* Not a typo. The Client-Name header does contain the client id. */)
            append("X-YouTube-Client-Version", client.clientVersion)
            append("X-Origin", YouTubeClient.ORIGIN_YOUTUBE_MUSIC)
            append("Referer", YouTubeClient.REFERER_YOUTUBE_MUSIC)
            if (setLogin && client.loginSupported) {
                cookie?.let { cookie ->
                    append("cookie", cookie)
                    if ("SAPISID" !in cookieMap) return@let
                    val currentTime = System.currentTimeMillis() / 1000
                    val sapisidHash = sha1("$currentTime ${cookieMap["SAPISID"]} ${YouTubeClient.ORIGIN_YOUTUBE_MUSIC}")
                    append("Authorization", "SAPISIDHASH ${currentTime}_${sapisidHash}")
                }
            }
        }
        userAgent(client.userAgent)
        parameter("prettyPrint", false)
    }

    suspend fun search(
        client: YouTubeClient,
        query: String? = null,
        params: String? = null,
        continuation: String? = null,
    ) = httpClient.post("search") {
        ytClient(client, setLogin = useLoginForBrowse)
        setBody(
            SearchBody(
                context = client.toContext(
                    locale,
                    visitorData,
                    if (useLoginForBrowse) dataSyncId else null
                ),
                query = query,
                params = params
            )
        )
        parameter("continuation", continuation)
        parameter("ctoken", continuation)
    }

    suspend fun player(
        client: YouTubeClient,
        videoId: String,
        playlistId: String?,
        signatureTimestamp: Int?,
        webPlayerPot: String?,
    ) = httpClient.post("player") {
        ytClient(client, setLogin = true)
        setBody(
            PlayerBody(
                context = client.toContext(locale, visitorData, dataSyncId).let {
                    if (client.isEmbedded) {
                        it.copy(
                            thirdParty = Context.ThirdParty(
                                embedUrl = "https://www.youtube.com/watch?v=${videoId}"
                            )
                        )
                    } else it
                },
                videoId = videoId,
                playlistId = playlistId,
                playbackContext = if (client.useSignatureTimestamp && signatureTimestamp != null) {
                    PlayerBody.PlaybackContext(
                        PlayerBody.PlaybackContext.ContentPlaybackContext(
                            signatureTimestamp
                        )
                    )
                } else null,
                serviceIntegrityDimensions = if (client.useWebPoTokens && webPlayerPot != null) {
                    PlayerBody.ServiceIntegrityDimensions(webPlayerPot)
                } else null
            )
        )
    }

    suspend fun registerPlayback(
        url: String,
        cpn: String,
        playlistId: String?,
        client: YouTubeClient = YouTubeClient.WEB_REMIX,
    ) = httpClient.get(url) {
        ytClient(client, true)
        parameter("ver", "2")
        parameter("c", client.clientName)
        parameter("cpn", cpn)

        if (playlistId != null) {
            parameter("list", playlistId)
            parameter("referrer", "https://music.youtube.com/playlist?list=$playlistId")
        }
    }

    suspend fun browse(
        client: YouTubeClient,
        browseId: String? = null,
        params: String? = null,
        continuation: String? = null,
        setLogin: Boolean = false,
    ) = httpClient.post("browse") {
        ytClient(client, setLogin = setLogin || useLoginForBrowse)
        setBody(
            BrowseBody(
                context = client.toContext(
                    locale,
                    visitorData,
                    if (setLogin || useLoginForBrowse) dataSyncId else null
                ),
                browseId = browseId,
                params = params,
                continuation = continuation
            )
        )
    }

    suspend fun next(
        client: YouTubeClient,
        videoId: String?,
        playlistId: String?,
        playlistSetVideoId: String?,
        index: Int?,
        params: String?,
        continuation: String? = null,
    ) = httpClient.post("next") {
        ytClient(client, setLogin = true)
        setBody(
            NextBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                videoId = videoId,
                playlistId = playlistId,
                playlistSetVideoId = playlistSetVideoId,
                index = index,
                params = params,
                continuation = continuation
            )
        )
    }

    suspend fun getSearchSuggestions(
        client: YouTubeClient,
        input: String,
    ) = httpClient.post("music/get_search_suggestions") {
        ytClient(client)
        setBody(
            GetSearchSuggestionsBody(
                context = client.toContext(locale, visitorData, null),
                input = input
            )
        )
    }

    suspend fun getQueue(
        client: YouTubeClient,
        videoIds: List<String>?,
        playlistId: String?,
    ) = httpClient.post("music/get_queue") {
        ytClient(client)
        setBody(
            GetQueueBody(
                context = client.toContext(locale, visitorData, null),
                videoIds = videoIds,
                playlistId = playlistId
            )
        )
    }

    suspend fun getTranscript(
        client: YouTubeClient,
        videoId: String,
    ) = httpClient.post("https://music.youtube.com/youtubei/v1/get_transcript") {
        parameter("key", "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3")
        headers {
            append("Content-Type", "application/json")
        }
        setBody(
            GetTranscriptBody(
                context = client.toContext(locale, null, null),
                params = "\n${11.toChar()}$videoId".encodeBase64()
            )
        )
    }

    suspend fun getSwJsData() = httpClient.get("https://music.youtube.com/sw.js_data")

    suspend fun accountMenu(client: YouTubeClient) = httpClient.post("account/account_menu") {
        ytClient(client, setLogin = true)
        setBody(AccountMenuBody(client.toContext(locale, visitorData, dataSyncId)))
    }

    suspend fun likeVideo(
        client: YouTubeClient,
        videoId: String,
    ) = httpClient.post("like/like") {
        ytClient(client, setLogin = true)
        setBody(
            LikeBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                target = LikeBody.Target.VideoTarget(videoId)
            )
        )
    }

    suspend fun unlikeVideo(
        client: YouTubeClient,
        videoId: String,
    ) = httpClient.post("like/removelike") {
        ytClient(client, setLogin = true)
        setBody(
            LikeBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                target = LikeBody.Target.VideoTarget(videoId)
            )
        )
    }

    suspend fun subscribeChannel(
        client: YouTubeClient,
        channelId: String,
    ) = httpClient.post("subscription/subscribe") {
        ytClient(client, setLogin = true)
        setBody(
            SubscribeBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                channelIds = listOf(channelId)
            )
        )
    }

    suspend fun unsubscribeChannel(
        client: YouTubeClient,
        channelId: String,
    ) = httpClient.post("subscription/unsubscribe") {
        ytClient(client, setLogin = true)
        setBody(
            SubscribeBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                channelIds = listOf(channelId)
            )
        )
    }

    suspend fun likePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = httpClient.post("like/like") {
        ytClient(client, setLogin = true)
        setBody(
            LikeBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                target = LikeBody.Target.PlaylistTarget(playlistId)
            )
        )
    }

    suspend fun unlikePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = httpClient.post("like/removelike") {
        ytClient(client, setLogin = true)
        setBody(
            LikeBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                target = LikeBody.Target.PlaylistTarget(playlistId)
            )
        )
    }

    suspend fun addToPlaylist(
        client: YouTubeClient,
        playlistId: String,
        videoId: String,
    ) = httpClient.post("browse/edit_playlist") {
        ytClient(client, setLogin = true)
        setBody(
            EditPlaylistBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                playlistId = playlistId.removePrefix("VL"),
                actions = listOf(
                    Action.AddVideoAction(addedVideoId = videoId)
                )
            )
        )
    }

    suspend fun addPlaylistToPlaylist(
        client: YouTubeClient,
        playlistId: String,
        addPlaylistId: String,
    ) = httpClient.post("browse/edit_playlist") {
        ytClient(client, setLogin = true)
        setBody(
            EditPlaylistBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                playlistId = playlistId.removePrefix("VL"),
                actions = listOf(
                    Action.AddPlaylistAction(addedFullListId = addPlaylistId)
                )
            )
        )
    }

    suspend fun removeFromPlaylist(
        client: YouTubeClient,
        playlistId: String,
        videoId: String,
        setVideoId: String,
    ) = httpClient.post("browse/edit_playlist") {
        ytClient(client, setLogin = true)
        setBody(
            EditPlaylistBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                playlistId = playlistId.removePrefix("VL"),
                actions = listOf(
                    Action.RemoveVideoAction(
                        removedVideoId = videoId,
                        setVideoId = setVideoId,
                    )
                )
            )
        )
    }

    suspend fun moveSongPlaylist(
        client: YouTubeClient,
        playlistId: String,
        setVideoId: String,
        successorSetVideoId: String,
    ) = httpClient.post("browse/edit_playlist") {
        ytClient(client, setLogin = true)
        setBody(
            EditPlaylistBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                playlistId = playlistId,
                actions = listOf(
                    Action.MoveVideoAction(
                        movedSetVideoIdSuccessor = successorSetVideoId,
                        setVideoId = setVideoId,
                    )
                )

            )
        )
    }

    suspend fun createPlaylist(
        client: YouTubeClient,
        title: String,
    ) = httpClient.post("playlist/create") {
        ytClient(client, true)
        setBody(
            CreatePlaylistBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                title = title
            )
        )
    }

    suspend fun renamePlaylist(
        client: YouTubeClient,
        playlistId: String,
        name: String,
    ) = httpClient.post("browse/edit_playlist") {
        ytClient(client, setLogin = true)
        setBody(
            EditPlaylistBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                playlistId = playlistId,
                actions = listOf(
                    Action.RenamePlaylistAction(
                        playlistName = name
                    )
                )
            )
        )
    }

    suspend fun deletePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = httpClient.post("playlist/delete") {
        println("deleting $playlistId")
        ytClient(client, setLogin = true)
        setBody(
            PlaylistDeleteBody(
                context = client.toContext(locale, visitorData, dataSyncId),
                playlistId = playlistId
            )
        )
    }
}

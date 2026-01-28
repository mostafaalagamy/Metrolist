package com.metrolist.innertube

import com.metrolist.innertube.models.YouTubeClient
import com.metrolist.innertube.models.response.PlayerResponse
import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe as OfficialNewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager as OfficialYoutubeJSPlayerManager
import java.io.IOException
import java.net.Proxy

/**
 * Downloader implementation for the official NewPipe Extractor from TeamNewPipe.
 * This uses the official upstream version from https://github.com/TeamNewPipe/NewPipeExtractor
 */
private class NewPipeExtractorDownloaderImpl(proxy: Proxy?, proxyAuth: String?) : Downloader() {

    private val client = OkHttpClient.Builder()
        .proxy(proxy)
        .proxyAuthenticator { _, response ->
            proxyAuth?.let { auth ->
                response.request.newBuilder()
                    .header("Proxy-Authorization", auth)
                    .build()
            } ?: response.request
        }
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, dataToSend?.toRequestBody())
            .url(url)
            .addHeader("User-Agent", YouTubeClient.USER_AGENT_WEB)

        headers.forEach { (headerName, headerValueList) ->
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                headerValueList.forEach { headerValue ->
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            response.close()
            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        val responseBodyToReturn = response.body.string()
        val latestUrl = response.request.url.toString()
        return Response(response.code, response.message, response.headers.toMultimap(), responseBodyToReturn, latestUrl)
    }
}

/**
 * Utility object for the official NewPipe Extractor from TeamNewPipe.
 * This is the default and recommended decryption library.
 * 
 * Source: https://github.com/TeamNewPipe/NewPipeExtractor
 * 
 * This library is actively maintained by the NewPipe team and receives
 * frequent updates to handle YouTube's signature changes.
 */
object NewPipeExtractorUtils {

    @Volatile
    private var isInitialized = false
    private val initLock = Any()

    private fun ensureInitialized() {
        if (!isInitialized) {
            synchronized(initLock) {
                if (!isInitialized) {
                    OfficialNewPipe.init(NewPipeExtractorDownloaderImpl(YouTube.proxy, YouTube.proxyAuth))
                    isInitialized = true
                }
            }
        }
    }

    /**
     * Gets the signature timestamp required for stream URL decryption.
     * 
     * @param videoId The YouTube video ID
     * @return Result containing the signature timestamp or an error
     */
    fun getSignatureTimestamp(videoId: String): Result<Int> = runCatching {
        ensureInitialized()
        OfficialYoutubeJSPlayerManager.getSignatureTimestamp(videoId)
    }

    /**
     * Gets the decrypted stream URL for a given format.
     * Handles both direct URLs and signature cipher URLs.
     * 
     * @param format The format containing URL or signatureCipher
     * @param videoId The YouTube video ID
     * @return Result containing the decrypted stream URL or an error
     */
    fun getStreamUrl(format: PlayerResponse.StreamingData.Format, videoId: String): Result<String> =
        runCatching {
            ensureInitialized()
            
            val url = format.url ?: format.signatureCipher?.let { signatureCipher ->
                val params = parseQueryString(signatureCipher)
                val obfuscatedSignature = params["s"]
                    ?: throw ParsingException("Could not parse cipher signature")
                val signatureParam = params["sp"]
                    ?: throw ParsingException("Could not parse cipher signature parameter")
                val urlBuilder = params["url"]?.let { URLBuilder(it) }
                    ?: throw ParsingException("Could not parse cipher url")
                urlBuilder.parameters[signatureParam] =
                    OfficialYoutubeJSPlayerManager.deobfuscateSignature(
                        videoId,
                        obfuscatedSignature
                    )
                urlBuilder.toString()
            } ?: throw ParsingException("Could not find format url")

            return@runCatching OfficialYoutubeJSPlayerManager.getUrlWithThrottlingParameterDeobfuscated(
                videoId,
                url
            )
        }
    
    /**
     * Clears cached player data to force refresh on next use.
     * Call this when decryption errors occur to get fresh player data.
     */
    fun clearCache() {
        synchronized(initLock) {
            isInitialized = false
        }
    }
}

package com.metrolist.innertube

import com.metrolist.innertube.models.response.PlayerResponse
import io.ktor.http.parseQueryString
import kotlinx.coroutines.runBlocking
import project.pipepipe.extractor.services.youtube.YouTubeDecryptionHelper
import java.net.URLEncoder

object NewPipeUtils {

    private var cachedPlayer: Pair<String, Int>? = null

    private fun getPlayer(): Pair<String, Int>? {
        if (cachedPlayer == null) {
            cachedPlayer = YouTubeDecryptionHelper.getLatestPlayer()
        }
        return cachedPlayer
    }

    fun getSignatureTimestamp(videoId: String): Result<Int> = runCatching {
        getPlayer()?.second ?: throw Exception("Could not get signature timestamp")
    }

    fun getStreamUrl(format: PlayerResponse.StreamingData.Format, videoId: String): Result<String> =
        runCatching {
            val player = getPlayer()?.first ?: throw Exception("Could not get player")
            
            // If format has direct URL
            format.url?.let { directUrl ->
                // Check if URL has encrypted 'n' parameter that needs decryption
                val nParam = parseQueryString(directUrl.substringAfter("?"))["n"]
                if (nParam != null) {
                    val decrypted = runBlocking {
                        YouTubeDecryptionHelper.batchDecryptCiphers(listOf(nParam), emptyList(), player)
                    }
                    val decryptedN = decrypted[nParam] ?: throw Exception("Could not decrypt n parameter")
                    return@runCatching directUrl.replace("n=$nParam", "n=${URLEncoder.encode(decryptedN, "UTF-8")}")
                }
                return@runCatching directUrl
            }

            // If format has signatureCipher
            format.signatureCipher?.let { signatureCipher ->
                val params = parseQueryString(signatureCipher)
                val obfuscatedSignature = params["s"]
                    ?: throw Exception("Could not parse cipher signature")
                val signatureParam = params["sp"] ?: "sig"
                val baseUrl = params["url"]
                    ?: throw Exception("Could not parse cipher url")

                // Determine if this is 'n' or 'sig' type
                val (nValues, sigValues) = if (signatureParam == "n") {
                    listOf(obfuscatedSignature) to emptyList()
                } else {
                    emptyList<String>() to listOf(obfuscatedSignature)
                }

                val decrypted = runBlocking {
                    YouTubeDecryptionHelper.batchDecryptCiphers(nValues, sigValues, player)
                }
                val decryptedValue = decrypted[obfuscatedSignature]
                    ?: throw Exception("Could not decrypt signature")

                return@runCatching "$baseUrl&$signatureParam=${URLEncoder.encode(decryptedValue, "UTF-8")}"
            }

            throw Exception("Could not find format url")
        }

}
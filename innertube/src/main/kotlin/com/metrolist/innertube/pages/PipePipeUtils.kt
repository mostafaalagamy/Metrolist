package com.metrolist.innertube

import com.metrolist.innertube.models.response.PlayerResponse
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import project.pipepipe.extractor.services.youtube.YouTubeDecryptionHelper
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Utility object for handling YouTube stream URL decryption using PipePipe Extractor.
 * Provides thread-safe access to player information and stream URL decryption.
 * 
 * This is the newer decryption library that uses the PipePipe API for signature decryption.
 * It's generally faster and more reliable than the NewPipe-based implementation.
 */
object PipePipeUtils {
    private const val TAG = "PipePipeUtils"
    private const val DEBUG = false
    private const val API_TIMEOUT_MS = 10000L

    @Volatile
    private var cachedPlayer: Pair<String, Int>? = null

    private val playerLock = Any()

    private fun log(message: String) {
        if (DEBUG) println("$TAG: $message")
    }

    /**
     * Gets the current YouTube player information (player ID and signature timestamp).
     * Results are cached to avoid unnecessary API calls.
     * Thread-safe.
     *
     * @return Pair of (player ID, signature timestamp) or null if unavailable
     */
    private fun getPlayer(): Pair<String, Int>? {
        cachedPlayer?.let { return it }

        return synchronized(playerLock) {
            // Double-check after acquiring lock
            cachedPlayer?.let { return it }

            log("Fetching latest player from API...")
            val player = YouTubeDecryptionHelper.getLatestPlayer()
            log("Got player: ${player?.first}, sts: ${player?.second}")
            cachedPlayer = player
            player
        }
    }

    /**
     * Clears the cached player information to force a refresh on next use.
     * Thread-safe.
     */
    fun clearCache() {
        synchronized(playerLock) {
            cachedPlayer = null
        }
    }

    /**
     * Gets the signature timestamp for the current YouTube player.
     *
     * @param videoId The video ID (for logging purposes)
     * @return Result containing the signature timestamp or an error
     */
    fun getSignatureTimestamp(videoId: String): Result<Int> = runCatching {
        require(videoId.isNotBlank()) { "Video ID cannot be blank" }
        getPlayer()?.second ?: throw IllegalStateException("Could not get signature timestamp from player API")
    }

    /**
     * Extracts a query parameter value from a URL, handling URL encoding properly.
     *
     * @param url The URL to extract from
     * @param param The parameter name to extract
     * @return The decoded parameter value, or null if not found
     */
    private fun getQueryParam(url: String, param: String): String? {
        require(param.isNotBlank()) { "Parameter name cannot be blank" }

        val queryStart = url.indexOf('?')
        if (queryStart == -1 || queryStart == url.length - 1) return null

        val query = url.substring(queryStart + 1)
        if (query.isBlank()) return null

        for (p in query.split('&')) {
            if (p.isBlank()) continue

            val parts = p.split('=', limit = 2)
            if (parts.size == 2 && parts[0] == param) {
                return try {
                    URLDecoder.decode(parts[1], "UTF-8")
                } catch (e: Exception) {
                    log("Failed to decode parameter value: ${e.message}")
                    parts[1] // Return unencoded if decoding fails
                }
            }
        }
        return null
    }

    /**
     * Replaces a query parameter value in a URL with proper URL encoding.
     * If the parameter doesn't exist, it will be appended.
     *
     * @param url The URL to modify
     * @param param The parameter name to replace
     * @param newValue The new value (will be URL-encoded)
     * @return The modified URL
     */
    private fun replaceQueryParam(url: String, param: String, newValue: String): String {
        require(param.isNotBlank()) { "Parameter name cannot be blank" }

        val queryStart = url.indexOf('?')
        if (queryStart == -1) {
            // No query string, append new parameter
            return "$url?$param=${URLEncoder.encode(newValue, "UTF-8")}"
        }

        val baseUrl = url.substring(0, queryStart)
        val query = url.substring(queryStart + 1)

        if (query.isBlank()) {
            return "$baseUrl?$param=${URLEncoder.encode(newValue, "UTF-8")}"
        }

        val params = query.split('&').toMutableList()
        val encodedNewValue = URLEncoder.encode(newValue, "UTF-8")
        var found = false

        for (i in params.indices) {
            val parts = params[i].split('=', limit = 2)
            if (parts[0] == param) {
                params[i] = "$param=$encodedNewValue"
                found = true
                break
            }
        }

        if (!found) {
            params.add("$param=$encodedNewValue")
        }

        return "$baseUrl?${params.joinToString("&")}"
    }

    /**
     * Decrypts cipher values using the PipePipe API.
     *
     * @param nValues List of encrypted 'n' parameter values
     * @param sigValues List of encrypted signature values
     * @param player The player ID to use for decryption
     * @return Map of encrypted value to decrypted value
     * @throws IllegalStateException if decryption fails
     */
    private suspend fun decryptCiphers(
        nValues: List<String>,
        sigValues: List<String>,
        player: String
    ): Map<String, String> {
        if (nValues.isEmpty() && sigValues.isEmpty()) {
            return emptyMap()
        }

        return try {
            withTimeout(API_TIMEOUT_MS) {
                YouTubeDecryptionHelper.batchDecryptCiphers(nValues, sigValues, player)
            }
        } catch (e: Exception) {
            log("Decryption failed: ${e.message}")
            throw IllegalStateException("Failed to decrypt stream parameters: ${e.message}", e)
        }
    }

    /**
     * Parses a cipher query string into a map of parameter names to decoded values.
     *
     * @param cipherString The cipher query string (e.g., "s=...&sp=sig&url=...")
     * @return Map of parameter names to decoded values
     */
    private fun parseCipherParams(cipherString: String): Map<String, String> {
        if (cipherString.isBlank()) return emptyMap()

        return cipherString.split('&')
            .filter { it.isNotBlank() }
            .associate { param ->
                val parts = param.split('=', limit = 2)
                if (parts.size == 2) {
                    parts[0] to try {
                        URLDecoder.decode(parts[1], "UTF-8")
                    } catch (e: Exception) {
                        log("Failed to decode cipher parameter: ${e.message}")
                        parts[1]
                    }
                } else {
                    parts[0] to ""
                }
            }
    }

    /**
     * Gets the decrypted stream URL for a given format and video.
     * Handles both direct URLs with encrypted 'n' parameters and signatureCipher formats.
     *
     * @param format The format object containing URL or signatureCipher
     * @param videoId The video ID (for logging purposes)
     * @return Result containing the decrypted stream URL or an error
     */
    fun getStreamUrl(format: PlayerResponse.StreamingData.Format, videoId: String): Result<String> =
        runCatching {
            require(videoId.isNotBlank()) { "Video ID cannot be blank" }

            val player = getPlayer()?.first
                ?: throw IllegalStateException("Player information unavailable from API")

            log("Processing stream URL for videoId: $videoId with player: $player")

            // Case 1: Format has direct URL (may need 'n' parameter decryption)
            format.url?.let { directUrl ->
                log("Format has direct URL")

                val nParam = getQueryParam(directUrl, "n")
                if (nParam != null && nParam.isNotBlank()) {
                    log("Found n parameter (length: ${nParam.length})")

                    val decrypted = runBlocking {
                        decryptCiphers(listOf(nParam), emptyList(), player)
                    }

                    val decryptedN = decrypted[nParam]
                        ?: throw IllegalStateException("Failed to decrypt 'n' parameter")

                    log("Successfully decrypted n parameter (length: ${decryptedN.length})")
                    return@runCatching replaceQueryParam(directUrl, "n", decryptedN)
                }

                log("No n parameter found, returning direct URL")
                return@runCatching directUrl
            }

            // Case 2: Format has signatureCipher (needs signature and possibly 'n' decryption)
            format.signatureCipher?.let { signatureCipher ->
                log("Format has signatureCipher")

                val params = parseCipherParams(signatureCipher)

                val obfuscatedSignature = params["s"]
                    ?: throw IllegalArgumentException("Missing 's' parameter in signatureCipher")
                val signatureParam = params["sp"]?.takeIf { it.isNotBlank() } ?: "sig"
                val baseUrl = params["url"]
                    ?: throw IllegalArgumentException("Missing 'url' parameter in signatureCipher")

                log("Cipher - sp: $signatureParam, s length: ${obfuscatedSignature.length}")

                // Decrypt the signature
                val sigDecrypted = runBlocking {
                    decryptCiphers(emptyList(), listOf(obfuscatedSignature), player)
                }

                val decryptedSig = sigDecrypted[obfuscatedSignature]
                    ?: throw IllegalStateException("Failed to decrypt signature")

                log("Successfully decrypted signature (length: ${decryptedSig.length})")

                // Build URL with decrypted signature
                var finalUrl = "$baseUrl&$signatureParam=${URLEncoder.encode(decryptedSig, "UTF-8")}"

                // Check if base URL also has an 'n' parameter that needs decryption
                val nParam = getQueryParam(baseUrl, "n")
                if (nParam != null && nParam.isNotBlank()) {
                    log("Base URL has n parameter, decrypting...")

                    val nDecrypted = runBlocking {
                        decryptCiphers(listOf(nParam), emptyList(), player)
                    }

                    nDecrypted[nParam]?.let { decryptedN ->
                        log("Successfully decrypted base URL n parameter")
                        finalUrl = replaceQueryParam(finalUrl, "n", decryptedN)
                    }
                }

                return@runCatching finalUrl
            }

            // Case 3: Neither URL nor signatureCipher present
            throw IllegalArgumentException("Format has neither 'url' nor 'signatureCipher' field")
        }.onFailure { error ->
            log("Stream URL extraction failed: ${error.message}")
        }

}

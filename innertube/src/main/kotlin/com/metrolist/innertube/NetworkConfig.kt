package com.metrolist.innertube

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Enhanced network configuration for better performance and reliability
 * Inspired by ArchiveTune optimizations
 */
object NetworkConfig {
    
    // Timeout settings
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 60L
    private const val WRITE_TIMEOUT_SECONDS = 60L
    private const val REQUEST_TIMEOUT_MILLIS = 60000L
    
    // Cache settings
    private const val CACHE_SIZE_MB = 50L * 1024L * 1024L // 50 MB
    
    @OptIn(ExperimentalSerializationApi::class)
    fun createOptimizedHttpClient(
        cacheDir: File? = null,
        enableCache: Boolean = true
    ): HttpClient = HttpClient(OkHttp) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
                isLenient = true
            })
        }

        install(ContentEncoding) {
            gzip(0.9F)
            deflate(0.8F)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            connectTimeoutMillis = CONNECT_TIMEOUT_SECONDS * 1000
            socketTimeoutMillis = READ_TIMEOUT_SECONDS * 1000
        }

        engine {
            config {
                // Timeout configurations
                connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                
                // Retry configuration
                retryOnConnectionFailure(true)
                
                // Cache configuration
                if (enableCache) {
                    val cacheDirectory = cacheDir ?: File(System.getProperty("java.io.tmpdir"), "metrolist_http_cache")
                    cache(okhttp3.Cache(cacheDirectory, CACHE_SIZE_MB))
                }
            }
        }
    }
    
    /**
     * Create a client specifically optimized for YouTube Music API
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun createYouTubeMusicClient(
        cacheDir: File? = null
    ): HttpClient {
        val baseClient = createOptimizedHttpClient(cacheDir)
        return baseClient.config {
            // Additional configuration can be added here if needed
        }
    }
    
    /**
     * Network quality detection and adaptive configuration
     */
    fun getAdaptiveTimeouts(networkQuality: NetworkQuality): TimeoutConfig {
        return when (networkQuality) {
            NetworkQuality.EXCELLENT -> TimeoutConfig(
                connectTimeout = 10000L,
                readTimeout = 30000L,
                requestTimeout = 45000L
            )
            NetworkQuality.GOOD -> TimeoutConfig(
                connectTimeout = 20000L,
                readTimeout = 45000L,
                requestTimeout = 60000L
            )
            NetworkQuality.POOR -> TimeoutConfig(
                connectTimeout = 30000L,
                readTimeout = 60000L,
                requestTimeout = 90000L
            )
            NetworkQuality.UNKNOWN -> TimeoutConfig(
                connectTimeout = CONNECT_TIMEOUT_SECONDS * 1000,
                readTimeout = READ_TIMEOUT_SECONDS * 1000,
                requestTimeout = REQUEST_TIMEOUT_MILLIS
            )
        }
    }
    
    enum class NetworkQuality {
        EXCELLENT, GOOD, POOR, UNKNOWN
    }
    
    data class TimeoutConfig(
        val connectTimeout: Long,
        val readTimeout: Long,
        val requestTimeout: Long
    )
}
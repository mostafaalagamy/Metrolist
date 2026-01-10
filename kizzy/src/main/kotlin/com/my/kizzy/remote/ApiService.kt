/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * ApiService.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */
package com.my.kizzy.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.logging.Logger

/**
 * Modified by Zion Huang
 */
class ApiService {
    private val logger = Logger.getLogger(ApiService::class.java.name)
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
        install(HttpCache)
    }

    suspend fun getImage(imageUrl: String) = runCatching {
        logger.info("Requesting image proxy for URL: $imageUrl")
        client.get {
            url("$BASE_URL/image")
            parameter("url", imageUrl)
        }
    }.onFailure {
        logger.severe("Image proxy request failed: ${it.stackTraceToString()}")
    }

    companion object {
        const val BASE_URL = "https://kizzy-api.cjjdxhdjd.workers.dev"
    }
}

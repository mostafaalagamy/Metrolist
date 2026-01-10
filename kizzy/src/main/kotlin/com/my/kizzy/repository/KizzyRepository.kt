/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * KizzyRepositoryImpl.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.my.kizzy.repository

import com.my.kizzy.remote.ApiService
import com.my.kizzy.remote.ImageProxyResponse
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

/**
 * Modified by Zion Huang
 */
class KizzyRepository {
    private val api = ApiService()

    suspend fun getImage(url: String): String? {
        return try {
            val response = api.getImage(url).getOrNull()
            if (response?.status == HttpStatusCode.OK) {
                response.body<ImageProxyResponse>().id
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

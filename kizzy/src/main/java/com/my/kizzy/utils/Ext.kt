/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * Ext.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.my.kizzy.utils

import com.my.kizzy.remote.ApiResponse
import com.my.kizzy.rpc.RpcImage
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode

suspend fun HttpResponse.toImageAsset(): String? {
    return try {
        if (this.status == HttpStatusCode.OK)
            this.body<ApiResponse>().id
        else
            null
    } catch (e: Exception) {
        null
    }
}

fun String.toRpcImage(): RpcImage? {
    return if (this.isBlank())
        null
    else if (this.startsWith("attachments"))
        RpcImage.DiscordImage(this)
    else
        RpcImage.ExternalImage(this)
}


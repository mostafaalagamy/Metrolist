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

package com.my.kizzy.data.utils

import android.content.Context
import android.graphics.Bitmap
import com.my.kizzy.data.remote.ApiResponse
import com.my.kizzy.data.rpc.RpcImage
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import java.io.File
import java.io.FileOutputStream

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

/**
 * Converts Bitmap to file
 * @param context Context
 * @param outputPathFolder Folder name for storing the png file eg (images, media, custom)
 */
fun Bitmap?.toFile(context: Context, outputPathFolder: String): File {
    val dir = File(context.filesDir.toString() + File.separator + outputPathFolder)
    dir.mkdirs()
    val image = File(dir, "Temp.png")
    FileOutputStream(image).use {
        this?.compress(Bitmap.CompressFormat.PNG, 100, it)
    }
    return image
}

fun String.toRpcImage(): RpcImage? {
    return if (this.isBlank())
        null
    else if (this.startsWith("attachments"))
        RpcImage.DiscordImage(this)
    else
        RpcImage.ExternalImage(this)
}


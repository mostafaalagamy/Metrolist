/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * RpcImage.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.my.kizzy.data.rpc

import android.content.Context
import android.graphics.Bitmap
import com.my.kizzy.data.repository.KizzyRepository
import com.my.kizzy.data.utils.toFile

/**
 * Modified by Zion Huang
 */
sealed class RpcImage {
    abstract suspend fun resolveImage(repository: KizzyRepository): String?

    class DiscordImage(val image: String) : RpcImage() {
        override suspend fun resolveImage(repository: KizzyRepository): String {
            return "mp:${image}"
        }
    }

    class ExternalImage(val image: String) : RpcImage() {
        override suspend fun resolveImage(repository: KizzyRepository): String? {
            return repository.getImage(image)
        }
    }

    class BitmapImage(
        private val context: Context,
        val bitmap: Bitmap?,
        private val packageName: String,
        val title: String,
    ) : RpcImage() {
        override suspend fun resolveImage(repository: KizzyRepository): String? {
            return repository.uploadImage(bitmap.toFile(this.context, "art"))
        }
    }
}

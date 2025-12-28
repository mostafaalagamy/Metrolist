/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.media3.common.util.BitmapLoader
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future

class CoilBitmapLoader(
    private val context: Context,
    private val scope: CoroutineScope,
) : BitmapLoader {
    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        scope.future(Dispatchers.IO) {
            try {
                BitmapFactory.decodeByteArray(data, 0, data.size)
                    ?: createBitmap(64, 64) // Return fallback bitmap instead of throwing error
            } catch (e: Exception) {
                // Handle bitmap decode errors gracefully
                android.util.Log.w("CoilBitmapLoader", "Failed to decode bitmap data", e)
                createBitmap(64, 64) // Return fallback bitmap
            }
        }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        scope.future(Dispatchers.IO) {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
                .build()

            val result = context.imageLoader.execute(request)

            // In case of error, returns an empty bitmap
            when (result) {
                is ErrorResult -> {
                    createBitmap(64, 64)
                }
                is SuccessResult -> {
                    try {
                        result.image.toBitmap()
                    } catch (e: Exception) {
                        createBitmap(64, 64)
                    }
                }
            }
        }
}

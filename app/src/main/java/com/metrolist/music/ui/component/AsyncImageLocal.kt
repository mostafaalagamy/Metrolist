package com.metrolist.music.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

const val MAX_IMAGE_JOBS = 8
@OptIn(ExperimentalCoroutinesApi::class)
val imageSession = Dispatchers.IO.limitedParallelism(MAX_IMAGE_JOBS)

/**
 * Non-blocking image
 */
@Composable
fun AsyncLocalImage(
    image: () -> Bitmap?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
) {
    var imageBitmapState by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(image) {
        CoroutineScope(imageSession).launch {
            try {
                imageBitmapState = image.invoke()?.asImageBitmap()
            } catch (e: Exception) {
//                e.printStackTrace()
                // this probably won't be an issue when debugging...
                // I'd like to add that this WAS a problem when debugging.
            }
        }
    }

    imageBitmapState.let { imageBitmap ->
        if (imageBitmap == null) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = modifier
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(16.dp))
                    .aspectRatio(ratio = 1f)
            )
        } else {
            Image(
                bitmap = imageBitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier.aspectRatio(ratio = 1f),
            )
        }
    }
}

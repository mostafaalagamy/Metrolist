package com.metrolist.music.ui.component.shimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.ThumbnailCornerRadius

@Composable
fun GridItemPlaceHolder(
    modifier: Modifier = Modifier,
    thumbnailShape: Shape = RoundedCornerShape(ThumbnailCornerRadius),
    fillMaxWidth: Boolean = false,
    isPlaylistThumbnail: Boolean = false,
) {
    Column(
        modifier =
        if (fillMaxWidth) {
            modifier
                .padding(12.dp)
                .fillMaxWidth()
        } else {
            modifier
                .padding(12.dp)
                .width(GridThumbnailHeight)
        },
    ) {
        Box(
            modifier =
            if (fillMaxWidth) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.height(GridThumbnailHeight)
            }.aspectRatio(if (isPlaylistThumbnail) 16f / 9f else 1f)
                .clip(thumbnailShape)
        ) {
            // Background fill that simulates cropped content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        TextPlaceholder()

        TextPlaceholder()
    }
}

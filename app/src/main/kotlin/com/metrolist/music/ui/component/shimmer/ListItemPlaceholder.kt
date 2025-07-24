package com.metrolist.music.ui.component.shimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.ThumbnailCornerRadius

@Composable
fun ListItemPlaceHolder(
    modifier: Modifier = Modifier,
    thumbnailShape: Shape = RoundedCornerShape(ThumbnailCornerRadius),
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
        modifier
            .height(ListItemHeight)
            .padding(horizontal = 6.dp),
    ) {
        Box(
            modifier =
            Modifier
                .padding(6.dp)
                .size(ListThumbnailSize)
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

        Column(
            modifier =
            Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
        ) {
            TextPlaceholder()
            TextPlaceholder()
        }
    }
}

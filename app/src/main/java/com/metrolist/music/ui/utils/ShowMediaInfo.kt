package com.metrolist.music.ui.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.MediaInfo
import com.metrolist.music.R
import com.metrolist.music.ui.component.shimmer.GridItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder


@UnstableApi
@Composable
fun ShowMediaInfo(
    videoId: String,
) {

    if (videoId.isBlank()) return

    val windowInsets = WindowInsets.systemBars

    var info by remember {
        mutableStateOf<MediaInfo?>(null)
    }

    LaunchedEffect(Unit, videoId) {
        info = YouTube.getMediaInfo(videoId).getOrNull()
    }

    //if (info == null) return

        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier
                .padding(
                    windowInsets
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                        .asPaddingValues()
                )
                .fillMaxSize()
        ) {
            item(contentType = "TitlePage") {
                Text(
                    text =  stringResource(R.string.information) ,
                    style = TextStyle(
                        fontSize = typography.titleMedium.fontSize,
                        fontWeight = typography.titleMedium.fontWeight,
                        color = typography.titleMedium.color,
                        textAlign = TextAlign.Start
                    )
                )
            }
            if (info != null) {
                item(contentType = "MediaTitle") {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp)
                    ) {
                        Text(
                            text = "" + info?.title,
                            style = TextStyle(
                                fontSize = typography.titleMedium.fontSize,
                                fontWeight = typography.titleMedium.fontWeight,
                                color = typography.titleMedium.color,
                                textAlign = TextAlign.Start
                            )
                        )
                    }
                }
                item(contentType = "MediaAuthor") {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.artists),
                            style = TextStyle(
                                fontSize = typography.titleMedium.fontSize,
                                fontWeight = typography.titleMedium.fontWeight,
                                color = typography.titleMedium.color,
                                textAlign = TextAlign.Start
                            )
                        )
                        BasicText(
                            text = "" + info?.author,
                            style = typography.bodyMedium,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp)
                        )
                    }
                }
                item(contentType = "MediaDescription") {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.description) ,
                            style = TextStyle(
                                fontSize = typography.titleMedium.fontSize,
                                fontWeight = typography.titleMedium.fontWeight,
                                color = typography.titleMedium.color,
                                textAlign = TextAlign.Start
                            )
                        )
                        BasicText(
                            text = info?.description ?: "",
                            style = typography.bodyMedium,
                            modifier = Modifier
                                .padding(all = 16.dp)
                        )
                    }
                }
                item(contentType = "MediaNumbers") {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.numbers),
                            style = TextStyle(
                                fontSize = typography.titleMedium.fontSize,
                                fontWeight = typography.titleMedium.fontWeight,
                                color = typography.titleMedium.color,
                                textAlign = TextAlign.Start
                            )
                        )
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Column {
                                BasicText(
                                    text =stringResource(R.string.subscribers),
                                    style = typography.titleSmall,
                                    modifier = Modifier
                                )
                                BasicText(
                                    text = info?.subscribers ?: "",
                                    style = typography.titleSmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            Column {
                                BasicText(
                                    text = stringResource(R.string.views),
                                    style = typography.titleSmall,
                                    modifier = Modifier
                                )
                                BasicText(
                                    text = "" + info?.viewCount?.toInt()
                                        ?.let { numberFormatter(it) },
                                    style = typography.titleSmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            Column {
                                BasicText(
                                    text =  stringResource(R.string.likes),
                                    style = typography.titleSmall,
                                    modifier = Modifier
                                )
                                BasicText(
                                    text = "" + info?.like?.toInt()?.let { numberFormatter(it) },
                                    style = typography.titleSmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            Column {
                                BasicText(
                                    text = stringResource(R.string.dislikes),
                                    style = typography.titleSmall,
                                    modifier = Modifier
                                )
                                BasicText(
                                    text = "" + info?.dislike?.toInt()?.let { numberFormatter(it) },
                                    style = typography.titleSmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                        }

                    }
                }

            } else {
                item(contentType = "MediaInfoLoader") {
                    ShimmerHost {
                        TextPlaceholder()
                    }
                }
            }
        }


}
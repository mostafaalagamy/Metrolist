package com.malopieds.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicShelfRenderer(
    val title: Runs?,
    val contents: List<Content>?,
    val bottomEndpoint: NavigationEndpoint?,
    val moreContentButton: Button?,
    val continuations: List<Continuation>?,
) {
    @Serializable
    data class Content(
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer,
    )
}

fun List<Continuation>.getContinuation() =
    firstOrNull()?.nextContinuationData?.continuation

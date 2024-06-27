package com.malopieds.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class GridRenderer(
    val header: Header?,
    val items: List<Item>,
) {
    @Serializable
    data class Header(
        val gridHeaderRenderer: GridHeaderRenderer,
    ) {
        @Serializable
        data class GridHeaderRenderer(
            val title: Runs,
        )
    }

    @Serializable
    data class Item(
        val musicNavigationButtonRenderer: MusicNavigationButtonRenderer?,
        val musicTwoRowItemRenderer: MusicTwoRowItemRenderer?,
    )
}

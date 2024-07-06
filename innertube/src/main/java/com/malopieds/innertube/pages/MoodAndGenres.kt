package com.malopieds.innertube.pages

import com.malopieds.innertube.models.BrowseEndpoint
import com.malopieds.innertube.models.GridRenderer
import com.malopieds.innertube.models.MusicNavigationButtonRenderer
import com.malopieds.innertube.models.SectionListRenderer

data class MoodAndGenres(
    val title: String,
    val items: List<Item>,
) {
    data class Item(
        val title: String,
        val stripeColor: Long,
        val endpoint: BrowseEndpoint,
    )

    companion object {
        fun fromSectionListRendererContent(content: SectionListRenderer.Content): MoodAndGenres? {
            return MoodAndGenres(
                title =
                    content.gridRenderer
                        ?.header
                        ?.gridHeaderRenderer
                        ?.title
                        ?.runs
                        ?.firstOrNull()
                        ?.text ?: return null,
                items =
                    content.gridRenderer.items
                        .mapNotNull(GridRenderer.Item::musicNavigationButtonRenderer)
                        .mapNotNull(::fromMusicNavigationButtonRenderer),
            )
        }

        fun fromMusicNavigationButtonRenderer(renderer: MusicNavigationButtonRenderer): Item? {
            return Item(
                title =
                    renderer.buttonText.runs
                        ?.firstOrNull()
                        ?.text ?: return null,
                stripeColor = renderer.solid?.leftStripeColor ?: return null,
                endpoint = renderer.clickCommand.browseEndpoint ?: return null,
            )
        }
    }
}

package com.metrolist.innertube.pages

<<<<<<< HEAD:innertube/src/main/java/com/metrolist/innertube/pages/BrowseResult.kt
import com.metrolist.innertube.models.YTItem
=======
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.filterExplicit
>>>>>>> a3851bbf (feat: option to hide explicit content):innertube/src/main/java/com/metrolist/innertube/pages/BrowseResult.kt

data class BrowseResult(
    val title: String?,
    val items: List<Item>,
) {
    data class Item(
        val title: String?,
        val items: List<YTItem>,
    )

    fun filterExplicit(enabled: Boolean = true) =
        if (enabled) {
            copy(
                items =
                    items.mapNotNull {
                        it.copy(
                            items =
                                it.items
                                    .filterExplicit()
                                    .ifEmpty { return@mapNotNull null },
                        )
                    },
            )
        } else {
            this
        }
}

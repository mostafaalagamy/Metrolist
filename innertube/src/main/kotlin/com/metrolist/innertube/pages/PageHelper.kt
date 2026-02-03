package com.metrolist.innertube.pages

import com.metrolist.innertube.models.Menu
import com.metrolist.innertube.models.MusicResponsiveListItemRenderer.FlexColumn
import com.metrolist.innertube.models.Run

object PageHelper {
    // Icon types for library management (YouTube changed these in Feb 2026)
    // Old icons: LIBRARY_ADD (not in library), LIBRARY_SAVED/LIBRARY_REMOVE (in library)
    // New icons: BOOKMARK_BORDER (not in library), BOOKMARK (in library)
    // Note: KEEP/KEEP_OFF are for "Pin to Listen Again" - different from library!
    private val LIBRARY_ADD_ICONS = setOf("LIBRARY_ADD", "BOOKMARK_BORDER")
    private val LIBRARY_SAVED_ICONS = setOf("LIBRARY_SAVED", "BOOKMARK", "LIBRARY_REMOVE")
    private val ALL_LIBRARY_ICONS = LIBRARY_ADD_ICONS + LIBRARY_SAVED_ICONS
    
    /**
     * Check if an icon type is a library-related icon (for filtering menu items)
     * Excludes KEEP/KEEP_OFF which are for "Pin to Listen Again"
     */
    fun isLibraryIcon(iconType: String?): Boolean {
        if (iconType == null) return false
        // Exclude KEEP/KEEP_OFF (Listen Again pins)
        if (iconType == "KEEP" || iconType == "KEEP_OFF") return false
        return iconType in ALL_LIBRARY_ICONS || iconType.startsWith("LIBRARY_")
    }
    
    fun extractRuns(columns: List<FlexColumn>, typeLike: String): List<Run> {
        val filteredRuns = mutableListOf<Run>()
        for (column in columns) {
            val runs = column.musicResponsiveListItemFlexColumnRenderer.text?.runs
                ?: continue

            for (run in runs) {
                val typeStr = run.navigationEndpoint?.watchEndpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
                    ?: run.navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
                    ?: continue

                if (typeLike in typeStr) {
                    filteredRuns.add(run)
                }
            }
        }
        return filteredRuns
    }

    /**
     * Extract feedback token for library operations.
     * 
     * YouTube's new icon system (Feb 2026):
     * - BOOKMARK_BORDER: Song NOT in library -> defaultToken = ADD, toggledToken = REMOVE
     * - BOOKMARK: Song IS in library -> defaultToken = REMOVE, toggledToken = ADD
     * 
     * @param menu The toggle menu renderer containing the feedback tokens
     * @param type "LIBRARY_ADD" to get the add token, "LIBRARY_REMOVE" to get the remove token
     * @return The appropriate feedback token, or null if not found
     */
    fun extractFeedbackToken(menu: Menu.MenuRenderer.Item.ToggleMenuServiceRenderer?, type: String): String? {
        if (menu == null) return null
        val defaultToken = menu.defaultServiceEndpoint.feedbackEndpoint?.feedbackToken
        val toggledToken = menu.toggledServiceEndpoint?.feedbackEndpoint?.feedbackToken
        val iconType = menu.defaultIcon.iconType

        // Determine if the current icon indicates song is NOT in library
        // BOOKMARK_BORDER or LIBRARY_ADD = song is NOT in library (default action is ADD)
        val songNotInLibrary = iconType in LIBRARY_ADD_ICONS
        
        return when (type) {
            "LIBRARY_ADD" -> {
                // We want the ADD token
                if (songNotInLibrary) {
                    // Icon shows "add" state, default action adds to library
                    defaultToken
                } else {
                    // Icon shows "saved" state, toggled action would add back
                    toggledToken
                }
            }
            "LIBRARY_REMOVE", "LIBRARY_SAVED" -> {
                // We want the REMOVE token
                if (songNotInLibrary) {
                    // Icon shows "add" state, toggled action would remove
                    toggledToken
                } else {
                    // Icon shows "saved" state, default action removes from library
                    defaultToken
                }
            }
            else -> if (iconType == type) defaultToken else toggledToken
        }
    }
}
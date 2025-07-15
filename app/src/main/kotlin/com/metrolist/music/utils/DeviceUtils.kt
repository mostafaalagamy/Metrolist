package com.metrolist.music.utils

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Utilities for detecting device type and screen configuration
 */
object DeviceUtils {
    
    /**
     * Check if the current device is a tablet based on screen size
     * A device is considered a tablet if the smallest width is >= 600dp
     */
    @Composable
    fun isTablet(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.smallestScreenWidthDp >= 600
    }
    
    /**
     * Check if the device is in landscape orientation
     */
    @Composable
    fun isLandscape(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }
    
    /**
     * Check if we should use side navigation
     * Returns true for tablets or phones in landscape with sufficient width
     */
    @Composable
    fun shouldUseSideNavigation(): Boolean {
        val configuration = LocalConfiguration.current
        val isTablet = isTablet()
        val isLandscape = isLandscape()
        
        return isTablet || (isLandscape && configuration.screenWidthDp >= 840)
    }
    
    /**
     * Get the appropriate navigation rail width for the current device
     */
    @Composable
    fun getNavigationRailWidth(): androidx.compose.ui.unit.Dp {
        return if (isTablet()) 80.dp else 72.dp
    }
    
    /**
     * Check if the device is a tablet (non-Composable version)
     */
    fun isTablet(context: Context): Boolean {
        val configuration = context.resources.configuration
        return configuration.smallestScreenWidthDp >= 600
    }
}
package com.dpi

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import kotlin.math.roundToInt

/**
 * Configuration class for adjusting screen density dynamically.
 * Applies density scaling to the entire application and maintains it across activity lifecycle events.
 */
internal class DensityConfiguration(
    private val densityScale: Float
) : ActivityLifecycleManager() {

    private var originalDensityDpi: Int = 0

    /**
     * Applies the density scaling to the application context.
     * This method should be called once during initialization.
     */
    fun applyDensityScaling(context: Context) {
        if (densityScale == 1.0f) return

        try {
            onCreate()
            val resources = context.resources
            val config = Configuration(resources.configuration)
            originalDensityDpi = config.densityDpi
            updateDensityDpi(config, resources)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply configuration", e)
        }
    }

    /**
     * Updates the density DPI in the configuration and applies it to resources.
     */
    private fun updateDensityDpi(config: Configuration, resources: Resources) {
        val newDensityDpi = (originalDensityDpi * densityScale).roundToInt()
        config.densityDpi = newDensityDpi
        Log.i(TAG, "Updated densityDpi to: $newDensityDpi")
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    /**
     * Reapply density scaling when an activity is created.
     */
    override fun onActivityCreated(activity: Activity) {
        applyDensityToActivity(activity)
    }

    /**
     * Reapply density scaling when an activity is resumed.
     */
    override fun onActivityResumed(activity: Activity) {
        applyDensityToActivity(activity)
    }

    /**
     * Reapply density scaling when an activity is started.
     */
    override fun onActivityStarted(activity: Activity) {
        applyDensityToActivity(activity)
    }

    /**
     * Applies the density configuration to a specific activity's resources.
     */
    private fun applyDensityToActivity(activity: Activity) {
        try {
            updateDensityDpi(activity.resources.configuration, activity.resources)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update density for activity", e)
        }
    }

    companion object {
        private val TAG = DensityConfiguration::class.java.simpleName
    }
}

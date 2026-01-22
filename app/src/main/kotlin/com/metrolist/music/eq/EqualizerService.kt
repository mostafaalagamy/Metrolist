package com.metrolist.music.eq


import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.metrolist.music.eq.data.SavedEQProfile
import com.metrolist.music.eq.audio.CustomEqualizerAudioProcessor
import com.metrolist.music.eq.data.ParametricEQ
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing custom EQ using ExoPlayer's AudioProcessor
 * Supports 10+ band Parametric EQ format (APO)
 */
@Singleton
class EqualizerService @Inject constructor() {

    @SuppressLint("UnsafeOptInUsageError")
    private var audioProcessor: CustomEqualizerAudioProcessor? = null
    private var pendingProfile: SavedEQProfile? = null
    private var shouldDisable: Boolean = false

    companion object {
        private const val TAG = "EqualizerService"
    }

    /**
     * Set the audio processor instance
     * This should be called when ExoPlayer is initialized
     */
    @OptIn(UnstableApi::class)
    fun setAudioProcessor(processor: CustomEqualizerAudioProcessor) {
        this.audioProcessor = processor
        Timber.tag(TAG).d("Audio processor set")

        // Apply pending profile if one was set before processor was available
        if (shouldDisable) {
            disable()
            shouldDisable = false
            Timber.tag(TAG).d("Applied pending disable request")
        } else if (pendingProfile != null) {
            val profile = pendingProfile!!
            applyProfile(profile)
            pendingProfile = null
            Timber.tag(TAG).d("Applied pending profile: ${profile.name}")
        }
    }

    /**
     * Apply an EQ profile
     * If audio processor is not set, stores as pending profile
     */
    @OptIn(UnstableApi::class)
    fun applyProfile(profile: SavedEQProfile): Result<Unit> {
        val processor = audioProcessor
        if (processor == null) {
            Timber.tag(TAG)
                .w("Audio processor not set yet. Storing profile as pending: ${profile.name}")
            pendingProfile = profile
            shouldDisable = false
            return Result.success(Unit)
        }

        try {
            // Clear any pending state since we're applying now
            pendingProfile = null
            shouldDisable = false

            // Convert SavedEQProfile to ParametricEQ
            val parametricEQ = ParametricEQ(
                preamp = profile.preamp,
                bands = profile.bands
            )

            processor.applyProfile(parametricEQ)
            Timber.tag(TAG)
                .d("Applied EQ profile: ${profile.name} with ${profile.bands.size} bands and ${profile.preamp} dB preamp")
            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e("Failed to apply profile: ${e.message}")
            return Result.failure(e)
        }
    }

    /**
     * Disable the equalizer (flat response)
     * If audio processor is not set, stores pending disable request
     */
    @OptIn(UnstableApi::class)
    fun disable() {
        val processor = audioProcessor
        if (processor == null) {
            Timber.tag(TAG).w("Audio processor not set yet. Storing disable as pending")
            shouldDisable = true
            pendingProfile = null
            return
        }

        try {
            // Clear any pending state since we're disabling now
            pendingProfile = null
            shouldDisable = false

            processor.disable()
            Timber.tag(TAG).d("Equalizer disabled")
        } catch (e: Exception) {
            Timber.tag(TAG).e("Failed to disable equalizer: ${e.message}")
        }
    }

    /**
     * Check if audio processor is set
     */
    fun isInitialized(): Boolean {
        return audioProcessor != null
    }

    /**
     * Check if equalizer is enabled
     */
    @OptIn(UnstableApi::class)
    fun isEnabled(): Boolean {
        return audioProcessor?.isEnabled() ?: false
    }

    /**
     * Get information about the current EQ capabilities
     */
    fun getEqualizerInfo(): EqualizerInfo {
        return EqualizerInfo(
            supportsUnlimitedBands = true,
            maxBands = Int.MAX_VALUE,
            description = "Custom ExoPlayer AudioProcessor with biquad filters"
        )
    }

    /**
     * Release resources (not needed for AudioProcessor, but kept for API compatibility)
     */
    fun release() {
        // AudioProcessor is managed by ExoPlayer, we just clear our reference
        audioProcessor = null
        Timber.tag(TAG).d("Audio processor reference cleared (pending state preserved)")
    }
}

/**
 * Information about equalizer capabilities
 */
data class EqualizerInfo(
    val supportsUnlimitedBands: Boolean,
    val maxBands: Int,
    val description: String
)

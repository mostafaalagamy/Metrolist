package com.metrolist.music.eq

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.denizen.nanosonic.ui.screens.wizard.databaseUtil.models.ParametricEQ
import com.denizen.nanosonic.ui.screens.wizard.databaseUtil.models.ParametricEQBand
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

/**
 * Custom audio processor for ExoPlayer that applies parametric EQ using biquad filters
 * Uses ParametricEQ format from AutoEQ project
 */
@UnstableApi
class CustomEqualizerAudioProcessor : AudioProcessor {

    private var sampleRate = 0
    private var channelCount = 0
    private var encoding = C.ENCODING_INVALID
    private var isActive = false
    private var equalizerEnabled = false

    private var inputBuffer: ByteBuffer = EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false

    private var filters: List<BiquadFilter> = emptyList()
    private var preampGain: Double = 1.0  // Linear preamp gain multiplier
    private var pendingProfile: ParametricEQ? = null

    companion object {
        private const val TAG = "CustomEqualizerAudioProcessor"
        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }

    /**
     * Apply an EQ profile
     */
    @Synchronized
    fun applyProfile(parametricEQ: ParametricEQ) {
        if (sampleRate == 0) {
            // Audio processor not configured yet, store as pending
            Log.d(TAG, "Audio processor not configured yet. Storing profile as pending with ${parametricEQ.bands.size} bands")
            pendingProfile = parametricEQ
            return
        }

        // Convert preamp from dB to linear gain
        preampGain = 10.0.pow(parametricEQ.preamp / 20.0)

        createFilters(parametricEQ.bands)
        equalizerEnabled = true

        // Reset filter states to ensure clean transition
        filters.forEach { it.reset() }

        Log.d(TAG, "Applied EQ profile with ${filters.size} bands and ${parametricEQ.preamp} dB preamp")
    }

    /**
     * Disable the equalizer
     */
    @Synchronized
    fun disable() {
        equalizerEnabled = false
        filters = emptyList()
        preampGain = 1.0
        pendingProfile = null
        Log.d(TAG, "Equalizer disabled")
    }

    /**
     * Check if equalizer is enabled
     */
    fun isEnabled(): Boolean = equalizerEnabled

    /**
     * Create biquad filters from ParametricEQ bands
     * Only creates filters for enabled bands below Nyquist frequency
     * Supports PK (peaking), LSC (low-shelf), and HSC (high-shelf) filter types
     */
    private fun createFilters(bands: List<ParametricEQBand>) {
        if (sampleRate == 0) {
            Log.w(TAG, "Cannot create filters: sample rate not set")
            return
        }

        // Filter out disabled bands and frequencies above Nyquist limit
        filters = bands
            .filter { it.enabled && it.frequency < sampleRate / 2.0 }
            .map { band ->
                BiquadFilter(
                    sampleRate = sampleRate,
                    frequency = band.frequency,
                    gain = band.gain,
                    q = band.q,
                    filterType = band.filterType
                )
            }

        Log.d(TAG, "Created ${filters.size} biquad filters from ${bands.size} bands (PK/LSC/HSC)")
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding

        Log.d(TAG, "Configured: sampleRate=$sampleRate, channels=$channelCount, encoding=$encoding")

        // Apply pending profile if one exists
        pendingProfile?.let { profile ->
            preampGain = 10.0.pow(profile.preamp / 20.0)
            createFilters(profile.bands)
            equalizerEnabled = true
            pendingProfile = null
            Log.d(TAG, "Applied pending profile with ${filters.size} bands and ${profile.preamp} dB preamp")
        }

        // Only support 16-bit PCM stereo/mono
        if (encoding != C.ENCODING_PCM_16BIT || channelCount > 2) {
            val exception = AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
            throw exception // Rethrow, unsupported
        }

        isActive = true
        return inputAudioFormat
    }

    override fun isActive(): Boolean = isActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!equalizerEnabled || filters.isEmpty()) {
            // Passthrough mode - directly use input as output
            outputBuffer = inputBuffer
            return
        }

        val inputSize = inputBuffer.remaining()
        if (inputSize == 0) {
            return
        }

        // Ensure we have our own output buffer (reuse if possible to avoid allocations)
        if (outputBuffer === EMPTY_BUFFER || outputBuffer === inputBuffer) {
            // Need new buffer - was empty or same as input
            outputBuffer = ByteBuffer.allocateDirect(inputSize).order(ByteOrder.nativeOrder())
        } else if (outputBuffer.capacity() < inputSize) {
            // Need larger buffer
            outputBuffer = ByteBuffer.allocateDirect(inputSize).order(ByteOrder.nativeOrder())
        } else {
            // Reuse existing buffer (most common path)
            outputBuffer.clear()
        }

        outputBuffer.limit(inputSize)

        // Process audio samples
        when (encoding) {
            C.ENCODING_PCM_16BIT -> {
                processAudioBuffer16Bit(inputBuffer, outputBuffer)
            }
            else -> {
                // Unsupported format, passthrough
                outputBuffer.put(inputBuffer)
            }
        }

        outputBuffer.flip()
        inputBuffer.position(inputBuffer.limit())
    }

    /**
     * Process 16-bit PCM audio through all biquad filters
     */
    private fun processAudioBuffer16Bit(input: ByteBuffer, output: ByteBuffer) {
        val sampleCount = input.remaining() / 2 // 2 bytes per 16-bit sample

        repeat(sampleCount / channelCount) {
            when (channelCount) {
                1 -> {
                    // Mono
                    val sample = input.getShort().toDouble() / 32768.0 // Normalize to [-1, 1]
                    var processed = sample

                    // Apply all filters in series
                    for (filter in filters) {
                        processed = filter.processSample(processed)
                    }

                    // Apply preamp gain
                    processed *= preampGain

                    // Clamp and convert back to 16-bit
                    val outputSample = (processed * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort()
                    output.putShort(outputSample)
                }
                2 -> {
                    // Stereo
                    val leftSample = input.getShort().toDouble() / 32768.0
                    val rightSample = input.getShort().toDouble() / 32768.0

                    var processedLeft = leftSample
                    var processedRight = rightSample

                    // Apply all filters in series
                    for (filter in filters) {
                        val (left, right) = filter.processStereo(processedLeft, processedRight)
                        processedLeft = left
                        processedRight = right
                    }

                    // Apply preamp gain
                    processedLeft *= preampGain
                    processedRight *= preampGain

                    // Clamp and convert back to 16-bit
                    val outputLeft = (processedLeft * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort()
                    val outputRight = (processedRight * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort()

                    output.putShort(outputLeft)
                    output.putShort(outputRight)
                }
                else -> {
                    // Should not happen as configure rejects > 2 channels
                    repeat(channelCount) {
                        output.putShort(input.getShort())
                    }
                }
            }
        }
    }

    override fun getOutput(): ByteBuffer {
        return outputBuffer
    }

    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer.remaining() == 0
    }

    @Deprecated("Deprecated in Java")
    override fun flush() {
        outputBuffer = EMPTY_BUFFER
        inputEnded = false

        // Reset filter states
        filters.forEach { it.reset() }
    }

    override fun reset() {
        flush()
        inputBuffer = EMPTY_BUFFER
        sampleRate = 0
        channelCount = 0
        encoding = C.ENCODING_INVALID
        isActive = false
        filters.forEach { it.reset() }
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }
}
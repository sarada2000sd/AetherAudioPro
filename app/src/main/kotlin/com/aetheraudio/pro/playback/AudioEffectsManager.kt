package com.aetheraudio.pro.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Real, functional DSP chain attached to ExoPlayer's audio session, using Android's built-in
 * AudioEffect framework (android.media.audiofx). This is genuinely active on-device processing —
 * not a mock. Band count and frequency range are hardware/driver-dependent (most devices expose
 * 5-6 bands); the EQ screen renders whatever the platform reports and lets the user drag a
 * touch-curve across them.
 *
 * NOTE ON SCOPE: the spec's 10/32-band *parametric* EQ, real-time BPM detection, and
 * pitch-preserving tempo stretch require a custom native (C++/FFmpeg, phase-vocoder) DSP engine
 * processing raw PCM before it reaches the sink -- a substantial standalone effort (NDK build,
 * JNI bridge, real-time buffer routing). That's intentionally left as a follow-up module; see
 * NativeDspEngine below for the seam it would plug into.
 */
object AudioEffectsManager {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var presetReverb: PresetReverb? = null

    private val _bandLevels = MutableStateFlow<List<Short>>(emptyList())
    val bandLevels: StateFlow<List<Short>> = _bandLevels

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled

    val bandCount: Int get() = equalizer?.numberOfBands?.toInt() ?: 0
    fun bandLevelRange(): IntRange {
        val range = equalizer?.bandLevelRange ?: return -1500..1500
        return range[0].toInt()..range[1].toInt()
    }
    fun centerFrequencyHz(band: Int): Int = (equalizer?.getCenterFreq(band.toShort()) ?: 0) / 1000

    fun attach(audioSessionId: Int) {
        release()
        equalizer = Equalizer(0, audioSessionId).apply {
            enabled = true
            _bandLevels.value = (0 until numberOfBands).map { getBandLevel(it.toShort()) }
        }
        bassBoost = BassBoost(0, audioSessionId).apply { enabled = false }
        presetReverb = PresetReverb(0, audioSessionId).apply { enabled = false }
    }

    fun setEnabled(on: Boolean) {
        _enabled.value = on
        equalizer?.enabled = on
    }

    fun setBandLevel(band: Int, levelMillibel: Short) {
        equalizer?.setBandLevel(band.toShort(), levelMillibel)
        _bandLevels.value = _bandLevels.value.toMutableList().also {
            if (band < it.size) it[band] = levelMillibel
        }
    }

    fun applyPreset(preset: EqPreset) {
        val eq = equalizer ?: return
        val range = bandLevelRange()
        for (band in 0 until eq.numberOfBands.toInt()) {
            val level = preset.curve.getOrElse(band) { 0f }
            val mb = (level * range.last).toInt().toShort()
            eq.setBandLevel(band.toShort(), mb)
        }
        _bandLevels.value = (0 until eq.numberOfBands).map { eq.getBandLevel(it.toShort()) }
    }

    fun setBassBoost(strength0to1000: Short) {
        bassBoost?.apply {
            enabled = strength0to1000 > 0
            setStrength(strength0to1000)
        }
    }

    fun setReverb(preset: Short) {
        presetReverb?.apply {
            enabled = preset != PresetReverb.PRESET_NONE
            this.preset = preset
        }
    }

    fun release() {
        equalizer?.release(); equalizer = null
        bassBoost?.release(); bassBoost = null
        presetReverb?.release(); presetReverb = null
    }
}

/** Curve values are -1f..1f fractions of the device's max band-level range. */
enum class EqPreset(val label: String, val curve: List<Float>) {
    FLAT("Flat", listOf(0f, 0f, 0f, 0f, 0f, 0f)),
    BASS("Bass", listOf(0.8f, 0.6f, 0.2f, -0.1f, -0.1f, 0f)),
    VOCAL("Vocal", listOf(-0.2f, 0f, 0.3f, 0.5f, 0.3f, 0f)),
    ROCK("Rock", listOf(0.5f, 0.3f, -0.1f, 0f, 0.3f, 0.5f)),
    CLASSICAL("Classical", listOf(0.3f, 0.2f, 0f, 0f, 0.1f, 0.3f)),
    CUSTOM("Custom", emptyList())
}

/**
 * Seam for the future native DSP module (BPM detection, pitch-preserving tempo stretch,
 * true multi-band parametric EQ via FFmpeg). Not implemented here -- see README "Roadmap".
 */
interface NativeDspEngine {
    fun setTempo(percent: Float) // 50f..200f, no pitch change
    fun setPitchSemitones(semitones: Float) // -12f..12f, no tempo change
    fun detectedBpm(): Float?
}

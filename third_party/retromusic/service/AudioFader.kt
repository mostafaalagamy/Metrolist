package code.name.monkey.retromusic.service

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.media.MediaPlayer
import android.provider.Settings
import androidx.core.animation.doOnEnd
import code.name.monkey.retromusic.service.playback.Playback
import code.name.monkey.retromusic.util.PreferenceUtil

class AudioFader {
    companion object {
        fun createFadeAnimator(
            context: Context,
            fadeInMp: MediaPlayer,
            fadeOutMp: MediaPlayer,
            endAction: (animator: Animator) -> Unit,
        ): Animator? {
            val animScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            val duration = (PreferenceUtil.crossFadeDuration * 1000 ) / animScale
            if (duration == 0F) {
                return null
            }
            return ValueAnimator.ofFloat(0f, 1f).apply {
                this.duration = duration.toLong()
                addUpdateListener { animation: ValueAnimator ->
                    fadeInMp.setVolume(
                        animation.animatedValue as Float, animation.animatedValue as Float
                    )
                    fadeOutMp.setVolume(1 - animation.animatedValue as Float,
                        1 - animation.animatedValue as Float)
                }
                doOnEnd {
                    endAction(it)
                }
            }
        }

        fun startFadeAnimator(
            playback: Playback,
            fadeIn: Boolean,
            callback: Runnable? = null,
        ) {
            val duration = PreferenceUtil.audioFadeDuration.toLong()
            if (duration == 0L) {
                callback?.run()
                return
            }
            val startValue = if (fadeIn) 0f else 1.0f
            val endValue = if (fadeIn) 1.0f else 0f
            val animator = ValueAnimator.ofFloat(startValue, endValue)
            animator.duration = duration
            animator.addUpdateListener { animation: ValueAnimator ->
                playback.setVolume(animation.animatedValue as Float)
            }
            animator.doOnEnd {
                callback?.run()
            }
            animator.start()
        }
    }
}


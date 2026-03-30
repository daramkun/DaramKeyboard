package com.daram.keyboard.feedback

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

class HapticFeedbackManager(private val context: Context) {
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    fun performKeyClick(intensity: String = "medium") {
        if (!vibrator.hasVibrator()) return
        val effect = when (intensity) {
            "weak"   -> VibrationEffect.createOneShot(10, 60)
            "strong" -> VibrationEffect.createOneShot(20, 255)
            else     -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        }
        vibrator.vibrate(effect)
    }
}

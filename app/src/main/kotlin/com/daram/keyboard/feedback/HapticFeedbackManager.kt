package com.daram.keyboard.feedback

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

class HapticFeedbackManager(private val context: Context) {
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    fun performKeyClick() {
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        )
    }
}

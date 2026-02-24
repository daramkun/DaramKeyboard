package com.daram.keyboard.feedback

import android.content.Context
import android.media.AudioManager

class SoundFeedbackManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun init() { /* 시스템 사운드 사용 — 별도 초기화 불필요 */ }

    fun playKeyClick() {
        audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1f)
    }

    fun release() { /* 해제할 리소스 없음 */ }
}

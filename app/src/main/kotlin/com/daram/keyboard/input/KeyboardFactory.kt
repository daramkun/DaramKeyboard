package com.daram.keyboard.input

import com.daram.keyboard.layout.CheonjiinLayout
import com.daram.keyboard.layout.DubeolsikLayout
import com.daram.keyboard.layout.KeyboardLayout
import com.daram.keyboard.layout.MotorolaLayout
import com.daram.keyboard.layout.NaratgulLayout
import com.daram.keyboard.layout.SkyIILayout
import com.daram.keyboard.model.KoreanKeyboardType
import `in`.daram.nutcracker.automata.CheonjiinAutomata
import `in`.daram.nutcracker.automata.DanmoemAutomata
import `in`.daram.nutcracker.automata.DubeolsikAutomata
import `in`.daram.nutcracker.automata.MotorolaAutomata
import `in`.daram.nutcracker.automata.NaratgeulAutomata
import `in`.daram.nutcracker.automata.SkyIIAutomata

object KeyboardFactory {

    /** 한국어 키보드 타입에 맞는 레이아웃과 입력 엔진 쌍을 반환 */
    fun createKorean(type: KoreanKeyboardType): Pair<KeyboardLayout, HangulInputEngine> {
        return when (type) {
            KoreanKeyboardType.NARATGEUL -> NaratgulLayout to HangulInputEngine(NaratgeulAutomata())
            KoreanKeyboardType.CHEONJIIN -> CheonjiinLayout to HangulInputEngine(CheonjiinAutomata())
            KoreanKeyboardType.SKY_II    -> SkyIILayout to HangulInputEngine(SkyIIAutomata())
            KoreanKeyboardType.MOTOROLA  -> MotorolaLayout to HangulInputEngine(MotorolaAutomata())
            KoreanKeyboardType.DANMOEUM  -> DubeolsikLayout to HangulInputEngine(DanmoemAutomata(), hasShift = true)
            KoreanKeyboardType.DUBEOLSIK -> DubeolsikLayout to HangulInputEngine(DubeolsikAutomata(), hasShift = true)
        }
    }
}

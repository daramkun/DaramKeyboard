package com.daram.keyboard.input

import com.daram.keyboard.layout.CheonjiinLayout
import com.daram.keyboard.layout.DanmoemLayout
import com.daram.keyboard.layout.DubeolsikLayout
import com.daram.keyboard.layout.KeyboardLayout
import com.daram.keyboard.layout.MotorolaLayout
import com.daram.keyboard.layout.Mue128Layout
import com.daram.keyboard.layout.NaratgulLayout
import com.daram.keyboard.layout.SkyIILayout
import com.daram.keyboard.model.KoreanKeyboardType
import com.daram.nutcracker.automata.CheonjiinAutomata
import com.daram.nutcracker.automata.DanmoemAutomata
import com.daram.nutcracker.automata.DubeolsikAutomata
import com.daram.nutcracker.automata.MotorolaAutomata
import com.daram.nutcracker.automata.Mue128Automata
import com.daram.nutcracker.automata.NaratgeulAutomata
import com.daram.nutcracker.automata.SkyIIAutomata

object KeyboardFactory {

    /** 한국어 키보드 타입에 맞는 레이아웃과 입력 엔진 쌍을 반환 */
    fun createKorean(type: KoreanKeyboardType): Pair<KeyboardLayout, HangulInputEngine> {
        return when (type) {
            KoreanKeyboardType.NARATGEUL -> NaratgulLayout to HangulInputEngine(NaratgeulAutomata())
            KoreanKeyboardType.CHEONJIIN -> CheonjiinLayout to HangulInputEngine(CheonjiinAutomata())
            KoreanKeyboardType.SKY_II    -> SkyIILayout to HangulInputEngine(SkyIIAutomata())
            KoreanKeyboardType.MOTOROLA  -> MotorolaLayout to HangulInputEngine(MotorolaAutomata())
            KoreanKeyboardType.DANMOEUM  -> DanmoemLayout to HangulInputEngine(DanmoemAutomata())
            KoreanKeyboardType.DUBEOLSIK -> DubeolsikLayout to HangulInputEngine(DubeolsikAutomata(), hasShift = true)
            KoreanKeyboardType.MUE128    -> Mue128Layout to HangulInputEngine(Mue128Automata())
        }
    }
}

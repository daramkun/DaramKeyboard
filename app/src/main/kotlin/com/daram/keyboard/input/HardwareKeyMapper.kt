package com.daram.keyboard.input

import android.view.KeyEvent
import com.daram.keyboard.model.KeyAction

/**
 * 하드웨어 키보드 키코드를 KeyAction으로 변환하는 인터페이스.
 *
 * @param keyCode  Android KeyEvent 키코드
 * @param event    키 이벤트 (metaState, isShiftPressed 등 참조용)
 * @param shiftState 현재 엔진의 소프트 Shift 상태 (필요 시 참고)
 * @return 대응하는 KeyAction, 처리 불가 시 null
 */
interface HardwareKeyMapper {
    fun mapKeyCode(keyCode: Int, event: KeyEvent, shiftState: ShiftState): KeyAction?
}

package com.daram.keyboard.layout

import com.daram.keyboard.model.Key

/** 보조 레이블(숫자 힌트 등)의 표시 위치 */
enum class SecondaryLabelAlignment {
    /** QWERTY 스타일: 왼쪽 상단 */
    TOP_LEFT,
    /** 나랏글/숫자 스타일: 오른쪽 하단 */
    BOTTOM_RIGHT
}

interface KeyboardLayout {
    val rows: List<List<Key>>

    /**
     * 각 행의 시작 x 위치 (전체 너비에 대한 비율 0.0~1.0). 기본값 0.
     * 예) QWERTY 행2를 행1 키 너비의 절반만큼 들여쓰려면 0.05 (=0.5/10)
     */
    val rowStartXRatios: List<Float> get() = List(rows.size) { 0f }

    /**
     * 보조(숫자/기호/이모지) 레이아웃 여부.
     * true이면 레이아웃 전환 시 prevLayout을 갱신하지 않음.
     */
    val isAuxiliary: Boolean get() = false

    /**
     * 보조 레이블(secondaryLabel) 표시 위치.
     * 나랏글·숫자 계열은 BOTTOM_RIGHT, QWERTY 계열은 TOP_LEFT.
     */
    val secondaryLabelAlignment: SecondaryLabelAlignment
        get() = SecondaryLabelAlignment.TOP_LEFT

    /**
     * ReturnToPrev 버튼에 표시할 레이블.
     * QWERTY 계열 레이아웃에서 보조 레이아웃(기호 등)을 열었다가 돌아올 때 표시되는 문자열.
     */
    val returnButtonLabel: String get() = "한"
}

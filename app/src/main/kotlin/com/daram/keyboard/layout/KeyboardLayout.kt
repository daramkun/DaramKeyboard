package com.daram.keyboard.layout

import com.daram.keyboard.model.Key

interface KeyboardLayout {
    val rows: List<List<Key>>
    /**
     * 각 행의 시작 x 위치 (전체 너비에 대한 비율 0.0~1.0). 기본값 0.
     * 예) QWERTY 행2를 행1 키 너비의 절반만큼 들여쓰려면 0.05 (=0.5/10)
     */
    val rowStartXRatios: List<Float> get() = List(rows.size) { 0f }
}

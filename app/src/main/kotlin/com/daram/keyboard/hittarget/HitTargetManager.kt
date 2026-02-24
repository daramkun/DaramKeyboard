package com.daram.keyboard.hittarget

import android.graphics.PointF
import android.graphics.RectF
import com.daram.keyboard.model.Key

/**
 * 동적 히트 타겟 관리자.
 *
 * WP8 방식: 각 키마다 시각적 RectF와 실제 히트 RectF를 분리 관리.
 * 터치 히스토리 및 키 사용 빈도에 따라 히트 영역을 동적으로 조정.
 *
 * 자소 단위 가중치: 같은 키라도 실제 입력된 자소(jamo)에 따라 별도 가중치를 관리.
 * 예) ㄱ 키에서 초성 'ㄱ'과 종성 후보 'ㄱ', 쌍자음 'ㄲ'은 각각 다른 컨텍스트이므로
 * 자소별로 세분화된 빈도를 누적하여 히트 영역 조정에 반영.
 */
class HitTargetManager {
    private val visualRects = mutableMapOf<String, RectF>()
    private val hitRects = mutableMapOf<String, RectF>()
    private val keyFrequencyWeights = mutableMapOf<String, Float>()
    /** 자소 단위 빈도 가중치: 키 "$keyId:$jamo" 형태로 저장 */
    private val jamoFrequencyWeights = mutableMapOf<String, Float>()
    private val touchHistory = ArrayDeque<PointF>(20)
    private var keyList: List<Key> = emptyList()

    /**
     * 다음 입력 예측 힌트: keyId → 추가 확장 가중치 (0.0 ~ 1.0).
     * 예측 엔진이 계산한 "다음에 눌릴 가능성"을 히트 영역 확장에 반영.
     * setNextKeyHints()로 갱신되며, recordTouch() 후 자동으로 재계산에 반영됨.
     */
    private val nextKeyHints = mutableMapOf<String, Float>()

    fun setKeyRects(keys: List<Key>, rects: Map<String, RectF>) {
        keyList = keys
        visualRects.clear()
        visualRects.putAll(rects)
        recalculateHitTargets()
    }

    /**
     * 예측 엔진으로부터 다음 키 힌트를 받아 히트 영역을 즉시 재계산.
     *
     * @param hints keyId → 예측 가중치 (0.0 이상). 높을수록 히트 영역을 더 넓힘.
     *              빈 맵을 전달하면 예측 힌트 없이 빈도 기반만 사용.
     */
    fun setNextKeyHints(hints: Map<String, Float>) {
        nextKeyHints.clear()
        nextKeyHints.putAll(hints)
        recalculateHitTargets()
    }

    /**
     * 주어진 좌표에서 가장 적합한 키를 반환.
     * 히트 RectF에 포함된 키가 없으면 가장 가까운 키의 중심점 기준으로 반환.
     */
    fun findKeyAt(x: Float, y: Float): Key? {
        val candidates = keyList.filter { key ->
            hitRects[key.id]?.contains(x, y) == true
        }

        return when {
            candidates.isEmpty() -> {
                // 히트 영역 밖이면 중심점 기준 최근접 키 반환
                keyList.minByOrNull { key ->
                    val rect = visualRects[key.id] ?: return@minByOrNull Float.MAX_VALUE
                    val cx = rect.centerX()
                    val cy = rect.centerY()
                    (x - cx) * (x - cx) + (y - cy) * (y - cy)
                }
            }
            candidates.size == 1 -> candidates.first()
            else -> {
                // 여러 키의 히트 영역이 겹치면 중심점 거리가 짧은 키 선택
                candidates.minByOrNull { key ->
                    val rect = visualRects[key.id] ?: return@minByOrNull Float.MAX_VALUE
                    val cx = rect.centerX()
                    val cy = rect.centerY()
                    (x - cx) * (x - cx) + (y - cy) * (y - cy)
                }
            }
        }
    }

    /**
     * 터치 기록 및 키 사용 빈도 업데이트 후 히트 타겟 재계산.
     *
     * @param jamo 실제로 입력된 자소 문자. null이면 자소 단위 추적을 하지 않음
     *             (QWERTY, 기호, 기능 키 등 한글 외 키에서는 null로 전달).
     */
    fun recordTouch(x: Float, y: Float, key: Key, jamo: Char? = null) {
        touchHistory.addLast(PointF(x, y))
        if (touchHistory.size > 20) touchHistory.removeFirst()

        // 키 단위 가중치 업데이트 (EMA)
        val current = keyFrequencyWeights.getOrDefault(key.id, 1f)
        keyFrequencyWeights[key.id] = (current * 0.95f + 0.05f * 2f).coerceIn(0.5f, 3f)

        // 다른 키들의 가중치는 서서히 감소
        for ((id, w) in keyFrequencyWeights) {
            if (id != key.id) {
                keyFrequencyWeights[id] = (w * 0.99f).coerceAtLeast(0.5f)
            }
        }

        // 자소 단위 가중치 업데이트: 자소가 제공된 경우에만 적용
        if (jamo != null) {
            val jamoKey = "${key.id}:$jamo"
            val jamoWeight = jamoFrequencyWeights.getOrDefault(jamoKey, 1f)
            jamoFrequencyWeights[jamoKey] = (jamoWeight * 0.95f + 0.05f * 2f).coerceIn(0.5f, 3f)

            // 같은 키의 다른 자소 가중치는 서서히 감소
            for ((k, w) in jamoFrequencyWeights) {
                if (k.startsWith("${key.id}:") && k != jamoKey) {
                    jamoFrequencyWeights[k] = (w * 0.99f).coerceAtLeast(0.5f)
                }
            }
        }

        recalculateHitTargets()
    }

    /**
     * 특정 키에서 가장 많이 입력된 자소의 가중치를 반환.
     * 자소 단위 가중치 데이터가 없으면 키 단위 가중치를 그대로 사용.
     */
    private fun resolvedWeight(keyId: String): Float {
        val jamoEntries = jamoFrequencyWeights.entries.filter { it.key.startsWith("$keyId:") }
        // 자소 단위 기록이 없으면 키 단위 가중치 사용
        if (jamoEntries.isEmpty()) return keyFrequencyWeights.getOrDefault(keyId, 1f)
        // 자소 단위 가중치 중 최댓값을 반영 (가장 자주 입력된 자소 기준)
        val maxJamoWeight = jamoEntries.maxOf { it.value }
        val keyWeight = keyFrequencyWeights.getOrDefault(keyId, 1f)
        // 키 가중치와 자소 최대 가중치를 평균하여 반영
        return (keyWeight + maxJamoWeight) / 2f
    }

    private fun recalculateHitTargets() {
        hitRects.clear()

        val centers = visualRects.mapValues { (_, rect) ->
            PointF(rect.centerX(), rect.centerY())
        }

        // 터치 방향 벡터 계산 (최근 터치 히스토리 기반)
        val directionBias = computeDirectionBias()

        for (key in keyList) {
            val visualRect = visualRects[key.id] ?: continue
            val center = centers[key.id] ?: continue
            val weight = resolvedWeight(key.id)

            val baseExpansion = 8f  // 기본 확장 픽셀
            val freqExpansion = (weight - 1f) * 6f  // 빈도 기반 추가 확장
            // 예측 힌트 기반 추가 확장: 힌트 가중치 × 최대 10px
            val hintExpansion = (nextKeyHints[key.id] ?: 0f) * 10f

            val hitRect = RectF(visualRect)

            // 방향 편향 적용 (터치 히스토리 방향으로 히트 영역 확장)
            val biasX = directionBias.x * 4f
            val biasY = directionBias.y * 4f

            val totalExpansion = baseExpansion + freqExpansion + hintExpansion
            hitRect.left -= totalExpansion - biasX
            hitRect.top -= totalExpansion - biasY
            hitRect.right += totalExpansion + biasX
            hitRect.bottom += totalExpansion + biasY

            hitRects[key.id] = hitRect
        }

        // Voronoi 분할: 인접 키 중심점 사이의 수직이등분선으로 히트 경계 클리핑
        applyVoronoiClipping(centers)
    }

    private fun applyVoronoiClipping(centers: Map<String, PointF>) {
        val keyIds = keyList.map { it.id }

        for (i in keyIds.indices) {
            val idA = keyIds[i]
            val centerA = centers[idA] ?: continue
            val hitA = hitRects[idA] ?: continue

            for (j in i + 1 until keyIds.size) {
                val idB = keyIds[j]
                val centerB = centers[idB] ?: continue
                val hitB = hitRects[idB] ?: continue

                // 두 키의 히트 영역이 겹치는지 확인
                if (!RectF.intersects(hitA, hitB)) continue

                // 중점
                val midX = (centerA.x + centerB.x) / 2f
                val midY = (centerA.y + centerB.y) / 2f

                // 방향 벡터 (A→B)
                val dx = centerB.x - centerA.x
                val dy = centerB.y - centerA.y

                // 수직이등분선 방향이 수평에 가까우면 Y 기준으로 분할, 아니면 X 기준
                if (Math.abs(dx) > Math.abs(dy)) {
                    // 수직선으로 분할: A가 왼쪽이면 A의 right와 B의 left를 midX로
                    if (centerA.x < centerB.x) {
                        hitA.right = hitA.right.coerceAtMost(midX)
                        hitB.left = hitB.left.coerceAtLeast(midX)
                    } else {
                        hitA.left = hitA.left.coerceAtLeast(midX)
                        hitB.right = hitB.right.coerceAtMost(midX)
                    }
                } else {
                    // 수평선으로 분할
                    if (centerA.y < centerB.y) {
                        hitA.bottom = hitA.bottom.coerceAtMost(midY)
                        hitB.top = hitB.top.coerceAtLeast(midY)
                    } else {
                        hitA.top = hitA.top.coerceAtLeast(midY)
                        hitB.bottom = hitB.bottom.coerceAtMost(midY)
                    }
                }
            }
        }
    }

    private fun computeDirectionBias(): PointF {
        if (touchHistory.size < 2) return PointF(0f, 0f)
        var dx = 0f
        var dy = 0f
        for (i in 1 until touchHistory.size) {
            dx += touchHistory[i].x - touchHistory[i - 1].x
            dy += touchHistory[i].y - touchHistory[i - 1].y
        }
        val count = (touchHistory.size - 1).toFloat()
        val avgDx = dx / count
        val avgDy = dy / count
        val len = Math.sqrt((avgDx * avgDx + avgDy * avgDy).toDouble()).toFloat()
        return if (len > 0) PointF(avgDx / len, avgDy / len) else PointF(0f, 0f)
    }

    fun getVisualRect(keyId: String): RectF? = visualRects[keyId]
    fun getHitRect(keyId: String): RectF? = hitRects[keyId]
}


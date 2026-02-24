package com.daram.keyboard.input

/**
 * 나랏글 방식 한글 자모 조합 엔진.
 *
 * ─────────────────────────────────────────
 * 나랏글 키 구성 (레퍼런스 이미지 기준)
 * ─────────────────────────────────────────
 *  ㄱ 키  : ㄱ  (획추가→ㅋ, 쌍자음→ㄲ)
 *  ㄴ 키  : ㄴ  (획추가→ㄷ→ㅌ, 쌍자음 없음)
 *  ㄹ 키  : ㄹ  (획추가/쌍자음 없음)
 *  ㅁ 키  : ㅁ  (획추가→ㅂ→ㅍ, 쌍자음→ㅃ)
 *  ㅅ 키  : ㅅ  (획추가→ㅈ→ㅊ, 쌍자음→ㅆ)
 *  ㅇ 키  : ㅇ  (획추가→ㅎ, 쌍자음 없음)
 *  ㅏㅓ 키: 첫 탭→ㅏ, 재탭→ㅓ (ㅗ+ㅏ=ㅘ, ㅜ+ㅓ=ㅝ 등 복합모음 포함)
 *  ㅗㅜ 키: 첫 탭→ㅗ, 재탭→ㅜ
 *  ㅣ 키  : ㅣ  (ㅗ+ㅣ=ㅚ, ㅜ+ㅣ=ㅟ, ㅡ+ㅣ=ㅢ 등)
 *
 * ─────────────────────────────────────────
 * 상태 머신
 * ─────────────────────────────────────────
 *  EMPTY
 *    ─(자음)→ CHOSEONG
 *    ─(모음)→ JUNGSEONG_ONLY
 *
 *  CHOSEONG
 *    ─(모음)→ CHO_JUNG
 *    ─(자음)→ 이전 초성 확정 commit, CHOSEONG(새 자음)
 *
 *  JUNGSEONG_ONLY
 *    ─(같은 모음 키 재탭)→ 모음 교체(순환), JUNGSEONG_ONLY
 *    ─(복합모음 형성 가능)→ 모음 합성, JUNGSEONG_ONLY
 *    ─(그 외 모음)→ 이전 모음 확정 commit, JUNGSEONG_ONLY(새 모음)
 *    ─(자음)→ 이전 모음 확정 commit, CHOSEONG(새 자음)
 *
 *  CHO_JUNG
 *    ─(같은 모음 키 재탭)→ 중성 교체(순환), CHO_JUNG
 *    ─(복합모음 형성 가능)→ 중성 합성, CHO_JUNG
 *    ─(그 외 모음)→ 현 음절 확정 commit, JUNGSEONG_ONLY(새 모음)
 *    ─(자음)→ CHO_JUNG_JONG_CANDIDATE(종성 후보)
 *
 *  CHO_JUNG_JONG_CANDIDATE
 *    ─(모음)→ 종성을 다음 초성으로 분리, 현 음절 확정 commit, CHO_JUNG(새 음절)
 *             이중받침이면 앞 자음을 종성으로 남기고 뒤 자음이 다음 초성
 *    ─(자음, 이중받침 가능)→ 이중받침으로 합성, CHO_JUNG_JONG_CANDIDATE
 *    ─(자음, 이중받침 불가)→ 현 음절 확정 commit, CHOSEONG(새 자음)
 *
 * ─────────────────────────────────────────
 * 획추가/쌍자음은 현재 조합 중인 자음에만 적용
 * ─────────────────────────────────────────
 *  획추가: CHOSEONG, CHO_JUNG_JONG_CANDIDATE 상태에서만 동작
 *  쌍자음: CHOSEONG, CHO_JUNG_JONG_CANDIDATE 상태에서만 동작
 *
 * ─────────────────────────────────────────
 * Backspace 동작
 * ─────────────────────────────────────────
 *  CHOSEONG              → EMPTY
 *  JUNGSEONG_ONLY(단일)  → EMPTY
 *  JUNGSEONG_ONLY(복합)  → 복합모음 분해 (ㅘ→ㅗ, ㅝ→ㅜ, ...)
 *  CHO_JUNG(단일 중성)   → CHOSEONG
 *  CHO_JUNG(복합 중성)   → 복합모음 분해 후 CHO_JUNG
 *  CHO_JUNG_JONG_CANDIDATE(단일 종성) → CHO_JUNG
 *  CHO_JUNG_JONG_CANDIDATE(이중받침)  → 이중받침 분해 (앞 자음만 종성으로)
 */
class HangulComposer {

    // ── 유니코드 자모 테이블 ──────────────────────────────────────────
    private val choseongList = charArrayOf(
        'ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ','ㅅ',
        'ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
    )
    private val jungseongList = charArrayOf(
        'ㅏ','ㅐ','ㅑ','ㅒ','ㅓ','ㅔ','ㅕ','ㅖ','ㅗ',
        'ㅘ','ㅙ','ㅚ','ㅛ','ㅜ','ㅝ','ㅞ','ㅟ','ㅠ','ㅡ','ㅢ','ㅣ'
    )
    private val jongseongList = charArrayOf(
        '\u0000','ㄱ','ㄲ','ㄳ','ㄴ','ㄵ','ㄶ','ㄷ','ㄹ','ㄺ',
        'ㄻ','ㄼ','ㄽ','ㄾ','ㄿ','ㅀ','ㅁ','ㅂ','ㅄ','ㅅ',
        'ㅆ','ㅇ','ㅈ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
    )

    // ── 획추가 매핑 ──────────────────────────────────────────────────
    // 자음: ㄱ→ㅋ, ㄴ→ㄷ→ㅌ, ㅁ→ㅂ→ㅍ, ㅅ→ㅈ→ㅊ, ㅇ→ㅎ
    // 모음: ㅏ→ㅑ, ㅓ→ㅕ, ㅗ→ㅛ, ㅜ→ㅠ (단모음에서 이중모음으로)
    private val strokeMap = mapOf(
        'ㄱ' to 'ㅋ',
        'ㄴ' to 'ㄷ', 'ㄷ' to 'ㅌ',
        'ㅁ' to 'ㅂ', 'ㅂ' to 'ㅍ',
        'ㅅ' to 'ㅈ', 'ㅈ' to 'ㅊ',
        'ㅇ' to 'ㅎ',
        'ㅏ' to 'ㅑ', 'ㅓ' to 'ㅕ',
        'ㅗ' to 'ㅛ', 'ㅜ' to 'ㅠ'
    )

    // ── 쌍자음 매핑 (토글) ───────────────────────────────────────────
    private val doubleMap = mapOf(
        'ㄱ' to 'ㄲ', 'ㄷ' to 'ㄸ', 'ㅂ' to 'ㅃ',
        'ㅅ' to 'ㅆ', 'ㅈ' to 'ㅉ'
    )
    private val doubleReverseMap = doubleMap.entries.associate { (k, v) -> v to k }

    // ── 복합 모음 합성 ───────────────────────────────────────────────
    // 나랏글에서 ㅣ 키는 선행 모음과 합성하여 ㅐ/ㅔ/ㅚ/ㅟ/ㅢ/ㅙ/ㅞ 생성
    private val compoundVowelMap = mapOf(
        Pair('ㅏ', 'ㅣ') to 'ㅐ',
        Pair('ㅑ', 'ㅣ') to 'ㅒ',
        Pair('ㅓ', 'ㅣ') to 'ㅔ',
        Pair('ㅕ', 'ㅣ') to 'ㅖ',
        Pair('ㅗ', 'ㅏ') to 'ㅘ',
        Pair('ㅗ', 'ㅣ') to 'ㅚ',
        Pair('ㅘ', 'ㅣ') to 'ㅙ',
        Pair('ㅜ', 'ㅏ') to 'ㅝ',   // ㅏ 키 첫 탭이 ㅜ 뒤에서 ㅝ로 합성
        Pair('ㅜ', 'ㅣ') to 'ㅟ',
        Pair('ㅝ', 'ㅣ') to 'ㅞ',
        Pair('ㅡ', 'ㅣ') to 'ㅢ'
    )

    // 복합 모음 분해 (backspace용): 복합모음 → 한 단계 이전 모음
    // ㅐ→ㅏ, ㅔ→ㅓ, ㅘ→ㅗ, ㅚ→ㅗ, ㅙ→ㅘ, ㅝ→ㅜ, ㅟ→ㅜ, ㅞ→ㅝ, ㅢ→ㅡ
    private val compoundVowelDecomposeMap = compoundVowelMap.entries
        .associate { (pair, compound) -> compound to pair.first }

    // ── 이중받침 합성/분해 ───────────────────────────────────────────
    private val doubleJongseongMap = mapOf(
        Pair('ㄱ', 'ㅅ') to 'ㄳ',
        Pair('ㄴ', 'ㅈ') to 'ㄵ',
        Pair('ㄴ', 'ㅎ') to 'ㄶ',
        Pair('ㄹ', 'ㄱ') to 'ㄺ',
        Pair('ㄹ', 'ㅁ') to 'ㄻ',
        Pair('ㄹ', 'ㅂ') to 'ㄼ',
        Pair('ㄹ', 'ㅅ') to 'ㄽ',
        Pair('ㄹ', 'ㅌ') to 'ㄾ',
        Pair('ㄹ', 'ㅍ') to 'ㄿ',
        Pair('ㄹ', 'ㅎ') to 'ㅀ',
        Pair('ㅂ', 'ㅅ') to 'ㅄ'
    )
    private val doubleJongseongDecomposeMap = doubleJongseongMap.entries
        .associate { (k, v) -> v to k }

    // ── 나랏글 모음 순환 ─────────────────────────────────────────────
    // 키에서 전달되는 기본 자모 → 순환 목록
    private val vowelCycleMap = mapOf(
        'ㅏ' to listOf('ㅏ', 'ㅓ'),
        'ㅗ' to listOf('ㅗ', 'ㅜ')
    )

    // ── 상태 ─────────────────────────────────────────────────────────
    private enum class State {
        EMPTY,
        CHOSEONG,
        JUNGSEONG_ONLY,
        CHO_JUNG,
        CHO_JUNG_JONG_CANDIDATE
    }

    private var state = State.EMPTY
    private var choseong: Char = '\u0000'
    private var jungseong: Char = '\u0000'
    private var jongseong: Char = '\u0000'
    private var lastVowelKey: Char = '\u0000'
    private var lastVowelCycleCount: Int = 0

    data class InputResult(
        val committed: String,
        val composing: String
    )

    // ── 자모 분류 ─────────────────────────────────────────────────────
    private fun isConsonant(c: Char): Boolean = c in choseongList
    private fun isVowel(c: Char): Boolean = c in jungseongList
    private fun isValidJongseong(c: Char): Boolean =
        c != '\u0000' && jongseongList.contains(c)

    // ── 인덱스 조회 ───────────────────────────────────────────────────
    private fun choseongIndex(c: Char) = choseongList.indexOf(c)
    private fun jungseongIndex(c: Char) = jungseongList.indexOf(c)
    private fun jongseongIndex(c: Char) = if (c == '\u0000') 0 else jongseongList.indexOf(c)

    // ── 음절 조합 ─────────────────────────────────────────────────────
    private fun compose(cho: Char, jung: Char, jong: Char = '\u0000'): Char {
        val ci = choseongIndex(cho)
        val ji = jungseongIndex(jung)
        val joi = jongseongIndex(jong)
        if (ci < 0 || ji < 0 || joi < 0) return '\u0000'
        return (0xAC00 + ci * 21 * 28 + ji * 28 + joi).toChar()
    }

    // ── 현재 조합 텍스트 ──────────────────────────────────────────────
    private fun currentComposing(): String = when (state) {
        State.EMPTY -> ""
        State.CHOSEONG -> choseong.toString()
        State.JUNGSEONG_ONLY -> jungseong.toString()
        State.CHO_JUNG -> compose(choseong, jungseong).toString()
        State.CHO_JUNG_JONG_CANDIDATE -> compose(choseong, jungseong, jongseong).toString()
    }

    // ── 상태 초기화 ───────────────────────────────────────────────────
    private fun resetState() {
        state = State.EMPTY
        choseong = '\u0000'
        jungseong = '\u0000'
        jongseong = '\u0000'
        lastVowelKey = '\u0000'
        lastVowelCycleCount = 0
    }

    // ── 현재 조합을 확정하고 상태 초기화 ─────────────────────────────
    private fun flush(): String {
        val result = currentComposing()
        resetState()
        return result
    }

    // ── 모음 순환 해석 ────────────────────────────────────────────────
    private fun resolveVowelCycle(jamoKey: Char): Char {
        val cycleList = vowelCycleMap[jamoKey] ?: return jamoKey
        return if (jamoKey == lastVowelKey) {
            lastVowelCycleCount = (lastVowelCycleCount + 1) % cycleList.size
            cycleList[lastVowelCycleCount]
        } else {
            lastVowelKey = jamoKey
            lastVowelCycleCount = 0
            cycleList[0]
        }
    }

    // ── flush 후 새 모음의 순환 상태 복원 ───────────────────────────────
    // flush() → resetState()가 lastVowelKey를 초기화하므로,
    // 새 모음이 어떤 순환 키에 속하는지 역추적하여 lastVowelKey/Count를 복원.
    private fun restoreVowelCycleFor(vowel: Char) {
        for ((key, list) in vowelCycleMap) {
            val idx = list.indexOf(vowel)
            if (idx >= 0) {
                lastVowelKey = key
                lastVowelCycleCount = idx
                return
            }
        }
        // 순환 목록에 없는 모음(ㅣ 등)은 복원 불필요
    }

    // ── 자모 입력 진입점 ──────────────────────────────────────────────
    /**
     * 나랏글 키보드에서 자모 키를 눌렀을 때 호출.
     * @param jamoKey 레이아웃에서 전달하는 기본 자모 (ㄱ, ㄴ, ㅏ, ㅗ, ㅣ 등)
     */
    fun inputJamo(jamoKey: Char): InputResult {
        // CHO_JUNG_JONG_CANDIDATE 상태에서 모음 입력 시:
        // 다음 음절이 시작되므로 모음 순환 상태를 리셋해야 첫 탭으로 인식됨
        if (state == State.CHO_JUNG_JONG_CANDIDATE && vowelCycleMap.containsKey(jamoKey)) {
            lastVowelKey = '\u0000'
            lastVowelCycleCount = 0
        }

        val isCycleTap = vowelCycleMap.containsKey(jamoKey) && jamoKey == lastVowelKey
        val resolvedJamo = resolveVowelCycle(jamoKey)

        return when {
            isVowel(resolvedJamo) -> handleVowel(resolvedJamo, isCycleTap)
            isConsonant(resolvedJamo) -> handleConsonant(resolvedJamo)
            else -> InputResult("", currentComposing())
        }
    }

    // ── 모음 처리 ─────────────────────────────────────────────────────
    private fun handleVowel(vowel: Char, isCycleTap: Boolean): InputResult {
        return when (state) {

            State.EMPTY -> {
                state = State.JUNGSEONG_ONLY
                jungseong = vowel
                InputResult("", currentComposing())
            }

            State.CHOSEONG -> {
                // 자음 입력 후 모음: 모음 순환 상태는 이미 resolveVowelCycle에서 갱신됨
                // 단, 이 상태로 진입할 때 자음을 입력했으므로 lastVowelKey를 여기서도 갱신
                state = State.CHO_JUNG
                jungseong = vowel
                InputResult("", currentComposing())
            }

            State.JUNGSEONG_ONLY -> {
                if (isCycleTap) {
                    // 같은 모음 키 재탭: 순환 교체
                    jungseong = vowel
                    InputResult("", currentComposing())
                } else {
                    val compound = compoundVowelMap[Pair(jungseong, vowel)]
                    if (compound != null) {
                        // 복합모음 합성
                        jungseong = compound
                        InputResult("", currentComposing())
                    } else {
                        // 합성 불가: 이전 모음 확정, 새 모음 시작
                        // flush() → resetState()가 lastVowelKey를 초기화하므로
                        // 새 모음의 순환 상태를 복원해야 함
                        val committed = flush()
                        state = State.JUNGSEONG_ONLY
                        jungseong = vowel
                        restoreVowelCycleFor(vowel)
                        InputResult(committed, currentComposing())
                    }
                }
            }

            State.CHO_JUNG -> {
                if (isCycleTap) {
                    // 같은 모음 키 재탭: 중성 교체
                    jungseong = vowel
                    InputResult("", currentComposing())
                } else {
                    val compound = compoundVowelMap[Pair(jungseong, vowel)]
                    if (compound != null) {
                        // 복합모음 합성
                        jungseong = compound
                        InputResult("", currentComposing())
                    } else {
                        // 합성 불가: 현 음절 확정, 새 모음 단독 시작
                        // flush() → resetState()가 lastVowelKey를 초기화하므로
                        // 새 모음의 순환 상태를 복원해야 함
                        val committed = flush()
                        state = State.JUNGSEONG_ONLY
                        jungseong = vowel
                        restoreVowelCycleFor(vowel)
                        InputResult(committed, currentComposing())
                    }
                }
            }

            State.CHO_JUNG_JONG_CANDIDATE -> {
                // 종성 후보가 있는 상태에서 모음 입력:
                // 종성을 다음 음절의 초성으로 분리
                val prevJong = jongseong

                // 이중받침이면 앞 자음은 종성, 뒤 자음이 다음 초성
                val decomp = doubleJongseongDecomposeMap[prevJong]
                val (newJong, nextCho) = if (decomp != null) {
                    Pair(decomp.first, decomp.second)
                } else {
                    Pair('\u0000', prevJong)
                }

                // 현 음절 확정 (newJong 적용)
                val syllable = compose(choseong, jungseong, newJong)
                val committed = syllable.toString()
                resetState()

                // 다음 음절 시작: 분리된 자음이 초성, 새 모음이 중성
                // resetState()가 lastVowelKey를 초기화했으므로 복원
                state = State.CHO_JUNG
                choseong = nextCho
                jungseong = vowel
                restoreVowelCycleFor(vowel)
                InputResult(committed, currentComposing())
            }
        }
    }

    // ── 자음 처리 ─────────────────────────────────────────────────────
    private fun handleConsonant(consonant: Char): InputResult {
        // 자음 입력 시 모음 순환 상태 초기화
        lastVowelKey = '\u0000'
        lastVowelCycleCount = 0

        return when (state) {

            State.EMPTY -> {
                state = State.CHOSEONG
                choseong = consonant
                InputResult("", currentComposing())
            }

            State.CHOSEONG -> {
                // 초성만 있는 상태에서 새 자음: 이전 초성 확정
                val committed = flush()
                state = State.CHOSEONG
                choseong = consonant
                InputResult(committed, currentComposing())
            }

            State.JUNGSEONG_ONLY -> {
                // 단독 모음 뒤에 자음: 모음 확정, 새 초성
                val committed = flush()
                state = State.CHOSEONG
                choseong = consonant
                InputResult(committed, currentComposing())
            }

            State.CHO_JUNG -> {
                // 초성+중성 뒤 자음: 종성 후보로
                state = State.CHO_JUNG_JONG_CANDIDATE
                jongseong = consonant
                InputResult("", currentComposing())
            }

            State.CHO_JUNG_JONG_CANDIDATE -> {
                // 이중받침 시도
                val doubled = doubleJongseongMap[Pair(jongseong, consonant)]
                if (doubled != null) {
                    jongseong = doubled
                    InputResult("", currentComposing())
                } else {
                    // 이중받침 불가: 현 음절 확정, 새 초성
                    val committed = flush()
                    state = State.CHOSEONG
                    choseong = consonant
                    InputResult(committed, currentComposing())
                }
            }
        }
    }

    // ── 획추가 ────────────────────────────────────────────────────────
    /**
     * 현재 조합 중인 자음 또는 모음에 획을 추가.
     * 성공 시 true, 해당 없으면 false.
     *
     * 자음 적용 상태: CHOSEONG, CHO_JUNG_JONG_CANDIDATE
     * 모음 적용 상태: JUNGSEONG_ONLY, CHO_JUNG (ㅏ→ㅑ, ㅓ→ㅕ, ㅗ→ㅛ, ㅜ→ㅠ)
     */
    fun addStroke(): Boolean {
        return when (state) {
            State.CHOSEONG -> {
                val next = strokeMap[choseong] ?: return false
                choseong = next
                true
            }
            State.JUNGSEONG_ONLY -> {
                val next = strokeMap[jungseong] ?: return false
                jungseong = next
                // 획추가로 모음이 바뀌었으므로 순환 상태 초기화
                lastVowelKey = '\u0000'
                lastVowelCycleCount = 0
                true
            }
            State.CHO_JUNG -> {
                val next = strokeMap[jungseong] ?: return false
                jungseong = next
                // 획추가로 모음이 바뀌었으므로 순환 상태 초기화
                lastVowelKey = '\u0000'
                lastVowelCycleCount = 0
                true
            }
            State.CHO_JUNG_JONG_CANDIDATE -> {
                val decomp = doubleJongseongDecomposeMap[jongseong]
                if (decomp != null) {
                    // 이중받침: 뒤 자음에 획추가 시도
                    val next = strokeMap[decomp.second] ?: return false
                    // 새 이중받침 조합 가능한지 확인
                    val newDouble = doubleJongseongMap[Pair(decomp.first, next)]
                    if (newDouble != null) {
                        jongseong = newDouble
                    } else {
                        // 이중받침 재조합 불가: 단일 종성으로 (획추가된 뒤 자음)
                        // 단, 종성 테이블에 있어야 함
                        if (!isValidJongseong(next)) return false
                        jongseong = next
                    }
                } else {
                    // 단일 종성에 획추가
                    val next = strokeMap[jongseong] ?: return false
                    // 획추가 결과가 종성 테이블에 있어야 함
                    if (!isValidJongseong(next)) return false
                    jongseong = next
                }
                true
            }
            else -> false
        }
    }

    // ── 쌍자음 토글 ───────────────────────────────────────────────────
    /**
     * 현재 조합 중인 자음을 쌍자음으로 전환하거나 원래대로 복원.
     * 성공 시 true, 해당 없으면 false.
     *
     * 적용 상태: CHOSEONG, CHO_JUNG_JONG_CANDIDATE
     * 이중받침 상태에서는 뒤 자음에 쌍자음 토글 적용.
     */
    fun toggleDouble(): Boolean {
        return when (state) {
            State.CHOSEONG -> {
                val doubled = doubleMap[choseong]
                if (doubled != null) {
                    choseong = doubled; true
                } else {
                    val origin = doubleReverseMap[choseong]
                    if (origin != null) { choseong = origin; true } else false
                }
            }
            State.CHO_JUNG_JONG_CANDIDATE -> {
                val decomp = doubleJongseongDecomposeMap[jongseong]
                if (decomp != null) {
                    // 이중받침: 뒤 자음에 쌍자음 토글 시도
                    val doubled = doubleMap[decomp.second]
                    if (doubled != null) {
                        val newDouble = doubleJongseongMap[Pair(decomp.first, doubled)]
                        if (newDouble != null) {
                            jongseong = newDouble; true
                        } else false
                    } else {
                        val origin = doubleReverseMap[decomp.second]
                        if (origin != null) {
                            val newDouble = doubleJongseongMap[Pair(decomp.first, origin)]
                            if (newDouble != null) {
                                jongseong = newDouble; true
                            } else false
                        } else false
                    }
                } else {
                    // 단일 종성에 쌍자음 토글
                    val doubled = doubleMap[jongseong]
                    if (doubled != null && isValidJongseong(doubled)) {
                        jongseong = doubled; true
                    } else {
                        val origin = doubleReverseMap[jongseong]
                        if (origin != null && isValidJongseong(origin)) {
                            jongseong = origin; true
                        } else false
                    }
                }
            }
            else -> false
        }
    }

    // ── Backspace ─────────────────────────────────────────────────────
    /**
     * 현재 조합 중인 마지막 자모를 하나 삭제.
     * 반환값: 수정된 조합 텍스트 (빈 문자열이면 조합 완전 취소됨)
     */
    fun backspace(): String {
        lastVowelKey = '\u0000'
        lastVowelCycleCount = 0

        when (state) {
            State.EMPTY -> { /* 외부에서 커서 앞 문자 삭제 */ }

            State.CHOSEONG -> {
                state = State.EMPTY
                choseong = '\u0000'
            }

            State.JUNGSEONG_ONLY -> {
                // 복합모음이면 분해, 단일이면 EMPTY
                val base = compoundVowelDecomposeMap[jungseong]
                if (base != null) {
                    jungseong = base
                    // state는 JUNGSEONG_ONLY 유지
                } else {
                    state = State.EMPTY
                    jungseong = '\u0000'
                }
            }

            State.CHO_JUNG -> {
                // 복합모음이면 분해, 단일이면 초성으로
                val base = compoundVowelDecomposeMap[jungseong]
                if (base != null) {
                    jungseong = base
                    // state는 CHO_JUNG 유지
                } else {
                    state = State.CHOSEONG
                    jungseong = '\u0000'
                }
            }

            State.CHO_JUNG_JONG_CANDIDATE -> {
                // 이중받침이면 앞 자음만 남기고, 단일이면 CHO_JUNG으로
                val decomp = doubleJongseongDecomposeMap[jongseong]
                if (decomp != null) {
                    jongseong = decomp.first
                    // state는 CHO_JUNG_JONG_CANDIDATE 유지
                } else {
                    state = State.CHO_JUNG
                    jongseong = '\u0000'
                }
            }
        }

        return currentComposing()
    }

    // ── 공개 API ──────────────────────────────────────────────────────
    fun flushComposing(): String = flush()
    fun getComposingText(): String = currentComposing()
    fun isEmpty(): Boolean = state == State.EMPTY

    fun reset() {
        resetState()
    }
}

# DaramKeyboard 코드베이스 레퍼런스

> 토큰 절약용 빠른 참조 문서. 새 세션 시작 시 이 파일을 읽으면 파일 탐색 최소화 가능.

## 패키지 구조

```
com.daram.keyboard/
├── service/     DaramInputMethodService.kt          (IME 핵심, 477줄)
├── view/        KeyboardView.kt, CandidateBarView.kt, EmojiKeyboardView.kt
├── input/       InputEngine(interface), HangulInputEngine, QwertyInputEngine,
│                KeyboardFactory, ShiftState, HardwareKeyMapper,
│                KoreanDubeolsikHardwareKeyMapper, QwertyHardwareKeyMapper
├── layout/      KeyboardLayout(interface) + 10개 구현체
├── model/       Key, KeyAction(sealed), KoreanKeyboardType(enum), KeyboardState
├── hittarget/   HitTargetManager.kt                 (WP8 방식 동적 히트, 247줄)
├── prediction/  WordPredictionEngine.kt, SharedPrefsUserLearningDelegate.kt
├── settings/    PreferenceManager.kt(object), SettingsActivity.kt
├── theme/       KeyboardTheme(data class), ThemeManager(object)
├── feedback/    HapticFeedbackManager.kt, SoundFeedbackManager.kt
└── MainActivity.kt
```

---

## 핵심 모델

### Key (data class)
```kotlin
Key(id, primaryLabel, secondaryLabel?, action: KeyAction,
    longPressAction?, widthWeight=1f, rowSpan=1,
    style: KeyStyle=NORMAL, visible=true)
```

### KeyAction (sealed class) - 주요 타입
```
TypeText(text), TypeHangul(jamo), TypeRawKey(char), TypeNumber(digit)
Backspace, Enter, Space
SwitchToQwerty, SwitchToNaratgul, SwitchToNumber, SwitchToSymbol
ShowEmoji, SwitchEmojiCategory(idx), ReturnToPrev
AddStroke, ToggleDouble, Shift, ShiftLock
SwitchKoreanType, SwitchToNextIme
```

### KoreanKeyboardType (enum)
`NARATGEUL, CHEONJIIN, SKY2, MOTOROLA, DANMOEUM, DUBEOLSIK, MUE128`
- 각 타입: `keyMapper()`, `ambiguityResolver()` 제공 → nutcracker 오토마타 연결

### KeyStyle (enum)
`NORMAL, FUNCTION, SPACE, ENTER, SHIFT, BACKSPACE, RETURN`

### KeyboardLayout (interface)
```kotlin
val rows: List<List<Key>>
val rowStartXRatios: List<Float>   // 행 시작 X 비율 (QWERTY 행2: 0.05)
val isAuxiliary: Boolean           // 보조 레이아웃 여부 (기호/이모지)
val secondaryLabelAlignment: SecondaryLabelAlignment
val returnButtonLabel: String      // "한" 또는 "EN"
```

---

## 레이아웃 목록

| 파일 | 자판 | 특징 |
|------|------|------|
| NaratgulLayout | 나랏글 9-키 | 4행, 모호성 기반 |
| CheonjiinLayout | 천지인 | · 방식 |
| SkyIILayout | SKY-II | |
| MotorolaLayout | 모토로라 | |
| DanmoemLayout | 단모음 | |
| DubeolsikLayout | 두벌식 | hasShift=true |
| Mue128Layout | 무이128 | §8-2 기준 |
| QwertyLayout | QWERTY | 4행, rowStartXRatios[1]=0.05 |
| SymbolLayout | 기호 | 3페이지, isAuxiliary=true |
| NumberLayout | 숫자 | isAuxiliary=true |
| EmojiLayout | 이모지 | 8카테고리, isAuxiliary=true |

---

## PreferenceManager 키 목록

| 키 | 타입 | 기본값 | 선택지 |
|----|------|--------|--------|
| `haptic_intensity` | String | "medium" | off/weak/medium/strong |
| `sound_enabled` | Boolean | false | |
| `theme` | String | "auto" | auto/light/dark |
| `word_prediction` | Boolean | true | |
| `show_hit_targets` | Boolean | false | (개발자) |
| `korean_keyboard_type` | String | "naratgeul" | 7가지 |
| `keyboard_height` | String | "medium" | small(240dp)/medium(280dp)/large(320dp) |
| `key_popup` | Boolean | true | |
| `emoji_skin_tone` | String | "none" | none/light/medium_light/medium/medium_dark/dark |
| `auto_period_double_space` | Boolean | false | |
| `auto_composition_timeout` | String | "0" | 0/500/1000/2000(ms) |
| `landscape_keyboard_type` | String | "same" | same + 7가지 한글 |
| `hide_keyboard_on_hardware` | Boolean | false | |

---

## KeyboardTheme (data class)
```kotlin
KeyboardTheme(backgroundColor, keyNormalBackground, keyFunctionBackground,
              keyPressedBackground, keyLabelColor, keySecondaryLabelColor,
              dividerColor, keyLabelSizeSp=20f, keySecondaryLabelSizeSp=11f)
```
`ThemeManager.resolveTheme(context)` → LIGHT 또는 DARK 반환

---

## 입력 처리 흐름

### 터치 → 한글 입력
```
onTouchEvent(x,y)
→ HitTargetManager.findKeyAt(x,y) → Key
→ (UP) HitTargetManager.recordTouch() + handleKeyAction(action)
→ HangulInputEngine.processAction()
→ automata.process() [nutcracker]
→ ic.setComposingText()
→ onComposingChanged() [콜백]
  → updateCandidates() → CandidateBarView 갱신
  → updateNextKeyHints() → HitTargetManager.setNextKeyHints()
```

### 스페이스/엔터 → 단어 확정
```
handleKeyAction(Space/Enter)
→ inputEngine.processAction()
→ flushAndCommit() → 완성된 단어 반환
→ predictionEngine.onWordCommitted(word, language)
→ SharedPrefs에 학습 데이터 저장
```

---

## HitTargetManager 알고리즘

### 가중치 업데이트 (EMA)
- 눌린 키: `weight = (prev × 0.95 + 0.1).clamp(0.5, 3.0)`
- 다른 키: `weight × 0.99`
- `jamoFrequencyWeights["keyId:jamo"]` 형식으로 자소별 추적

### 히트 영역 확장 계산
```
확장 = baseExpansion(8px)
     + freqExpansion((weight-1) × 6px)   // 최대 +12px
     + hintExpansion(hint × 10px)         // 예측 힌트 최대 +10px
     + directionBias(4px)                 // 터치 방향 편향
→ Voronoi 클리핑: 인접 키와 경계 분할
```

### 주요 메서드
```kotlin
setKeyRects(keys, rects: Map<String, RectF>)
recordTouch(x, y, key: Key, jamo: Char?)
setNextKeyHints(hints: Map<String, Float>)  // keyId → 0.0~1.0
findKeyAt(x, y): Key?
recalculateHitTargets()
```

---

## DaramInputMethodService 주요 멤버

```kotlin
// Views
keyboardView: KeyboardView
emojiKeyboardView: EmojiKeyboardView
candidateBarView: CandidateBarView

// Engines
koreanEngine: InputEngine          // 자판 타입에 따라 변경
qwertyEngine: QwertyInputEngine
hardwareHangulEngine: HangulInputEngine  // 두벌식 고정

// Prediction
predictionEngine: WordPredictionEngine

// Feedback
hapticManager: HapticFeedbackManager
soundManager: SoundFeedbackManager
```

### 주요 메서드
```kotlin
onCreateInputView(): View          // 후보바+키보드+이모지 구성
onKeyDown(keyCode, event): Boolean // 하드웨어 키보드
onStartInputView(info, restarting) // 테마/레이아웃 적용
applySubtypeLayout()               // 언어/회전 기반 자판 자동 선택
onComposingChanged(committed, state, composing) // 예측 업데이트
rescheduleAutoComposition()        // 자동 조합 완성 타이머
```

---

## KeyboardView 주요 동작

### 특수 터치 처리
- **롱프레스** (400ms): `longPressAction` 실행
- **백스페이스 롱프레스**: 400ms 후 50ms마다 자동 삭제
- **이모지 스와이프**: 40px 이상 좌우 → 카테고리 전환
- **더블스페이스** (500ms 내): `". "` 자동 입력 (설정 필요)

### 주요 메서드
```kotlin
onDraw(canvas)                              // 키보드 렌더링
onTouchEvent(event): Boolean               // 터치 처리
switchLayout(layout, engine?)              // 자판 변경
handleKeyAction(action: KeyAction)         // 키 액션 처리
calculateKeyRects()                        // 키 위치 계산
applyNextKeyHints(hints: Map<String,Float>) // 예측 힌트 반영
```

### 키 위치 계산
```
rowHeight = totalHeight / rows.size
startX = rowStartXRatios[rowIdx] × totalWidth
pixelsPerWeight = (totalWidth - startX) / sum(widthWeights)
keyRect.left = startX + sum(prev widths)
keyRect.right = keyRect.left + key.widthWeight × pixelsPerWeight
```

---

## HangulInputEngine 주요 동작

```kotlin
// 상태
automata: HangulAutomata           // nutcracker 라이브러리
shiftState: ShiftState             // OFF/ONCE/LOCKED (hasShift=true 인 경우만)
lastInputJamo: Char?               // 마지막 입력 자소 (히트 타겟 조정용)
onTextCommitted: ((String) -> Unit)? // commit 추적 콜백

// 주요 메서드
processAction(action, ic)
processInput(input: KeyInput, ic)
flushAndCommit(ic): String    // 조합 완성, 완성된 문자열 반환
decomposeLastJamo(): Char?    // 음절→자소 분해
```

---

## WordPredictionEngine 주요 메서드

```kotlin
init()                             // 비동기 사전 초기화
getSuggestions(committed, state, composing, language): List<String>  // 후보 3개
getNextKeyHintsForHangul(...): Map<String, Float>   // keyId → 확률
getNextKeyHintsForQwerty(input): Map<String, Float>
onWordCommitted(word, language)    // 단어 학습
onCandidateSelected(word, language) // 후보 선택 학습
```

---

## 빌드 설정

```
compileSdk = 36, minSdk = 35, targetSdk = 36
Kotlin JVM target = 21
의존성:
  - androidx.core.ktx, appcompat, preference.ktx, recyclerview
  - kotlinx.coroutines.android
  - com.github.daramkun:nutcracker:main-SNAPSHOT (JitPack)
    → KMP 아티팩트: iOS/macOS 네이티브 exclude 필수
```

---

## 의존 관계 요약

```
DaramInputMethodService
├── KeyboardView
│   ├── HitTargetManager
│   ├── InputEngine (HangulInputEngine / QwertyInputEngine)
│   ├── KeyboardLayout (10개 구현체)
│   ├── Key, KeyAction, KeyboardTheme
│   └── PreferenceManager
├── CandidateBarView
├── EmojiKeyboardView
├── WordPredictionEngine
│   ├── nutcracker (외부)
│   └── SharedPrefsUserLearningDelegate
├── HapticFeedbackManager, SoundFeedbackManager
└── KeyboardFactory → (layout, engine) 쌍 생성
```

---

## ShiftState 상태 머신

```
OFF → ONCE (Shift 한 번) → LOCKED (Shift 두 번) → OFF
                                                    ↑
                         스페이스 입력 시 ONCE→OFF ──┘
```

---

## 자주 수정되는 파일

| 파일 | 수정 이유 |
|------|-----------|
| `layout/*.kt` | 자판 키 배치/액션 변경 |
| `service/DaramInputMethodService.kt` | 입력 로직, 자판 전환, 예측 연동 |
| `view/KeyboardView.kt` | UI 렌더링, 터치 처리, 팝업 |
| `hittarget/HitTargetManager.kt` | 히트 타겟 알고리즘 튜닝 |
| `settings/PreferenceManager.kt` | 새 설정 키 추가 |
| `model/KeyAction.kt` | 새 액션 타입 추가 |

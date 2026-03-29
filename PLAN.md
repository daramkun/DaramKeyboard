# 다람 키보드 구현 계획

## 배경

nutcracker 라이브러리 통합, 다중 키보드 타입 지원, 레이아웃 추상화, 하드웨어 키보드 지원을 위한
단계별 구현 계획.

---

## 작업 순서

### 1단계: 레이아웃 렌더링 추상화 ✅ (진행 중)

다른 단계의 기반이 되는 작업. 새 레이아웃 추가가 쉬워지도록 구조를 개선한다.

**변경 사항:**

- `KeyboardLayout` 인터페이스에 렌더링 메타데이터 속성 추가
  - `isAuxiliary: Boolean` — 숫자/기호/이모지 등 보조 레이아웃 여부. `true`이면 레이아웃 전환 시 `prevLayout` 갱신 안 함
  - `secondaryLabelAlignment: SecondaryLabelAlignment` — 보조 레이블(숫자 힌트 등) 위치 (`TOP_LEFT` / `BOTTOM_RIGHT`)
  - `returnButtonLabel: String` — 보조 레이아웃 내 ReturnToPrev 버튼에 표시할 레이블 ("한" 또는 "EN")
- 각 레이아웃 파일에서 해당 속성 선언
- `KeyboardView`에서 하드코딩된 `is NaratgulLayout`, `is SymbolLayout` 등 타입 체크를 메타데이터 참조로 교체
- `isAuxLayout()` 메서드 제거 (→ `l.isAuxiliary`로 대체)

**목표:** 새 레이아웃 추가 시 `KeyboardView` 수정 불필요. 레이아웃 파일만 추가하면 됨.

---

### 2단계: nutcracker 라이브러리 통합

**현재:** 자체 `HangulComposer` + `Trie` / `WordPredictionEngine` / `UserLearningModel`

**변경 방향:**

1. `build.gradle.kts`에 nutcracker dependency 추가 (JitPack 또는 로컬 모듈)
2. `HangulComposer.kt` → nutcracker 오토마타 API로 교체 (또는 삭제)
3. `NaratgulInputEngine.kt` → nutcracker 한글 조합 API 사용
4. `WordPredictionEngine.kt` → nutcracker 예측 API 사용 (기능 중복 시 자체 코드 삭제)
5. `Trie.kt`, `UserLearningModel.kt` → nutcracker가 커버하면 삭제

**원칙:** `InputEngine` 인터페이스는 그대로 유지. 내부 구현만 nutcracker로 교체하여 다른 코드 영향 최소화.

**참조:** https://github.com/daramkun/nutcracker

---

### 3단계: 다중 키보드 타입 지원

**지원할 키보드 타입:** 나랏글(기존), 천지인, SKY-II, 모토로라, 단모음, 두벌식

**아키텍처 확장:**

```
InputEngine (interface)
├── NaratgulInputEngine      ← 현재 (nutcracker 기반으로 교체됨)
├── MobilePhoneInputEngine   ← 공통 베이스 (천지인/SKY-II/모토로라 공유)
│   ├── CheonjinInputEngine  ← ㆍ,ㅡ,ㅣ 조합 규칙 테이블
│   ├── SkyIIInputEngine     ← SKY-II 조합 규칙 테이블
│   └── MotorolaInputEngine  ← 모토로라 조합 규칙 테이블
├── DanmoeumInputEngine      ← 단순 단모음 1:1 매핑
└── DubulsikInputEngine      ← KS X 5002 표준 두벌식
```

```
KeyboardLayout (interface)
├── NaratgulLayout           ← 현재
├── CheonjinLayout           ← 3×4 천지인
├── SkyIILayout
├── MotorolaLayout
├── DanmoeumLayout
└── DubulsikLayout           ← QWERTY 배열에 한글 자모 배치
```

**키보드 타입 선택:**

- `PreferenceManager`에 `keyboardType: KeyboardType` enum 추가
- `DaramInputMethodService`에서 설정값에 따라 엔진/레이아웃 팩토리에서 생성

**`KeyAction` 확장:**

```kotlin
// 특정 타입 이름 대신 범용 전환 액션 사용
data class SwitchToKeyboardType(val type: KeyboardType) : KeyAction()
```

---

### 4단계: 하드웨어 키보드 지원

**구현 방법:** `InputMethodService`의 `onKeyDown` / `onKeyUp` 오버라이드

```
HardwareKeyMapper (interface)
  └── mapKeyCode(keyCode: Int, event: KeyEvent, shiftState: ShiftState): KeyAction?

KoreanDubulsikHardwareKeyMapper   ← KS X 5002 두벌식 매핑 테이블
QwertyHardwareKeyMapper           ← 키코드 그대로 전달

DaramInputMethodService
  └── onKeyDown() → currentHardwareKeyMapper.mapKeyCode() → InputEngine.processAction()
```

**추가 고려 사항:**
- `onEvaluateInputViewShown()` 오버라이드: 하드웨어 키보드 연결 시 소프트 키보드 자동 숨김 (설정 옵션)
- `KEYCODE_LANGUAGE_SWITCH` 처리: 한/영 전환
- 나랏글/천지인 등 특수 레이아웃의 하드웨어 매핑은 두벌식 기본 매핑 사용

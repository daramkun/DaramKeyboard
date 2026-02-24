# 다람 키보드
Android용 나랏글 키보드 프로젝트.

## 필요 기능
- [동적 히트 타겟](https://blogs.windows.com/windowsexperience/2012/12/06/the-secrets-of-the-windows-phone-8-keyboard/)
- 플랫 UI 디자인 (Windows Phone용 키보드와 동일한 디자인) 및 라이트/다크 모드
   - references/ 내 이미지 참조
- 한국어(나랏글) <-> QWERTY 전환
- 일부 키를 꾹 누르고 있으면 숫자로 변환입력
- 키 햅틱 반응 및 사운드 출력 옵션

## 프로젝트 구조

| 컴포넌트 | 기술 |
|:-----:|:----:|
| Operating System | Android 15 or newer |
| Programming Language | `Kotlin` |
| Build System | Android Gradle Plugin 9.1 or higher |
| IME | `InputMethodService` |
| Render | Custom View + Canvas |
| Hit Target | 키 당 별도 RectF 관리 + 확률 기반 조정 |
| 단어 예측 | Trie 기반 사전 + bigram |

## 그 외
.claude/claude-code-agent-workflow-principles-boris-cherny-claude.md 파일 참조

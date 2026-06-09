# Claude 작업 규칙

## 절대 규칙
- **GitHub MCP 플러그인(`mcp__plugin_ecc_github__push_files`)으로 절대 푸시하지 말 것. 반드시 `git` 명령어만 사용.**

## 빌드 오류 방지 규칙
어떤 클래스/필드/함수 시그니처를 수정할 때는 반드시 먼저:
```
Grep으로 수정할 대상 이름을 전체 프로젝트에서 검색 → 참조하는 파일 전부 확인 → 함께 수정
```

### 과거 실수 사례 (잊지 말 것)
1. `ScheduledMessage` 필드 삭제(`hour/minute/second/isRepeating`) → `AlarmScheduler.kt` 업데이트 누락 → 빌드 오류
2. `MessageStore` 함수에 `roomId` 파라미터 추가 → `OverlayActivity.kt` 업데이트 누락 → 빌드 오류
3. `MainActivity.kt`에서 Glide import 삭제 → `loadRoomProfile()`에서 여전히 사용 → 빌드 오류

## 프로젝트 개요
- KakaoTalk 스타일 예약 메세지 알림 Android 앱
- 언어: Kotlin
- 주요 파일: MainActivity, AlarmDisplayActivity, OverlayActivity, MessageStore, ScheduledMessage, NotificationCardAdapter

# 키오스크 세션 영속화 (프로세스 회수 시 로그아웃 방지)

## 목표
안드로이드가 앱 프로세스를 회수해도 로그인 상태가 유지되게 한다.

## 배경 (2026-08-03 제보)
"앱을 계속 켜뒀는데 번호 체크인하다 로그아웃되고 다시 로그인해야 한다. 로그인한 지 1시간도 안 됐다."

원인 확정.
- `SessionManager`가 `token` / `branchId` 등을 **평범한 in-memory `var`로만** 들고 있고
  디스크 저장이 전혀 없다. 프로세스가 회수되면 **토큰 나이와 무관하게** 세션이 통째로 사라진다.
  `AdminTokenRefresher`의 7일 만료 갱신 로직은 이 경로에서 작동할 기회가 없다.
- 프로세스가 다시 뜨면 `NavGraph`의 SPLASH가 **세션 확인 없이 항상 LOGIN으로** 보낸다.
  `LoginViewModel.init`의 자동로그인은 `autoLoginManager.isEnabled`가 켜져 있을 때만 동작한다.

## 영향 파일
- `data/session/SessionManager.kt` — SharedPreferences 영속화
- `ui/navigation/NavGraph.kt` — SPLASH 이후 분기 (로그인 상태면 HOME)
- `ui/home/MainCheckinScreen.kt`, `ui/home/HomeScreen.kt` — `@Preview`의 직접 생성자 호출 수정
  (생성자 시그니처가 바뀌므로. **둘 다 `@Preview` 안이라 실동작에는 영향 없다.**)

## 단계
1. `SessionManager`에 `@ApplicationContext` 주입, 모든 필드를 SharedPreferences 읽기/쓰기로 전환.
   `login()`은 저장, `logout()`은 삭제, `updateBranchInfo()`도 저장.
2. `NavGraph`의 SPLASH `onSplashFinished`에서 `sessionManager.isLoggedIn`이면 HOME, 아니면 LOGIN.
3. `@Preview` 2곳의 `SessionManager(...)` 생성자 호출에 context 추가.
4. `:app:compileDebugKotlin --rerun-tasks` 통과 확인.

## 비대상 (손대지 않는다)
- 토큰 만료·갱신 로직(`AdminTokenRefresher`) — 그대로 둔다.
- 자동로그인(`AutoLoginManager`) 동작 — 그대로 둔다.
- 로그인 화면 UI.

## 알려진 한계
- **저장한 토큰이 7일 만료를 넘기면 갱신에는 자격증명이 필요하다.** `AdminTokenRefresher.refreshBlocking`은
  `autoLoginManager.isEnabled` + 저장된 아이디/비번이 있어야 재로그인한다. 자동로그인이 꺼져 있으면
  만료 후에는 결국 수동 로그인이 필요하다. 이 수정은 **프로세스 회수(수 시간 주기)** 를 막는 것이고
  **토큰 만료(7일 주기)** 를 막는 게 아니다.
- 토큰을 평문 SharedPreferences에 저장한다. 다만 `AutoLoginManager`가 이미 **아이디/비밀번호를
  평문으로** 저장하고 있어 보안 수준이 나빠지지는 않는다.

## 검증
- `:app:compileDebugKotlin --rerun-tasks` 0 에러
- 실기기 확인 필요: 로그인 → 앱 강제종료(또는 개발자옵션 "백그라운드 프로세스 제한") → 재실행 시
  로그인 화면을 거치지 않고 홈으로 진입하는지

## 관련
- 백엔드 조사: `body_switch_backend/.ai-docs/exec-plans/20260803-194127-kiosk-client-failure-report.md`

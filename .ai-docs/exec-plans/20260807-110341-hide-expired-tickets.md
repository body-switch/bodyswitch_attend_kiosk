# 만료 이용권 숨기기 설정 추가

## 목표
체크인 화면에 "사용 중인 수강권·이용권"만 보이도록 하는 설정을 추가한다. 현재는 만료된 것까지 같이 노출된다.

## 결정 사항
- 저장 위치: 기기 로컬 SharedPreferences (기존 QR/전화/출입문 설정과 동일). 서버·DDL 변경 없음.
- 숨김 범위: "만료된 수강권" + "만료된 이용권"(이용권 + PASS형 체험권) 두 섹션 모두.
- 기본값: `false` (현행 유지). 지점별로 켜서 사용.
- 만료 판정: 서버가 내려주는 `status == "INACTIVE"` 그대로. 기간 만료·횟수 소진 구분 없음.
- 예외: 유효한 수강권/이용권이 하나도 없는 회원은 설정이 켜져 있어도 만료 카드를 그대로 보여준다(화면이 완전히 비는 것 방지).

## 영향 파일
- `app/src/main/java/com/bodyswitch/checkin/data/session/CheckinSettingsManager.kt`
- `app/src/main/java/com/bodyswitch/checkin/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/bodyswitch/checkin/ui/checkin/CheckinViewModel.kt`
- `app/src/main/java/com/bodyswitch/checkin/ui/checkin/CheckinScreen.kt`

## 단계
1. `CheckinSettingsManager`에 `hideExpiredTicketsEnabled` 프로퍼티 추가 (key `hide_expired_tickets`, 기본 false).
2. `SettingsScreen`에 "체크인 화면 표시" 섹션과 토글 카드 추가. 기존 저장 버튼(`SaveConfirmDialog.onConfirm`) 흐름에서 기록.
3. `CheckinUiState`에 `hideExpiredTickets` 추가하고 이용권 조회 시 설정값 주입 (`settingsManager` 이미 주입돼 있음).
4. `CheckinScreen`의 만료 섹션 두 곳에 `showExpired` 게이트 적용.

## 손대지 않는 것
- 백엔드 `KioskCheckinService.getActiveCourseTickets/getActiveTrialTickets/getActiveCoursePasses` — 계속 ACTIVE+INACTIVE 둘 다 내려준다.
- 자동 체크인 판정, 무차감 재입장, 퇴실 흐름.
- 만료 카드의 클릭 불가 처리(`onClick = {}`).

## 검증
- `./gradlew assembleDebug --rerun-tasks`
- 수동 QA 4케이스: 설정 OFF/ON × (유효권 있음 / 유효권 없고 만료권만 있음)

## DoD
- [ ] 설정 화면에 토글이 보이고 저장 후 재진입 시 값이 유지된다
- [ ] 설정 ON + 유효권 있음 → 만료 섹션 미노출
- [ ] 설정 ON + 유효권 없음 → 만료 섹션 노출
- [ ] 설정 OFF → 현행과 동일
- [ ] assembleDebug 빌드 성공

# 재입장 시 이용권 재확인 설정 추가

## 배경 — 먼저 확인한 것
"하루에 수업이 두 번이면 재입장 로직 때문에 두 번째 체크인이 막히는 것 아니냐"는 우려를 조사했으나 **막히지 않는다.**
- `KioskCheckinService.computeReentry` 맨 앞의 `hasPendingReserved` 가드가, 오늘 아직 출석 안 한 예약(종료 전)이 남아 있으면 재입장을 건너뛰고 일반 체크인 흐름으로 보낸다. 재입장 최초 커밋(`e6eca239`, 2026-06-19)부터 있던 가드다.
- 운영 데이터 확인: 최근 30일 하루 2개 이상 예약 회원(최대 5개)이 전부 ATTENDED. 재입장 로그 142건 중 그날 예약이 2건 이상이었던 경우 0건. `END_TIME` NULL 예약 0건(가드 우회 경로 없음).
- 즉 "아침 헬스 이용권 + 저녁 PT" 같은 조합은 이미 정상 동작한다.

그래서 재입장 로직을 제거하는 대신 **지점이 선택할 수 있는 설정으로 뺀다.**

## 목표
재입장 자격이 있는 회원에게 이용권을 다시 확인시킬지를 지점(기기)별로 선택할 수 있게 한다.

## 결정 사항
- 저장 위치: 기기 로컬 SharedPreferences. 서버 변경 없음.
- 기본값: `false`(재확인 안 함) = 현행 유지.
- 적용 범위: 자동 재입장과 "계속 이용 / 퇴실" 선택 화면의 "계속 이용" 양쪽 모두.
- 켰을 때 동작: 무차감 재입장을 건너뛰고 일반 이용권 선택 화면으로 보낸다. 이용권만 있는 회원은 기존 자동 체크인 분기를 그대로 탄다.

## 영향 파일
- `app/src/main/java/com/bodyswitch/checkin/data/session/CheckinSettingsManager.kt`
- `app/src/main/java/com/bodyswitch/checkin/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/bodyswitch/checkin/ui/checkin/CheckinViewModel.kt`

## 단계
1. `CheckinSettingsManager`에 `recheckTicketOnReentry` 추가 (key `recheck_ticket_on_reentry`, 기본 false).
2. `SettingsScreen`에 "재입장" 섹션 + 토글 카드 추가. 기존 저장 버튼 흐름에 연결.
3. `CheckinViewModel.loadTickets`의 `canReentry` 산출에 설정을 AND로 건다. `canReentry`가 자동 재입장과 `continueEntry()` 양쪽에서 쓰이므로 이 한 곳이면 충분하다.

## 이중 차감 검토
- `COURSE_TICKET`: `attendReservation`이 이미 멱등. 이미 ATTENDED면 차감 0으로 로그만 남긴다. 안전.
- `COURSE_PASS`: 애초에 차감 없음. 안전.
- `TRIAL_TICKET` PASS+PERIOD: `deductTrialTicket`의 `noDeduction` 분기. 안전.
- `TRIAL_TICKET` PASS+COUNT: 입장마다 차감된다. 다만 운영에 상품 2건이 있을 뿐 **발급된 회원 일일권은 0건**이라 현재 위험 없음. 이 상품이 실제로 팔리기 시작하면 재검토 필요.

## 손대지 않는 것
- 백엔드 `computeReentry` / `reentry` API. 재입장 기능 자체는 그대로 둔다.
- 퇴실 흐름(`checkout()`).

## 검증
- `./gradlew assembleDebug`
- 수동 QA: 설정 OFF/ON × (당일 이용권 입장 후 재입장 / 당일 수업 출석 후 재입장)

## DoD
- [ ] 설정 저장 후 재진입 시 토글 유지
- [ ] OFF → 현행과 동일(바로 재입장)
- [ ] ON + 당일 입장 이력 → 이용권 선택 화면 노출
- [ ] ON + 이용권만 보유 → 기존대로 자동 체크인
- [ ] assembleDebug 빌드 성공

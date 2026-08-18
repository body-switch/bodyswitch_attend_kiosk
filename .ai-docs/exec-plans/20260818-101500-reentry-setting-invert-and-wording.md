# 재입장 설정 의미 반전 + 이용권 표시 문구 정리

## 목표
- "재입장 시 이용권 다시 확인" 토글을 **"이용권 확인 없이 바로 재입장"** 으로 뒤집는다.
  켜면 무차감 재입장, 끄면(기본) 이용권 선택 화면을 거친다.
- "당일 입장 가능한 것만 표시" 문구를 **"오늘 입장 가능한 상품만 표시"** 로 자연스럽게 고친다.

## 배경
사용자 결정(2026-08-18). 종전 토글은 켜야 "다시 확인"이라 기본값(꺼짐)이 무차감 통과였다.
반전 후 기본값(꺼짐)은 **재입장 때도 이용권 선택 화면**이 된다. 즉 기본 동작이 바뀐다.

## 영향 파일
- `data/session/CheckinSettingsManager.kt` — `recheckTicketOnReentry` → `skipTicketCheckOnReentry`,
  pref key `recheck_ticket_on_reentry` → `skip_ticket_check_on_reentry` (미출시 브랜치라 마이그레이션 불필요)
- `ui/checkin/CheckinViewModel.kt` — `canReentry` 조건에서 `!` 제거
- `ui/settings/SettingsScreen.kt` — 토글 2종 title/subtitle, 상태 변수명, 저장 라인

## 단계
1. 설정 매니저 프로퍼티·키 반전
2. 뷰모델 `canReentry` 조건 반전
3. 설정 화면 문구·변수명·저장 라인 반영
4. `:app:compileDebugKotlin --rerun-tasks` 통과 확인

## 검증
- ✅ `:app:compileDebugKotlin --rerun-tasks` BUILD SUCCESSFUL (기존 경고 2건 외 신규 없음)
- ⬜ 실기기 QA — 기본값(꺼짐)에서 재입장 시 이용권 선택 화면이 뜨는지, 켜면 바로 통과하는지

## 비대상
- 무차감 재입장 자격 판정(서버 `computeReentry`) 로직
- 퇴실 플로우, 만료 이용권 숨기기

# 키오스크 관리자 토큰 자동 갱신 (장시간 켜둔 태블릿 대응)

## 목표
키오스크 태블릿을 계속 켜두면 관리자 토큰(`X-Admin-Token`)이 7일 뒤 만료돼
안면등록/QR체크인 등에서 "지점 정보를 확인할 수 없습니다"가 뜬다.
요청 직전에 저장된 자동로그인 자격증명으로 토큰을 선제 재발급해 해소한다.

## 원인
- 관리자 JWT 만료 = `checkin-kiosk/application.yml` `app.jwt.expiration.minutes: 10080` (7일).
- 앱의 재로그인은 `LoginViewModel.init`에서 1회만 → 앱이 안 죽고 화면도 안 바뀌면(키오스크 상주)
  메모리의 `SessionManager.token`이 만료돼도 아무도 갱신하지 않음.
- 만료 토큰을 관리자 토큰으로 쓰는 곳: PhoneLogin, Checkin(QR·attend·reentry), AccessRegistration 등 다수.

## 설계 (중앙화 — OkHttp 인터셉터)
- `AdminTokenRefresher` (Singleton): JWT `exp` 디코드 → 만료 임박(1일 이내)/만료 시
  저장된 자동로그인 자격증명으로 **동기** 재로그인, `SessionManager` 갱신, 새 토큰 반환.
  재진입/데드락 방지를 위해 **별도 OkHttpClient**(인터셉터 없음) 기반 `KioskApi` 사용 + `synchronized`.
- `AdminTokenInterceptor` (Singleton): 요청이 관리자 토큰을 `X-Admin-Token` 또는
  `Authorization: Bearer <token>`로 실어보내고, 그 토큰이 만료 임박이면 refresher로 갱신 후 헤더 교체.
- `AppModule`: 메인 OkHttpClient에 인터셉터 등록 + 갱신 전용 bare Retrofit/Api 제공.

거부한 대안: (a) ViewModel마다 갱신 호출 → 호출부 누락 위험. (b) 401/406 반응형 재시도 →
406은 한글 메시지라 만료 판별 모호. JWT `exp` 기준이 확정적.

## 영향 파일
- 신규 `data/session/AdminTokenRefresher.kt`
- 신규 `data/network/AdminTokenInterceptor.kt`
- 수정 `di/AppModule.kt` (인터셉터 등록 + bare refreshApi 제공)

## 비대상
- 백엔드 토큰 만료 기간 변경(방식 A) 안 함.
- 로그인 화면/기존 자동로그인 UX 변경 안 함.

## 검증
- `./gradlew :app:compileDebugKotlin` (또는 assembleDebug) 컴파일 통과.
- 실기기 검증(만료 임박 토큰 주입 후 안면등록/QR 재발급 자동 성공)은 APK 배포 후 수동.

# Kiosk Admin API 명세서

> Base URL: `http://{server}:8082`
> Swagger: `http://{server}:8082/swagger-ui.html`

---

## 1. 관리자 로그인

### `POST /kiosk/api/v1/admin/login`

**Request Body:**
```json
{
  "username": "manager01",
  "password": "password123!"
}
```

**Response 200:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresAt": "2026-03-30 12:00:00",
  "userRole": "MANAGER",
  "username": "manager01",
  "name": "홍길동",
  "branchId": 102,
  "branchName": "강남점",
  "centerId": 100,
  "businessName": "바디스위치",
  "centerType": "BRANCH"
}
```

**허용 역할:** `OPERATOR`, `MANAGER`, `EMPLOYEE`, `GUEST`

| 코드 | 설명 |
|------|------|
| 401 | 아이디/비밀번호 불일치, 로그인 권한 없음 |
| 406 | 승인 대기/거절/탈퇴/비활성 계정 |

---

## 2. 지점 정보 조회

### `GET /kiosk/api/v1/admin/branch-info`

**Headers:** `Authorization: Bearer {관리자 토큰}`

**Response 200:**
```json
{
  "branchId": 102,
  "branchName": "강남점",
  "centerId": 100,
  "businessName": "바디스위치",
  "centerType": "BRANCH",
  "address": "서울특별시 강남구 테헤란로 123",
  "addressDetail": "4층 401호",
  "representativeNumber": "02-1234-5678"
}
```

---

## 3~5. 회원 로그인 (QR/전화번호/userId)

`X-Admin-Token` 헤더로 지점 검증. 다른 지점 → 406.

## 6. 이용권 목록 조회

`GET /kiosk/api/v1/checkin/tickets` (Bearer 회원토큰)

## 7. 체크인 차감

`POST /kiosk/api/v1/checkin` (Bearer 회원토큰)

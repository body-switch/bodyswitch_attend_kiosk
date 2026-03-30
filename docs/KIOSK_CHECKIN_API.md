# 키오스크 체크인 API 문서

## 전체 플로우

1. QR 스캔 → `POST /kiosk/api/v1/auth/qr-login` → JWT 토큰 발급
2. 이용권 조회 → `GET /kiosk/api/v1/checkin/tickets` → 회원 이용권 목록
3. 이용권 선택 → `POST /kiosk/api/v1/checkin` → 차감 처리

---

## 1. QR 로그인

```
POST /kiosk/api/v1/auth/qr-login
Content-Type: application/json
(인증 불필요)
```

### Request
```json
{
  "qrPayload": "Ywj4uchw8l|21baf71409b545bbcec7959aca4b2ffb"
}
```

### Response 200
```json
{
  "token": "eyJhbGci...",
  "pushYn": "Y",
  "memberId": "abc123",
  "userId": 15,
  "name": "김바디",
  "expiresAt": "2026-03-23 20:30:00"
}
```

---

## 2. 회원 보유 이용권 목록 조회

```
GET /kiosk/api/v1/checkin/tickets
Authorization: Bearer {token}
```

### Response 200
```json
{
  "memberId": "abc123",
  "memberName": "김바디",
  "courseTickets": [
    {
      "id": 101,
      "ticketName": "PT 개인레슨 30회",
      "usageCount": 30,
      "usedCount": 18,
      "remainCount": 12,
      "startDate": "2026-01-01",
      "expireDate": "2026-06-30",
      "ticketType": "COURSE_TICKET"
    },
    {
      "id": 102,
      "ticketName": "필라테스 그룹 20회",
      "usageCount": 20,
      "usedCount": 15,
      "remainCount": 5,
      "startDate": "2026-02-01",
      "expireDate": "2026-07-31",
      "ticketType": "COURSE_TICKET"
    }
  ],
  "trialTickets": [
    {
      "id": 201,
      "ticketName": "PT 체험 1회",
      "usageCount": 1,
      "usedCount": 0,
      "remainCount": 1,
      "startDate": "2026-03-20",
      "expireDate": "2026-03-27",
      "ticketType": "TRIAL_TICKET"
    }
  ],
  "coursePasses": [
    {
      "id": 301,
      "passName": "시설이용권 3개월",
      "startDate": "2026-01-01",
      "expireDate": "2026-03-31",
      "ticketType": "COURSE_PASS"
    }
  ]
}
```

---

## 3. 체크인 차감

```
POST /kiosk/api/v1/checkin
Authorization: Bearer {token}
Content-Type: application/json
```

### Request
```json
{
  "ticketType": "COURSE_TICKET",
  "ticketId": 101,
  "deductCount": 1
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| ticketType | String | O | COURSE_TICKET 또는 TRIAL_TICKET |
| ticketId | Long | O | 이용권 목록에서 받은 id |
| deductCount | Integer | X | 차감 횟수 (기본 1) |

### Response 200 (성공)
```json
{
  "success": true,
  "message": "체크인 완료",
  "ticketName": "PT 개인레슨 30회",
  "deductedCount": 1,
  "remainCount": 11,
  "usageCount": 30
}
```

### Response 406 (잔여횟수 부족)
```json
{
  "message": "잔여 횟수가 없습니다. 프론트에 문의해주세요.",
  "status": 406
}
```

---

## 에러 코드 정리

| 상태 | 상황 |
|------|------|
| 404 | 회원/이용권 없음 |
| 406 | 잔여횟수 부족, 비활성 이용권, 본인 이용권 아님 |
| 401 | 토큰 만료/미인증 |

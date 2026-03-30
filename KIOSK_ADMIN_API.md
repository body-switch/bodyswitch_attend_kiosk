# Kiosk Admin API 명세서

> Base URL: `http://{server}:8087`
> Swagger: `http://{server}:8087/swagger-ui.html`

---

## 1. 관리자 로그인

키오스크 진입 시 관리자(매니저/운영자/직원)가 먼저 로그인합니다.

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

**에러:**
| 코드 | 설명 |
|------|------|
| 401 | 아이디/비밀번호 불일치, 로그인 권한 없음 |
| 406 | 승인 대기/거절/탈퇴/비활성 계정 |

**중요:** 응답의 `token`을 저장해두고, 이후 회원 QR/전화번호 로그인 시 `X-Admin-Token` 헤더로 전달해야 합니다.

---

## 2. 지점 정보 조회 (사이드바용)

### `GET /kiosk/api/v1/admin/branch-info`

**Headers:**
```
Authorization: Bearer {관리자 토큰}
```

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

## 3. 회원 QR 로그인 (지점 검증 포함)

### `POST /kiosk/api/v1/auth/qr-login`

**Headers:**
```
X-Admin-Token: {관리자 토큰}
```

**Request Body:**
```json
{
  "qrPayload": "qrConfirmationToken|hmacSignature"
}
```

**Response 200:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "pushYn": "Y",
  "memberId": "M20260101001",
  "userId": 50,
  "name": "김회원",
  "expiresAt": "2026-03-30 12:00:00"
}
```

**에러:**
| 코드 | 설명 |
|------|------|
| 401 | QR 코드 유효하지 않음 |
| 406 | 해당 지점의 회원이 아닙니다 |

> `X-Admin-Token`이 없으면 지점 검증 없이 기존처럼 동작합니다.

---

## 4. 회원 전화번호 로그인 (지점 검증 포함)

### `POST /kiosk/api/v1/auth/phone-login`

**Headers:**
```
X-Admin-Token: {관리자 토큰}
```

**Request Body:**
```json
{
  "phoneNumber": "010-1234-5678"
}
```

**Response 200:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "pushYn": "Y",
  "memberId": "M20260101001",
  "userId": 50,
  "name": "김회원",
  "expiresAt": "2026-03-30 12:00:00"
}
```

**에러:**
| 코드 | 설명 |
|------|------|
| 404 | 등록된 회원 없음 |
| 406 | 해당 지점의 회원이 아닙니다 |

---

## 5. 회원 userId 로그인 (지점 검증 포함)

### `POST /kiosk/api/v1/auth/uid-login`

**Headers:**
```
X-Admin-Token: {관리자 토큰}
```

**Request Body:**
```json
{
  "userId": "50"
}
```

**Response:** QR 로그인과 동일

---

## 6. 회원 이용권 목록 조회 (체크인 화면)

### `GET /kiosk/api/v1/checkin/tickets?branchId={branchId}`

**Headers:**
```
Authorization: Bearer {회원 토큰}
```

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| branchId | Long | O | 키오스크가 설치된 지점 ID |

> 해당 지점의 수강권/체험권/이용권만 조회됩니다. `ACTIVE` 상태인 이용권만 반환됩니다 (만료, 환불, 정지, 양도된 이용권 제외).

**Response 200:**
```json
{
  "memberId": "M20260101001",
  "memberName": "김회원",
  "courseTickets": [
    {
      "id": 1,
      "ticketName": "PT 30회",
      "usageCount": 30,
      "usedCount": 10,
      "remainCount": 20,
      "startDate": "2026-01-01",
      "expireDate": "2026-06-30",
      "ticketType": "COURSE_TICKET",
      "status": "ACTIVE"
    }
  ],
  "trialTickets": [
    {
      "id": 5,
      "ticketName": "체험 3회권",
      "usageCount": 3,
      "usedCount": 1,
      "remainCount": 2,
      "startDate": "2026-03-01",
      "expireDate": "2026-03-31",
      "ticketType": "TRIAL_TICKET",
      "status": "ACTIVE"
    }
  ],
  "coursePasses": [
    {
      "id": 10,
      "passName": "1개월 이용권",
      "startDate": "2026-03-01",
      "expireDate": "2026-03-31",
      "ticketType": "COURSE_PASS",
      "status": "ACTIVE"
    }
  ]
}
```

---

## 7. 체크인 차감

### `POST /kiosk/api/v1/checkin`

**Headers:**
```
Authorization: Bearer {회원 토큰}
```

**Request Body:**
```json
{
  "branchId": 104,
  "ticketType": "COURSE_TICKET",
  "ticketId": 1,
  "deductCount": 1,
  "checkInMethod": "QR"
}
```

- `branchId`: 키오스크가 설치된 지점 ID (필수)
- `ticketType`: `COURSE_TICKET`, `TRIAL_TICKET`, `COURSE_PASS` 중 하나 (필수)
- `ticketId`: 이용권 ID (위 목록 조회에서 받은 `id`) (필수)
- `deductCount`: 차감 횟수 (기본 1)
- `checkInMethod`: `QR` 또는 `PHONE` (필수)

**Response 200:**
```json
{
  "success": true,
  "message": "체크인 완료",
  "ticketName": "PT 30회",
  "deductedCount": 1,
  "remainCount": 19,
  "usageCount": 30
}
```

**에러:**
| 코드 | 설명 |
|------|------|
| 404 | 이용권 없음 |
| 406 | 잔여 횟수 부족 / 비활성 이용권 |

---

## 8. 지점 체크인 이력 조회

지점 전체의 키오스크 체크인(QR/전화번호) 이력을 조회합니다. (`check_in_log_history` 기반)

### `GET /kiosk/api/v1/checkin/history`

**Headers:**
```
Authorization: Bearer {관리자 토큰}
```

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `branchId` | Long | N | 지점 ID |
| `searchInput` | String | N | 검색어 (회원명 또는 전화번호) |
| `status` | String | N | 상태 필터 (`APPROVED`, `ERROR`, `CANCELED`) |
| `startDate` | String (yyyy-MM-dd) | N | 조회 시작일 |
| `endDate` | String (yyyy-MM-dd) | N | 조회 종료일 |
| `page` | Integer | N | 페이지 번호 (기본: 1) |
| `limit` | Integer | N | 페이지 크기 (기본: 10) |

**Response 200:**
```json
{
  "totalElements": 150,
  "number": 1,
  "size": 10,
  "content": [
    {
      "id": 101,
      "memberId": "M20260101001",
      "memberName": "김회원",
      "phoneNumber": "010-1234-5678",
      "checkInMethod": "QR",
      "ticketType": "COURSE_TICKET",
      "ticketName": "PT 30회",
      "deductCount": 1,
      "remainCount": 19,
      "status": "APPROVED",
      "writeDate": "2026-03-24",
      "writeTime": "10:30:00"
    },
    {
      "id": 98,
      "memberId": "M20260101002",
      "memberName": "이영희",
      "phoneNumber": "010-9876-5432",
      "checkInMethod": "PHONE",
      "ticketType": "COURSE_PASS",
      "ticketName": "1개월 이용권",
      "deductCount": null,
      "remainCount": null,
      "status": "APPROVED",
      "writeDate": "2026-03-24",
      "writeTime": "09:15:00"
    }
  ]
}
```

> `COURSE_PASS`는 차감이 없으므로 `deductCount`, `remainCount`가 `null`입니다.

---

## 9. 지점 출석 기록 조회

지점 전체의 일일 출석 기록을 조회합니다. (`member_attend_history` 기반, 안면인식 + QR/전화번호 통합)

### `GET /kiosk/api/v1/checkin/attendance`

**Headers:**
```
Authorization: Bearer {관리자 토큰}
```

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `branchId` | Long | N | 지점 ID |
| `searchInput` | String | N | 검색어 (회원명 또는 전화번호) |
| `startDate` | String (yyyy-MM-dd) | N | 조회 시작일 |
| `endDate` | String (yyyy-MM-dd) | N | 조회 종료일 |
| `page` | Integer | N | 페이지 번호 (기본: 1) |
| `limit` | Integer | N | 페이지 크기 (기본: 10) |

**Response 200:**
```json
{
  "totalElements": 45,
  "number": 1,
  "size": 10,
  "content": [
    {
      "id": 456,
      "memberId": "M20260101001",
      "memberName": "김회원",
      "phoneNumber": "010-1234-5678",
      "branchName": "강남점",
      "attendType": "입실",
      "attendCount": 1,
      "writeDate": "2026-03-24",
      "startTime": "10:30:00",
      "endTime": null
    },
    {
      "id": 450,
      "memberId": "M20260101002",
      "memberName": "이영희",
      "phoneNumber": "010-9876-5432",
      "branchName": "강남점",
      "attendType": "입실",
      "attendCount": 2,
      "writeDate": "2026-03-23",
      "startTime": "09:00:00",
      "endTime": null
    }
  ]
}
```

> `attendCount`는 당일 누적 입장 횟수입니다. 같은 날 재입장 시 증가합니다.
> `endTime`은 퇴장 시간으로, 안면인식 퇴장 단말기를 통해서만 기록됩니다.

---

## 10. 체크인 취소

관리자가 잘못된 체크인을 취소합니다. 상태가 `CANCELED`로 변경됩니다.

### `POST /kiosk/api/v1/checkin/cancel`

**Headers:**
```
Authorization: Bearer {관리자 토큰}
X-Admin-Token: {관리자 토큰}
```

**Request Body:**
```json
{
  "checkInLogId": 101
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "체크인이 취소되었습니다.",
  "checkInLogId": 101,
  "status": "CANCELED"
}
```

**에러:**
| 코드 | 설명 |
|------|------|
| 404 | 체크인 로그 없음 |

> 취소 시 이용권 횟수는 자동 복원되지 않습니다. 필요 시 관리자 페이지에서 수동 조정해야 합니다.

---

## 11. 오늘 예약 수업 조회 (수강권/체험권 선택 시)

수강권 또는 체험권을 선택하면, 해당 이용권으로 오늘 예약된 수업 목록을 조회합니다.

### `GET /kiosk/api/v1/checkin/reservations`

**Headers:**
```
Authorization: Bearer {회원 토큰}
```

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `branchId` | Long | O | 키오스크가 설치된 지점 ID |
| `ticketType` | String | O | `COURSE_TICKET` 또는 `TRIAL_TICKET` |
| `ticketId` | Long | O | 회원 수강권/체험권 ID (이용권 목록의 `id`) |

**Response 200:**
```json
{
  "memberId": "M20260101001",
  "memberName": "김회원",
  "ticketType": "COURSE_TICKET",
  "ticketId": 1,
  "ticketName": "PT 30회",
  "reservations": [
    {
      "reservationId": 567,
      "courseClassName": "오전 PT",
      "classDate": "2026-03-25",
      "startTime": "10:00",
      "endTime": "10:50",
      "roomName": "A룸",
      "employeeName": "박트레이너",
      "classType": "PRIVATE_LESSON",
      "status": "RESERVED",
      "ticketName": "PT 30회"
    },
    {
      "reservationId": 568,
      "courseClassName": "오후 PT",
      "classDate": "2026-03-25",
      "startTime": "14:00",
      "endTime": "14:50",
      "roomName": "B룸",
      "employeeName": "박트레이너",
      "classType": "PRIVATE_LESSON",
      "status": "ATTENDED",
      "ticketName": "PT 30회"
    }
  ]
}
```

> `status`가 `ATTENDED`이면 이미 출석 완료된 수업입니다.
> `reservations`가 빈 배열이면 오늘 예약된 수업이 없습니다 → 기존 체크인 차감 플로우 사용.

**에러:**
| 코드 | 설명 |
|------|------|
| 404 | 회원/수강권/체험권 없음 |
| 406 | 본인의 이용권이 아님, COURSE_PASS는 지원하지 않음 |

---

## 12. 예약 기반 출석 처리

예약된 수업을 선택하여 출석 처리합니다. 출석 + 수강권/체험권 차감이 동시에 이루어집니다.
이미 출석된 수업이면 차감 없이 출입만 허용됩니다.

### `POST /kiosk/api/v1/checkin/attend`

**Headers:**
```
Authorization: Bearer {회원 토큰}
X-Admin-Token: {관리자 토큰}
```

**Request Body:**
```json
{
  "branchId": 104,
  "reservationId": 567,
  "checkInMethod": "QR"
}
```

- `branchId`: 키오스크가 설치된 지점 ID (필수)
- `reservationId`: 예약 ID (위 예약 조회에서 받은 `reservationId`) (필수)
- `checkInMethod`: `QR` 또는 `PHONE` (필수)

**Response 200 (신규 출석):**
```json
{
  "success": true,
  "message": "출석 및 차감 완료",
  "alreadyAttended": false,
  "ticketName": "PT 30회",
  "deductedCount": 1,
  "remainCount": 19,
  "usageCount": 30,
  "reservationInfo": {
    "reservationId": 567,
    "courseClassName": "오전 PT",
    "classDate": "2026-03-25",
    "startTime": "10:00",
    "endTime": "10:50",
    "roomName": "A룸",
    "employeeName": "박트레이너",
    "classType": "PRIVATE_LESSON",
    "status": "ATTENDED",
    "ticketName": "PT 30회"
  }
}
```

**Response 200 (이미 출석된 수업):**
```json
{
  "success": true,
  "message": "이미 출석된 수업입니다. 출입이 허용됩니다.",
  "alreadyAttended": true,
  "ticketName": "PT 30회",
  "deductedCount": null,
  "remainCount": 19,
  "usageCount": 30,
  "reservationInfo": {
    "reservationId": 567,
    "courseClassName": "오전 PT",
    "classDate": "2026-03-25",
    "startTime": "10:00",
    "endTime": "10:50",
    "roomName": "A룸",
    "employeeName": "박트레이너",
    "classType": "PRIVATE_LESSON",
    "status": "ATTENDED",
    "ticketName": "PT 30회"
  }
}
```

> `alreadyAttended`가 `true`이면 이미 출석된 수업이므로 차감 없이 출입만 처리됩니다.

**에러:**
| 코드 | 설명 |
|------|------|
| 404 | 예약/회원 없음 |
| 406 | 본인의 예약이 아님, 오늘 수업 아님, 출석 불가 상태 |

---

## 13. 직원 호출 알림톡 발송

앱 설정에 저장된 직원 전화번호로 알림톡을 발송합니다.

### `POST /kiosk/api/v1/branches/manager/call`

**Request Body:**
```json
{
  "phoneNumber": "010-1234-5678"
}
```

- `phoneNumber`: 앱 설정에서 저장해둔 직원 전화번호 (필수)

**Response 201:**
```json
{
  "message": "성공적으로 처리되었습니다."
}
```

> DB `kakao_template` 테이블에 `code = 'G200'`, `use_yn = 'Y'` 레코드가 있어야 알림톡이 발송됩니다.
> 템플릿이 없으면 응답은 201이지만 알림톡은 발송되지 않습니다.

---

## 전체 플로우

```
1. 관리자 로그인
   POST /kiosk/api/v1/admin/login
   → adminToken 저장

2. (선택) 사이드바 지점 정보 조회
   GET /kiosk/api/v1/admin/branch-info
   Authorization: Bearer {adminToken}

3. 회원 QR/전화번호 로그인
   POST /kiosk/api/v1/auth/qr-login (또는 /phone-login)
   X-Admin-Token: {adminToken}
   → memberToken 저장
   → 다른 지점 회원이면 406 에러

4. 회원 이용권 목록 조회 (해당 지점 이용권만, 만료 포함)
   GET /kiosk/api/v1/checkin/tickets?branchId={branchId}
   Authorization: Bearer {memberToken}

5-A. [수강권/체험권] 예약 기반 출석 플로우 (권장)
   5-A-1. 오늘 예약 수업 조회
          GET /kiosk/api/v1/checkin/reservations?branchId={branchId}&ticketType=COURSE_TICKET&ticketId={ticketId}
          Authorization: Bearer {memberToken}
          → 예약 목록 반환

   5-A-2a. 예약이 있으면 → 수업 선택 → 출석 처리
           POST /kiosk/api/v1/checkin/attend
           Authorization: Bearer {memberToken}
           X-Admin-Token: {adminToken}
           Body: { branchId, reservationId, checkInMethod }
           → 출석 + 차감 + 출입 (이미 출석이면 차감 없이 출입)

   5-A-2b. 예약이 없으면 → 기존 체크인 차감
           POST /kiosk/api/v1/checkin
           Body: { branchId, ticketType, ticketId, deductCount, checkInMethod }

5-B. [이용권(COURSE_PASS)] 기존 체크인 플로우
   POST /kiosk/api/v1/checkin
   Authorization: Bearer {memberToken}
   Body: { branchId, ticketType: "COURSE_PASS", ticketId, deductCount, checkInMethod }

6. (선택) 지점 체크인 이력 조회
   GET /kiosk/api/v1/checkin/history?branchId={branchId}&page=1&limit=10
   Authorization: Bearer {adminToken}

7. (선택) 지점 출석 기록 조회
   GET /kiosk/api/v1/checkin/attendance?branchId={branchId}&page=1&limit=10
   Authorization: Bearer {adminToken}

8. (선택) 체크인 취소 (관리자)
   POST /kiosk/api/v1/checkin/cancel
   Authorization: Bearer {adminToken}
   Body: { checkInLogId }
```

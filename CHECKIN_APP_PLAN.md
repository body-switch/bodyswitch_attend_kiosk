# AI 기반 회원 출입 체크인 및 이용권 관리 시스템

## 기술 스택

| 항목 | 선택 | 비고 |
|------|------|------|
| **언어** | Kotlin | Android 네이티브 |
| **최소 SDK** | API 26 (Android 8.0) | |
| **UI** | Jetpack Compose + Material3 | 모던 선언형 UI |
| **카메라/QR** | CameraX + ML Kit (Barcode Scanning) | 실시간 스캔 + 이미지 스캔 |
| **네트워크** | Retrofit2 + OkHttp | REST API 통신 |
| **DI** | Hilt | 의존성 주입 |
| **로컬 DB** | Room | 오프라인 체크인 로그 캐싱 |
| **인증** | Firebase Auth (SMS) | 전화번호 인증 |
| **아키텍처** | MVVM + Clean Architecture | |

---

## 구현 Phase 로드맵

### Phase 1: QR 스캔 (MVP - 최우선)
- QR 카메라 실시간 스캔 (CameraX + ML Kit)
- QR 이미지 파일 스캔 (갤러리에서 선택)
- 스캔 결과 UI (회원 ID 파싱 + 표시)
- 성공/실패 피드백 (소리, 진동)

### Phase 2: 인증 및 사용자 관리
- 로그인/로그아웃 (이메일 또는 ID 기반)
- 전화번호 SMS 인증 (Firebase Auth)
- 사용자 세션 관리

### Phase 3: 체크인 + 이용권 차감
- 백엔드 API 연동 (체크인 요청 → 회원 정보 응답)
- 이용권 상태 확인 (수강 중 / 만료 / 정지)
- 횟수권: 체크인 시 1회 차감
- 기간권: 유효 기간 내 출입 허용 확인
- 중복 체크인 방지

### Phase 4: 로그 및 관리
- 출입 로그 기록 (시간, 위치, 차감 내역)
- 오늘 입출입 내역 리스트
- 현재 재실 회원 목록
- 수동 체크인 (이름/전화번호 검색)

---

## 프로젝트 구조

```
checkin-app/
├── app/
│   └── src/main/
│       ├── java/com/bodyswitch/checkin/
│       │   ├── App.kt                          # Application 클래스 (Hilt)
│       │   ├── MainActivity.kt
│       │   │
│       │   ├── di/                             # Hilt 모듈
│       │   │   ├── AppModule.kt
│       │   │   └── NetworkModule.kt
│       │   │
│       │   ├── data/
│       │   │   ├── api/                        # Retrofit API 인터페이스
│       │   │   │   ├── AuthApi.kt
│       │   │   │   └── CheckinApi.kt
│       │   │   ├── model/                      # 데이터 모델 (DTO)
│       │   │   │   ├── MemberResponse.kt
│       │   │   │   ├── CheckinRequest.kt
│       │   │   │   └── TicketInfo.kt
│       │   │   ├── local/                      # Room DB
│       │   │   │   ├── CheckinDatabase.kt
│       │   │   │   └── CheckinLogDao.kt
│       │   │   └── repository/
│       │   │       ├── AuthRepository.kt
│       │   │       └── CheckinRepository.kt
│       │   │
│       │   ├── domain/                         # 비즈니스 로직
│       │   │   ├── usecase/
│       │   │   │   ├── ProcessCheckinUseCase.kt
│       │   │   │   └── DeductTicketUseCase.kt
│       │   │   └── model/                      # 도메인 모델
│       │   │       ├── Member.kt
│       │   │       └── Ticket.kt
│       │   │
│       │   └── ui/
│       │       ├── navigation/
│       │       │   └── NavGraph.kt
│       │       ├── theme/
│       │       │   └── Theme.kt
│       │       ├── scanner/                    # QR 스캔 화면
│       │       │   ├── ScannerScreen.kt
│       │       │   ├── ScannerViewModel.kt
│       │       │   ├── CameraPreview.kt        # CameraX 프리뷰
│       │       │   └── ImageQrReader.kt        # 갤러리 QR 읽기
│       │       ├── result/                     # 스캔 결과 화면
│       │       │   ├── ResultScreen.kt
│       │       │   └── ResultViewModel.kt
│       │       ├── auth/                       # 로그인 화면
│       │       │   ├── LoginScreen.kt
│       │       │   └── AuthViewModel.kt
│       │       └── log/                        # 출입 내역 화면
│       │           ├── LogScreen.kt
│       │           └── LogViewModel.kt
│       │
│       ├── res/
│       │   ├── raw/
│       │   │   └── beep.mp3                    # 스캔 성공 효과음
│       │   └── values/
│       │       └── strings.xml
│       └── AndroidManifest.xml
│
├── build.gradle.kts                            # 프로젝트 레벨
├── app/build.gradle.kts                        # 앱 레벨
└── gradle/libs.versions.toml                   # 버전 카탈로그
```

---

## Phase 1 상세 구현 계획

### Step 1: 프로젝트 초기 셋업
- Android Studio 프로젝트 생성 (Compose 템플릿)
- Gradle 의존성 추가 (CameraX, ML Kit, Hilt, Navigation)
- 기본 테마 및 네비게이션 구성

### Step 2: QR 카메라 실시간 스캔
```kotlin
// CameraX + ML Kit 핵심 흐름
val barcodeScanner = BarcodeScanning.getClient(
    BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
)

// CameraX ImageAnalysis → ML Kit으로 프레임 전달
imageAnalysis.setAnalyzer(executor) { imageProxy ->
    val inputImage = InputImage.fromMediaImage(
        imageProxy.image!!, imageProxy.imageInfo.rotationDegrees
    )
    barcodeScanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            barcodes.firstOrNull()?.rawValue?.let { qrData ->
                // 회원 ID 파싱 → 체크인 처리
            }
        }
}
```

### Step 3: QR 이미지 파일 스캔
```kotlin
// 갤러리에서 이미지 선택 → ML Kit으로 QR 디코딩
val pickImage = rememberLauncherForActivityResult(
    ActivityResultContracts.GetContent()
) { uri ->
    uri?.let {
        val inputImage = InputImage.fromFilePath(context, it)
        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                // 동일한 QR 처리 로직
            }
    }
}
```

### Step 4: 스캔 결과 UI
- 회원 정보 카드 (이름, 상태, 이용권 정보)
- 체크인 성공/실패 애니메이션
- 재스캔 버튼

### Step 5: 피드백 (소리, 진동)
- `MediaPlayer`로 비프음 재생
- `Vibrator`로 진동 피드백

---

## 핵심 의존성 (libs.versions.toml)

```toml
[versions]
kotlin = "2.0.21"
compose-bom = "2024.12.01"
camerax = "1.4.1"
mlkit-barcode = "17.3.0"
hilt = "2.53.1"
retrofit = "2.11.0"
room = "2.6.1"
navigation = "2.8.5"

[libraries]
# Compose
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-ui = { module = "androidx.compose.ui:ui" }

# CameraX
camerax-core = { module = "androidx.camera:camera-core", version.ref = "camerax" }
camerax-camera2 = { module = "androidx.camera:camera-camera2", version.ref = "camerax" }
camerax-lifecycle = { module = "androidx.camera:camera-lifecycle", version.ref = "camerax" }
camerax-view = { module = "androidx.camera:camera-view", version.ref = "camerax" }

# ML Kit
mlkit-barcode = { module = "com.google.mlkit:barcode-scanning", version.ref = "mlkit-barcode" }

# Network
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-gson = { module = "com.squareup.retrofit2:converter-gson", version.ref = "retrofit" }

# DI
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }

# Room
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }

# Navigation
navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
```

---

## 시작 명령

```bash
# Android Studio에서 프로젝트 생성 후
# 1. Empty Compose Activity 템플릿 선택
# 2. Package: com.bodyswitch.checkin
# 3. Minimum SDK: API 26
# 4. Build configuration language: Kotlin DSL
```

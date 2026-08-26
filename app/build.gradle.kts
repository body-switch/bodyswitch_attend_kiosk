import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// 체크인앱 서버 주소.
// 2026-08-04: api-dev.bodyswitch.co.kr(dev서버 116.44.106.81:8084) → 운영서버로 전환.
// 운영 Apache 가 /kiosk 를 8097(blue)/8077(green) 로 프록시한다.
// ⚠️ 경로 프리픽스 /kiosk 는 KioskApi 의 각 엔드포인트에 이미 들어 있다 (kiosk/api/v1/...).
val PROD_BASE_URL = "https://api.bodyswitch.co.kr/"

// local.properties 의 checkin.baseUrl (있으면 debug 빌드가 이 주소를 본다)
val localBaseUrl: String? = run {
    val file = rootProject.file("local.properties")
    if (!file.exists()) return@run null
    val props = Properties()
    file.inputStream().use { props.load(it) }
    props.getProperty("checkin.baseUrl")?.takeIf { it.isNotBlank() }
}

android {
    namespace = "com.bodyswitch.checkin"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bodyswitch.checkin"
        minSdk = 26
        targetSdk = 36
        versionCode = 11
        versionName = "1.1.0"
    }

    buildTypes {
        debug {
            // 로컬 백엔드를 보려면 local.properties 에 아래 한 줄을 넣는다 (gitignore 대상).
            //   checkin.baseUrl=http://10.0.2.2:8087/
            // 10.0.2.2 는 안드로이드 에뮬레이터에서 호스트 PC(localhost)를 가리키는 주소다.
            // 실기기라면 PC 의 LAN IP 를 쓰고 방화벽에서 8087 을 열어야 한다.
            // 값을 안 넣으면 release 와 동일하게 운영 서버를 본다.
            buildConfigField("String", "BASE_URL", "\"${localBaseUrl ?: PROD_BASE_URL}\"")
            // 로컬 서버는 https 가 아니라 평문 http 라, debug 빌드에서만 허용한다.
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            buildConfigField("String", "BASE_URL", "\"$PROD_BASE_URL\"")
            manifestPlaceholders["usesCleartextTraffic"] = "false"

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.foundation.layout)
    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // AndroidX
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // ML Kit Barcode
    implementation(libs.mlkit.barcode)
    implementation(libs.mlkit.face)

    // ZXing (QR 코드 생성)
    implementation(libs.zxing.core)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    ksp(libs.moshi.codegen)

    // Permissions
    implementation(libs.accompanist.permissions)

    // Splash
    implementation(libs.splashscreen)
}

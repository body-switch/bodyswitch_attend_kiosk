import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// 체크인앱 서버 주소. "dev" 가 붙어 있지만 운영 서버다 (AppModule.kt 주석 참조).
val PROD_BASE_URL = "https://api-dev.bodyswitch.co.kr/"

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
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bodyswitch.checkin"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.0.7"
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

    lint {
        // NullSafeMutableLiveData 디텍터가 IncompatibleClassChangeError로 죽어
        // lintVitalAnalyzeRelease가 실패하고 릴리스 빌드가 끝나지 않는다.
        // 우리 코드 문제가 아니다 (프로젝트에 MutableLiveData 사용 0건).
        // AGP/lint 버전 호환 이슈이므로 이 규칙만 끈다. 나머지 lint 검사는 그대로 돈다.
        disable += "NullSafeMutableLiveData"
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

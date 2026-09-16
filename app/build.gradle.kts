import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// local.properties(VCS 제외)에서 기본 제공용 키 로드. 미존재 시 빈 문자열(사용자 입력 키만 사용).
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val dartDefaultKey: String = localProperties.getProperty("dart.default.key", "")

android {
    namespace = "com.kitwlshcom.kdailyutil"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kitwlshcom.kdailyutil"
        minSdk = 26

        targetSdk = 36
        // 버전 스킴 = 유의적 버전(MAJOR.MINOR.PATCH). versionCode와 **묶지 않는다**.
        //   versionCode = Play가 보는 정수(업로드마다 증가, 사용자에게 안 보임)
        //   versionName = 사람이 보는 문자열. 버그수정→PATCH / 기능추가→MINOR / 호환깨짐→MAJOR
        // 상세·전환 배경 = doc/DEVELOPER_GUIDE.md '버전 스킴'
        // ⚠️ 1.7.1은 스킴상 PATCH지만 이번 판에는 기능 추가(지문 카드 시작 버튼 ·
        //    마지막 훈련 기억 · 결과 화면 「다시·다음」)가 섞여 있다. 규칙대로면 1.8.0인데
        //    **사용자가 1.7.1로 하기로 결정했다**(2026-09-08). 스킴이 바뀐 것은 아니다.
        versionCode = 9
        versionName = "1.7.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "DART_DEFAULT_KEY", "\"$dartDefaultKey\"")
    }

    // 릴리즈 서명: local.properties(VCS 제외)에 키 정보가 있을 때만 활성화.
    // 필요한 키: release.store.file / release.store.password / release.key.alias / release.key.password
    val hasReleaseSigning = listOf(
        "release.store.file", "release.store.password", "release.key.alias", "release.key.password"
    ).all { !localProperties.getProperty(it).isNullOrBlank() }
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(localProperties.getProperty("release.store.file"))
                storePassword = localProperties.getProperty("release.store.password")
                keyAlias = localProperties.getProperty("release.key.alias")
                keyPassword = localProperties.getProperty("release.key.password")
            }
        }
    }

    buildTypes {
        // 🔴 **디버그는 다른 앱으로 깔린다**(2026-09-16).
        //    개발용 빌드는 스토어 앱과 **서명이 달라** 덮어쓰기가 안 된다. 그래서 예전에는
        //    실기기 확인을 하려면 **스토어 앱을 지워야** 했고, 그때마다 사용자의
        //    출석·연속·기록·보관함 지문·AI 키가 통째로 날아갔다(연속 10일이 실제로 걸려 있었다).
        //    패키지명만 갈라 두면 둘이 **나란히** 깔려 그 대가가 사라진다.
        //    ⚠️ release 블록은 건드리지 않는다 — **출하되는 앱의 패키지명은 그대로**다.
        //    ⚠️ FileProvider authority도 `${applicationId}`로 따라가야 한다(매니페스트) —
        //       고정 문자열이면 두 앱이 같은 authority를 선언해 **설치가 거부된다**.
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            // 🔴 이게 없으면 `android.util.Log`가 단위 테스트에서 «not mocked» 예외를 던진다.
            // 진단 로그를 넣은 코드는 그 순간부터 테스트가 불가능해진다 — 로그를 지우는 게 아니라
            // 이 스위치를 켜는 것이 답이다(K장부 `doc/OPERATIONS.md` §3-4에서 같은 함정을 이미 풀었다).
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.jsoup)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.generative.ai)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.coil.compose)
    implementation(libs.androidx.documentfile)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
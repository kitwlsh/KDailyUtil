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
        versionCode = 7
        versionName = "1.6.1"

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
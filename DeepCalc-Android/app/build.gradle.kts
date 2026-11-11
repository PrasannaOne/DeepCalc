plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ABI APK splits are disabled only for the AAB-only build invocation.
// This keeps the AAB clean while the APK build still emits every ABI.
val abiSplitsEnabled = providers.gradleProperty("deepcalc.abiSplits")
    .map { it.toBoolean() }
    .getOrElse(true)

android {
    namespace = "com.prasannaone.deepcalc"
    compileSdk = 36

    val releaseKeyStore = System.getenv("DEEPCALC_KEYSTORE_FILE")
    val releaseSigningAvailable = !releaseKeyStore.isNullOrBlank()

    signingConfigs {
        create("deepcalcRelease") {
            if (releaseSigningAvailable) {
                storeFile = file(releaseKeyStore!!)
                storePassword = System.getenv("DEEPCALC_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("DEEPCALC_KEY_ALIAS")
                keyPassword = System.getenv("DEEPCALC_KEY_PASSWORD")
            }
        }
    }

    defaultConfig {
        applicationId = "com.prasannaone.deepcalc"
        minSdk = 23
        targetSdk = 36
        versionCode = 3
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Keep the original device coverage while producing focused downloads.
    // The universal APK contains all four ABIs; split APKs contain one each.
    splits {
        abi {
            isEnable = abiSplitsEnabled
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (releaseSigningAvailable) signingConfig = signingConfigs.getByName("deepcalcRelease")
        }
    }

    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}

// AGP 9 provides Kotlin compilation itself. Keep the bytecode target at Java 17
// so the app remains compatible with its minSdk while CI runs on JDK 21.
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

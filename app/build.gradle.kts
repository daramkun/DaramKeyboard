plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.daram.keyboard"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.daram.keyboard"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
}

// nutcracker is a KMP library; exclude non-Android native platform artifacts
// that JitPack includes in its metadata but are incompatible with Android builds.
configurations.all {
    exclude(group = "com.github.daramkun.nutcracker", module = "nutcracker-iosarm64")
    exclude(group = "com.github.daramkun.nutcracker", module = "nutcracker-iossimulatorarm64")
    exclude(group = "com.github.daramkun.nutcracker", module = "nutcracker-iosx64")
    exclude(group = "com.github.daramkun.nutcracker", module = "nutcracker-macosarm64")
    exclude(group = "com.github.daramkun.nutcracker", module = "nutcracker-macosx64")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.recyclerview)
    implementation("com.github.daramkun:nutcracker:main-SNAPSHOT")
}

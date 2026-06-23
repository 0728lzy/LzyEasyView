plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.lzylym.lzyeasyview"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lzylym.lzyeasyview"
        minSdk = 21
        targetSdk = 34
        versionCode = 100
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        apiVersion = "1.6"
        languageVersion = "1.6"
    }
}

dependencies {
    // AndroidX 核心
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.2")

    // Material 组件
    implementation("com.google.android.material:material:1.6.0")

    // 测试
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}

plugins {
    id("com.android.application")
}

android {
    namespace = "com.nijatva.file"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nijatva.file"
        minSdk = 23
        targetSdk = 34
        versionCode = 32
        versionName = "32.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Empty default = runs 100% standalone from bundled assets inside the APK (no external server needed).
// Optional: pass -PAPP_URL=https://your-public-domain.com/?tv=1 to point to a custom hosted backend.
val appUrl: String = (project.findProperty("APP_URL") as String?)?.trim().orEmpty()

android {
    namespace = "com.mamxanhtube.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mamxanhtube.tv"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"

        buildConfigField("String", "APP_URL", "\"$appUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.12.1")
}

import java.util.Properties

val localProps = Properties().also { props ->
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { props.load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.appdistribution)
}

android {
    namespace = "com.subtranslate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.subtranslate"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "1.7"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val geminiApiKey = System.getenv("GEMINI_API_KEY") ?: localProps["GEMINI_API_KEY"] ?: ""
        val googleTranslateApiKey = System.getenv("GOOGLE_TRANSLATE_API_KEY") ?: localProps["GOOGLE_TRANSLATE_API_KEY"] ?: ""
        // Key already in preview/index.html (public repo) — fallback so CI builds work without the secret
        val opensubtitlesApiKey = System.getenv("OPENSUBTITLES_API_KEY")
            ?: localProps["OPENSUBTITLES_API_KEY"]?.toString()
            ?: ""

        // Key already in preview/server.js (public repo) — fallback so CI works without the secret
        val subdlApiKey = System.getenv("SUBDL_API_KEY")
            ?: localProps["SUBDL_API_KEY"]?.toString()
            ?: ""

        val osUsername = System.getenv("OPENSUBTITLES_USERNAME")
            ?: localProps["OPENSUBTITLES_USERNAME"]?.toString()
            ?: ""
        val osPassword = System.getenv("OPENSUBTITLES_PASSWORD")
            ?: localProps["OPENSUBTITLES_PASSWORD"]?.toString()
            ?: ""

        val microsoftApiKey = System.getenv("MICROSOFT_API_KEY")
            ?: localProps["MICROSOFT_API_KEY"]?.toString()
            ?: ""
        val microsoftRegion = System.getenv("MICROSOFT_REGION")
            ?: localProps["MICROSOFT_REGION"]?.toString()
            ?: "global"

        buildConfigField("String", "OPENSUBTITLES_API_KEY", "\"$opensubtitlesApiKey\"")
        buildConfigField("String", "SUBDL_API_KEY", "\"$subdlApiKey\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "GOOGLE_TRANSLATE_API_KEY", "\"$googleTranslateApiKey\"")
        buildConfigField("String", "OPENSUBTITLES_USERNAME", "\"$osUsername\"")
        buildConfigField("String", "OPENSUBTITLES_PASSWORD", "\"$osPassword\"")
        buildConfigField("String", "MICROSOFT_API_KEY", "\"$microsoftApiKey\"")
        buildConfigField("String", "MICROSOFT_REGION", "\"$microsoftRegion\"")

    }

    signingConfigs {
        create("release") {
            val ksPath = System.getenv("KEYSTORE_PATH") ?: localProps["KEYSTORE_PATH"]?.toString()
            if (!ksPath.isNullOrBlank()) storeFile = file(ksPath)
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: localProps["KEYSTORE_PASSWORD"]?.toString() ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: localProps["KEY_ALIAS"]?.toString() ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: localProps["KEY_PASSWORD"]?.toString() ?: ""
        }
    }

    flavorDimensions += "variant"
    productFlavors {
        create("stable") {
            dimension = "variant"
            // uses default applicationId
        }
        create("lab") {
            dimension = "variant"
            applicationIdSuffix = ".lab"
            versionNameSuffix = "-lab"
            resValue("string", "app_name", "Subty Lab")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            firebaseAppDistribution {
                artifactType = "AAB"
                val creds = System.getenv("FIREBASE_SERVICE_ACCOUNT").orEmpty()
                if (creds.isNotBlank()) serviceCredentialsFile = creds
            }
        }

        debug {
            signingConfig = signingConfigs.getByName("debug")
            firebaseAppDistribution {
                artifactType = "APK"
                val creds = System.getenv("FIREBASE_SERVICE_ACCOUNT").orEmpty()
                if (creds.isNotBlank()) serviceCredentialsFile = creds
                testers = "arnon7700@gmail.com"
            }
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
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Retrofit + OkHttp + Moshi
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.okhttp)

    // DataStore
    implementation(libs.datastore.preferences)

    // Security
    implementation(libs.security.crypto)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.runner)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation("com.google.firebase:firebase-config-ktx")
}

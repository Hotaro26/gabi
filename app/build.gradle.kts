import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.chaquo.python")
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation("dev.chrisbanes.haze:haze:0.7.2")
            
            // Ktor
            val ktor_version = "2.3.8"
            implementation("io.ktor:ktor-client-core:$ktor_version")
            implementation("io.ktor:ktor-client-cio:$ktor_version")
            implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
            implementation("io.ktor:ktor-client-logging:$ktor_version")
            
            // Kotlinx Serialization & Coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            
            // Koin Dependency Injection
            implementation("io.insert-koin:koin-core:3.5.3")
            implementation("io.insert-koin:koin-compose:1.1.2")
        }
        
        androidMain.dependencies {
            implementation("androidx.core:core-ktx:1.12.0")
            implementation("androidx.core:core-splashscreen:1.0.1")
            implementation("androidx.activity:activity-compose:1.8.2")
            
            // Koin Android
            implementation("io.insert-koin:koin-android:3.5.3")
            implementation("io.insert-koin:koin-androidx-compose:3.5.3")
            
            // Old Android-only dependencies (will break if moved to commonMain)
            implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
            implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
            implementation("androidx.compose.material:material-icons-extended:1.6.0")
            implementation("com.google.android.material:material:1.11.0")
            implementation("androidx.compose.material3:material3-window-size-class:1.2.0")
            
            val room_version = "2.6.1"
            implementation("androidx.room:room-runtime:$room_version")
            implementation("androidx.room:room-ktx:$room_version")
            
            implementation("io.coil-kt:coil-compose:2.6.0")
            implementation("io.coil-kt:coil-video:2.6.0")
            
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
            implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.3")
            implementation("com.squareup.okhttp3:okhttp:4.12.0")
            implementation("androidx.media3:media3-exoplayer:1.2.1")
            implementation("androidx.media3:media3-ui:1.2.1")
        }
        
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
        }
    }
}

android {
    namespace = "com.material.downloader"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.material.downloader"
        minSdk = 24
        targetSdk = 34
        versionCode = 5
        versionName = "3.9"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = project.rootProject.file("keys/gabi-release.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    buildFeatures {
        buildConfig = true
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    add("kspAndroid", "androidx.room:room-compiler:2.6.1")
}

chaquopy {
    defaultConfig {
        version = "3.11"
        pip {
            install("pip")
            install("yt-dlp")
            install("gallery-dl")
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.material.downloader.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageName = "Gabi"
            packageVersion = "3.5.0"
        }
    }
}

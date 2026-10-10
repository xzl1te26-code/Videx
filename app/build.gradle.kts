import java.net.URL
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.chaquopy)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.videodownloader"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.videodownloader"
        minSdk = 26
        targetSdk = 36
        versionCode = 16
        versionName = "1.1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
        }
    }

    // --- Оптимизация нативных библиотек под 16KB (Android 15) ---
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    // -----------------------------------------------------------

    signingConfigs {
        create("release") {
            storeFile = file("keystore/release.keystore")
            storePassword = "androidappvidex"
            keyAlias = "videxkey"
            keyPassword = "androidappvidex"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:strongSkipping=true"
        )
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

// Настройка Python с библиотеками yt-dlp и requests
chaquopy {
    defaultConfig {
        version = "3.13"
        buildPython("python")
        pip {
            install("yt-dlp")
            install("requests")
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ⭐️ БАЗА ДАННЫХ ROOM
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Библиотеки для картинок и генерации превью из видео (Coil)
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-video:2.6.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // 🎬 MEDIA3 PLAYER & MEDIA SESSION
    val media3Version = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("androidx.media:media:1.7.0")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.register("downloadFFmpeg") {
    val jniDir = file("src/main/jniLibs")
    val arm64Dir = file("src/main/jniLibs/arm64-v8a")
    val x8664Dir = file("src/main/jniLibs/x86_64")
    
    outputs.dir(jniDir)
    
    doLast {
        arm64Dir.mkdirs()
        x8664Dir.mkdirs()
        
        fun verifySha256(targetFile: File, expectedHash: String) {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = targetFile.readBytes()
            val actualHash = digest.digest(bytes).joinToString("") { b -> String.format("%02X", b) }
            if (!actualHash.equals(expectedHash, ignoreCase = true)) {
                targetFile.delete()
                throw GradleException("SHA-256 verification failed for ${targetFile.name}! Expected: $expectedHash, Actual: $actualHash")
            }
        }

        val armFile = file("${arm64Dir.path}/libffmpeg.so")
        val expectedArmHash = "6BB182D0D75D23028DB82E9E4F723CA69B853D055698486E6984DDB2C06FB8CE"
        if (!armFile.exists()) {
            println("Downloading FFmpeg for arm64...")
            URL("https://github.com/eugeneware/ffmpeg-static/releases/download/b6.1.1/ffmpeg-linux-arm64.gz").openStream().use { input ->
                GZIPInputStream(input).use { gzInput ->
                    FileOutputStream(armFile).use { output ->
                        gzInput.copyTo(output)
                    }
                }
            }
            verifySha256(armFile, expectedArmHash)
        }
        
        val x86File = file("${x8664Dir.path}/libffmpeg.so")
        val expectedX86Hash = "E7E7FB30477F717E6F55F9180A70386C62677EF8A4D4D1A5D948F4098AA3EB99"
        if (!x86File.exists()) {
            println("Downloading FFmpeg for x86_64...")
            URL("https://github.com/eugeneware/ffmpeg-static/releases/download/b6.1.1/ffmpeg-linux-x64.gz").openStream().use { input ->
                GZIPInputStream(input).use { gzInput ->
                    FileOutputStream(x86File).use { output ->
                        gzInput.copyTo(output)
                    }
                }
            }
            verifySha256(x86File, expectedX86Hash)
        }
    }
}

tasks.named("preBuild") {
    dependsOn("downloadFFmpeg")
}
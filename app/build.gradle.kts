import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.sipherdiary"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.sipherdiary"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // --- Firebase tanpa plugin google-services ---
        // Jika app/google-services.json ada, nilai-nilainya dijadikan string resource
        // (google_app_id, gcm_defaultSenderId, dll). FirebaseInitProvider membaca
        // resource ini secara otomatis. Jika file tidak ada, aplikasi berjalan dalam
        // "mode demo" (OTP & backup disimulasikan secara lokal).
        val gsFile = file("google-services.json")
        if (gsFile.exists()) {
            @Suppress("UNCHECKED_CAST")
            val json = JsonSlurper().parse(gsFile) as Map<String, Any?>
            val projectInfo = json["project_info"] as Map<String, Any?>
            val clients = json["client"] as List<Map<String, Any?>>
            val client = clients.firstOrNull {
                val info = it["client_info"] as Map<String, Any?>
                val android = info["android_client_info"] as Map<String, Any?>
                android["package_name"] == applicationId
            } ?: clients.first()
            val clientInfo = client["client_info"] as Map<String, Any?>
            val apiKey = (client["api_key"] as List<Map<String, Any?>>).first()["current_key"].toString()
            val webClientId = (client["oauth_client"] as? List<Map<String, Any?>>)
                ?.firstOrNull { (it["client_type"] as Number).toInt() == 3 }
                ?.get("client_id")?.toString() ?: ""

            resValue("string", "google_app_id", clientInfo["mobilesdk_app_id"].toString())
            resValue("string", "google_api_key", apiKey)
            resValue("string", "gcm_defaultSenderId", projectInfo["project_number"].toString())
            resValue("string", "project_id", projectInfo["project_id"].toString())
            resValue("string", "google_storage_bucket", projectInfo["storage_bucket"].toString())
            resValue("string", "default_web_client_id", webClientId)
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        resValues = true
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
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("androidx.compose.material:material-icons-extended")

    // Navigasi antar layar Compose
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Penyimpanan pengaturan lokal (DataStore)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Pemantau siklus hidup aplikasi (untuk auto-lock)
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")

    // Sidik jari (BiometricPrompt)
    implementation("androidx.biometric:biometric:1.1.0")

    // Hash PIN dengan bcrypt
    implementation("at.favre.lib:bcrypt:0.10.2")

    // Database Lokal (Room)
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")

    // Firebase (TANPA analytics/iklan, agar tidak ada tracker di laporan MobSF)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // Google SSO lewat Credential Manager
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Kamera depan untuk foto penyusup
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")

    // Backup otomatis harian
    implementation("androidx.work:work-runtime-ktx:2.10.0")
}
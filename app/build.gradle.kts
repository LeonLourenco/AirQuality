import java.util.Properties
import java.io.FileInputStream

@Suppress("DSL_SCOPE_VIOLATION") // Permite o uso de `libs` no bloco de plugins
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.airquality"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.airquality"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Lê as propriedades do local.properties
        val properties = Properties()
        properties.load(FileInputStream(rootProject.file("local.properties")))

        // Usa manifestPlaceholders para substituir valores no AndroidManifest.xml
        manifestPlaceholders["Maps_key"] = properties.getProperty("Maps_key") ?: ""

        // Usa buildConfigField para disponibilizar variáveis no código Kotlin/Java
        buildConfigField("String", "SUPABASE_URL", "\"${properties.getProperty("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${properties.getProperty("SUPABASE_ANON_KEY")}\"")
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui.graphics)

    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:2.4.1"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:coil-integration:2.4.1")

    // Ktor + JSON
    implementation("io.ktor:ktor-client-okhttp:2.3.11")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Hilt (DI)
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Google Maps e localização
    implementation("com.google.maps.android:maps-compose:6.0.0")
    implementation("com.google.maps.android:maps-compose-utils:6.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps")
    implementation("com.google.accompanist:accompanist-permissions:0.31.5-beta")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // ML Kit OCR
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // Coil (imagens)
    implementation("io.coil-kt.coil3:coil-compose:3.0.0-alpha06")

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Vico Charts
    implementation("com.patrykandpatrick.vico:compose-m3:1.14.0")
}

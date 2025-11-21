import java.io.File

fun loadEnvMap(vararg files: File?): Map<String, String> {
    val result = mutableMapOf<String, String>()
    files.filterNotNull().forEach { file ->
        if (!file.exists()) return@forEach
        file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .forEach { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    result[parts[0].trim()] = parts[1].trim()
                }
            }
    }
    return result
}

fun String.toBuildConfigString(): String =
    this.replace("\\", "\\\\").replace("\"", "\\\"")

val envValues = loadEnvMap(
    rootProject.file(".env"),
    rootProject.projectDir.parentFile?.resolve(".env")
)
val backendBaseUrl =
    envValues["BACKEND_BASE_URL"]?.takeIf { it.isNotBlank() }
        ?: "http://192.168.1.36:8080/api/users/"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.shoter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.shoter"
        minSdk = 30
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "BACKEND_BASE_URL",
            "\"${backendBaseUrl.toBuildConfigString()}\""
        )
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // Camera and AR dependencies
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("androidx.camera:camera-extensions:1.3.4")
    
    // ARCore
    implementation("com.google.ar:core:1.40.0")
    
    // ML Kit Pose Detection dependency
    implementation("com.google.mlkit:pose-detection:18.0.0-beta4")
    implementation("com.google.mlkit:pose-detection-accurate:18.0.0-beta4")
    implementation("com.google.mlkit:face-detection:16.1.5")
    
    // Networking for server communication
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    
    // ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

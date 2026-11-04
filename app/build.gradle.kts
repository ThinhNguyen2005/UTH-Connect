import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services) apply false
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

val useMockData = providers.gradleProperty("useMockData").orElse("true").get().toBooleanStrict()
val useSupabase = providers.gradleProperty("useSupabase").orElse("false").get().toBooleanStrict()
val supabaseUrl = providers.gradleProperty("supabaseUrl").orElse("").get().trimEnd('/')
val supabasePublishableKey = providers.gradleProperty("supabasePublishableKey").orElse("").get()
if (useSupabase) {
    require(!useMockData) { "To enable Supabase, set -PuseMockData=false" }
    require(supabaseUrl.startsWith("https://") && supabasePublishableKey.startsWith("sb_publishable_")) {
        "Supply -PsupabaseUrl=https://... -PsupabasePublishableKey=sb_publishable_..."
    }
}
if (!useMockData && !useSupabase) { pluginManager.apply(libs.plugins.google.services.get().pluginId)
}

android {
    namespace = "com.giathinh.uthconnect"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }

    defaultConfig {
        applicationId = "com.giathinh.uthconnect"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("boolean", "USE_MOCK_DATA", useMockData.toString())
        buildConfigField("boolean", "USE_SUPABASE", useSupabase.toString())
        buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${supabasePublishableKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        disable += "OldTargetRuntime"
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation("com.google.zxing:core:3.5.4")

    // Máy quét QR điểm danh: preview + phân tích frame liên tục trong app.
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.kotlinx.coroutines.play.services)

    // DI
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.compiler)
    ksp(libs.kotlin.metadata.jvm)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

configurations.all {
    resolutionStrategy {
        force("androidx.concurrent:concurrent-futures:1.2.0")
    }
}

tasks.configureEach {
    if (name.contains("AarMetadata")) {
        enabled = false
    }
}

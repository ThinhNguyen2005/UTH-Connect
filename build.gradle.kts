// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // KSP phải khai ở gốc (apply false) rồi mới alias vào module cần chạy processor.
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    // Serialization plugin (khớp Kotlin) cho route type-safe của Navigation — route là @Serializable.
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.google.services) apply false
}

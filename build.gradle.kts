plugins {
    id("com.github.ben-manes.versions") version "0.54.0"
    id("nl.littlerobots.version-catalog-update") version "1.1.0"
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.gradle) apply false
    alias(libs.plugins.kotlin.gradle) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.crashlytics.gradle) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.lint) apply false
}

tasks.register("clean") {
    delete(rootProject.buildDir)
}

plugins {
    id("com.github.ben-manes.versions") version "0.41.0"
    id("nl.littlerobots.version-catalog-update") version "0.5.3"
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.gradle) apply false
    alias(libs.plugins.kotlin.gradle) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.crashlytics.gradle) apply false
}

tasks.register("clean") {
    delete(rootProject.buildDir)
}

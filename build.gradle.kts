buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath(libs.plugin.android.gradle)
        classpath(libs.plugin.kotlin.gradle)
        classpath(libs.google.services)
        classpath(libs.plugin.crashlytics.gradle)
        classpath(libs.plugin.hilt.gradle)
//        classpath(libs.plugin.ksp)
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.52.0"
    id("nl.littlerobots.version-catalog-update") version "1.0.0"
    id("com.google.devtools.ksp") version "2.1.21-2.0.1"
    alias(libs.plugins.kotlin.compose) apply false
}

//allprojects {
//    repositories {
//        google()
//        mavenCentral()
//        maven { url = uri("https://jitpack.io") }
//        // jcenter() is deprecated, removed
//    }
//}

tasks.register("clean") {
    delete(rootProject.buildDir)
}

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
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.41.0"
    id("nl.littlerobots.version-catalog-update") version "0.5.3"
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
}

tasks.register("clean") {
    delete(rootProject.buildDir)
}

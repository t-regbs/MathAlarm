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

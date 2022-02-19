repositories {
    google()
    jcenter()
}

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("com.android.tools.build:gradle:7.1.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31")
    implementation("com.google.gms:google-services:4.3.10")
    implementation("com.google.firebase:firebase-crashlytics-gradle:2.8.0")
    implementation("com.google.dagger:hilt-android-gradle-plugin:2.38.1")
}

plugins {
    id("java-library")
    id("kotlin")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.android)
//    implementation(libs.timber)
    implementation(libs.kotlinx.datetime)
}

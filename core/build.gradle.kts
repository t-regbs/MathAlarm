plugins {
    id("java-library")
    id("kotlin")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.21")
    implementation(libs.coroutines.android)
    implementation(libs.timber)
}

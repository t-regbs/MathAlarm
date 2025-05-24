include(":core")
include(":app")
rootProject.name = "MathAlarm"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://jitpack.io") }
    }
}

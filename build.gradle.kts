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

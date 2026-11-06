import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.serialization)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.native.coroutines)
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    android {
        namespace = "com.timilehinaregbesola.mathalarm.shared"
        compileSdk = libs.versions.android.compile.sdk.get().toInt()
        minSdk = libs.versions.android.min.sdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        withHostTestBuilder {
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        // Both the app framework and standalone native database tests use OS SQLite.
        iosTarget.binaries.all { linkerOpts.add("-lsqlite3") }
        iosTarget.binaries.framework {
            baseName = "app"
            isStatic = true
            export(project(":core"))
            export(libs.observable.viewmodel)
        }
    }
    
    sourceSets {
        all { languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi") }
        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.core.ktx)
        }
        iosMain.dependencies {
            implementation(libs.androidx.sqlite.driver.framework)
        }
        commonMain {
            dependencies {
                api(project(":core"))
                api(libs.observable.viewmodel)
                implementation(libs.jetbrains.lifecycle.viewmodel)
                implementation(libs.native.coroutines.annotations)

                val koinBom = project.dependencies.platform(libs.koin.bom)
                implementation(koinBom)
                implementation(libs.koin.core)
                implementation(libs.koin.core.viewmodel)

                implementation(libs.kermit)

                implementation(libs.coroutines.core)
                implementation(libs.kotlinx.serialization)

                implementation(libs.kotlinx.datetime)
                implementation(libs.multiplatform.settings.no.arg)
                
                implementation(libs.androidx.room.runtime)

            }
        }
        
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.kotest.assertions)
            implementation(libs.multiplatform.settings.test)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)

}

// Room emits public *_Impl subclasses. Refine those generated native declarations
// together with their HiddenFromObjC ports so database infrastructure is never a
// Swift API. Runs after KSP (including cached KSP outputs) and before compilation.
// This changes export visibility only; generated SQL/schema/runtime remain intact.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>().configureEach {
    doFirst {
        listOf("iosArm64", "iosSimulatorArm64").forEach { target ->
            fileTree("build/generated/ksp/$target/${target}Main/kotlin") {
                include("**/AlarmDatabase_Impl.kt", "**/AlarmDao_Impl.kt", "**/AlarmDatabaseConstructor.kt")
            }.forEach { source ->
                val annotation = "@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)\n@kotlin.native.HiddenFromObjC\n"
                val content = source.readText()
                if (!content.contains("@kotlin.native.HiddenFromObjC")) {
                    source.writeText(content.replace("public class ", annotation + "public class ").replace("public actual object ", annotation + "public actual object "))
                }
            }
        }
    }
}

// The legacy fixture is a public disposable schema-9 snapshot, never phone data.
tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest>().configureEach {
    val fixture = file("src/iosTest/resources/legacy-v9-native.sqlite3")
    inputs.file(fixture)
    environment("MATHALARM_M8_LEGACY_SQLITE_FIXTURE", fixture.absolutePath)
    environment("SIMCTL_CHILD_MATHALARM_M8_LEGACY_SQLITE_FIXTURE", fixture.absolutePath)
}

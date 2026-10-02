@file:OptIn(ExperimentalSwiftExportDsl::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.native.coroutines)
    alias(libs.plugins.androidx.room)
}

ksp {
    arg("lyricist.generateStringsProperty", "true")
}

// Room KMP configuration
room {
    schemaDirectory("$projectDir/schemas")
}

compose.resources {
    packageOfResClass = "mathalarm.app.generated.resources"
}

kotlin {
    android {
        namespace = "com.timilehinaregbesola.mathalarm.shared"
        compileSdk = libs.versions.android.compile.sdk.get().toInt()
        minSdk = libs.versions.android.min.sdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            allWarningsAsErrors = false
            freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xopt-in=kotlin.Experimental")
        }
        androidResources {
            enable = true
        }
        withHostTestBuilder {
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "app"
            isStatic = true
            export(libs.calf.ui)
            export(project(":core"))
            export(libs.observable.viewmodel)
        }
    }
    
    // Swift Export configuration for AlarmKit integration
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
    swiftExport {
        // Module name for Swift imports
        moduleName = "MathAlarmShared"
        
        // Flatten package structure for cleaner Swift code
        flattenPackage = "com.timilehinaregbesola.mathalarm.alarm"
        
        // Compiler configuration
        configure {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    sourceSets {
        all { languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi") }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.core.ktx)
        }
        commonMain {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            dependencies {
                api(project(":core"))
                api(libs.observable.viewmodel)
                implementation(libs.native.coroutines.annotations)
                implementation(libs.runtime)
                implementation(libs.foundation)
                implementation(libs.material3)
                implementation(libs.haze)
                api(libs.calf.ui)
                implementation(libs.ui)
                implementation(libs.components.resources)
                implementation(libs.ui.tooling.preview)

                val koinBom = project.dependencies.platform(libs.koin.bom)
                implementation(koinBom)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)

                implementation(libs.kermit)

                implementation(libs.coroutines.core)
                implementation(libs.kotlinx.serialization)
                implementation(libs.lyricist)

                implementation(libs.jetbrains.navigation3.ui)
                implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
                implementation(libs.compose.material3.adaptive.navigation3)
                implementation(libs.kotlinx.datetime)
                implementation(libs.multiplatform.settings.no.arg)
                
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.sqlite.driver.bundled)

                implementation(libs.compottie.lite)
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
    // Room KMP - compiler for each platform
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)

    add("kspCommonMainMetadata", libs.lyricist.processor)
}

afterEvaluate {
    // Make all compilation tasks depend on KSP common metadata
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
        if (name != "kspCommonMainKotlinMetadata") {
            dependsOn("kspCommonMainKotlinMetadata")
        }
    }
}

// Ensure all KSP tasks run after common metadata KSP
tasks.matching { it.name.startsWith("ksp") && it.name != "kspCommonMainKotlinMetadata" }.configureEach {
    dependsOn(tasks.named("kspCommonMainKotlinMetadata"))
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

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("dagger.hilt.android.plugin")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("com.google.firebase.crashlytics")
    id("com.google.devtools.ksp")
    alias(libs.plugins.kotlin.compose)
}

ksp {
    arg("lyricist.generateStringsProperty", "true")
}

android {
    namespace = "com.timilehinaregbesola.mathalarm"
    defaultConfig {
        applicationId = "com.timilehinaregbesola.mathalarm"
        versionCode = 19
        versionName = "2.3.0"
        minSdk = libs.versions.android.min.sdk.get().toInt()
        targetSdk = libs.versions.android.target.sdk.get().toInt()
        compileSdk = libs.versions.android.compile.sdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }

        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        allWarningsAsErrors = false
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xopt-in=kotlin.Experimental")
        jvmTarget = "17"
    }
    lint {
        disable += setOf("LogNotTimber", "StringFormatInTimber", "ThrowableNotAtBeginning", "BinaryOperationInTimber", "TimberArgCount", "TimberArgTypes", "TimberTagLength", "TimberExceptionLogging")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packagingOptions {
        resources.excludes.apply {
            add("META-INF/DEPENDENCIES")
            add("META-INF/LICENSE")
            add("META-INF/LICENSE.txt")
            add("META-INF/license.txt")
            add("META-INF/NOTICE")
            add("META-INF/NOTICE.txt")
            add("META-INF/notice.txt")
            add("META-INF/AL2.0")
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(project(":core"))

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.android.material)
    implementation(libs.androidx.ktx)

    implementation(libs.androidx.test.core.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)
    testImplementation(libs.mockk)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.lottie.compose)

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    implementation(libs.accompanist.navigation.material)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.timber)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.moshi)
    implementation(libs.androidx.datastore)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.lyricist)
    ksp(libs.lyricist.processor)
}

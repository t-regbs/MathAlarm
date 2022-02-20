import extensions.addComposeConfig

plugins {
    id(GradlePlugin.ANDROID_APPLICATION)
    id(GradlePlugin.KOTLIN_ANDROID)
    id(GradlePlugin.KAPT)
    id(GradlePlugin.DAGGER_HILT)
    id(GradlePlugin.GOOGLE_SERVICES)
    id(GradlePlugin.FIREBASE_CRASHLYTICS)
}

android {
    defaultConfig {
        applicationId = "com.timilehinaregbesola.mathalarm"
        versionCode = Releases.versionCode
        versionName = Releases.versionName
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk
        compileSdk = Versions.compileSdk
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
        }

        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        allWarningsAsErrors = false
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xopt-in=kotlin.Experimental")
        jvmTarget = "11"
        useIR = true
    }
    lint {
        disable += setOf("LogNotTimber", "StringFormatInTimber", "ThrowableNotAtBeginning", "BinaryOperationInTimber", "TimberArgCount", "TimberArgTypes", "TimberTagLength", "TimberExceptionLogging")
    }

    addComposeConfig()

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(project(":core"))

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")

    implementation(Deps.android.material)
    implementation(Deps.compose.ui)
    implementation(Deps.compose.material)
    implementation(Deps.compose.icons)
    implementation(Deps.compose.tooling)
    implementation(Deps.compose.runtime)
    implementation(Deps.compose.runtimeSaveable)
    implementation(Deps.compose.runtimeLivedata)

    implementation(Deps.android.ktx)

    implementation(Deps.test.coreKtx)
    testImplementation(Deps.test.junit)
    testImplementation(Deps.test.coroutinesTest)
    androidTestImplementation(Deps.test.junitExt)
    androidTestImplementation(Deps.test.espressoCore)
    testImplementation(Deps.test.mockk)

    implementation(Deps.room.runtime)
    implementation(Deps.room.ktx)
    kapt(Deps.room.compiler)

    implementation(Deps.coroutines.core)
    implementation(Deps.coroutines.android)

    implementation(Deps.compose.activity)
    implementation(Deps.accompanist.navigationMaterial)
    implementation(Deps.compose.navigation)

    implementation(Deps.timber)

    implementation(Deps.hilt.android)
    kapt(Deps.hilt.androidCompiler)
    implementation(Deps.hilt.lifecycleVm)
    kapt(Deps.hilt.compiler)

    implementation(Deps.dialog.datetime)

    implementation(platform(Deps.firebase.bom))
    implementation(Deps.firebase.analytics)
    implementation(Deps.firebase.crashlytics)
    implementation(Deps.firebase.firebaseMessaging)

    implementation(Deps.splash.core)
}
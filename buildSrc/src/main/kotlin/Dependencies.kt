object Releases {
    const val versionCode = 12
    const val versionName = "2.0.2"
}

object Versions {
    const val compileSdk = 31
    const val targetSdk = 31
    const val minSdk = 21

    const val material = "1.4.0"
    const val ktx = "1.7.0"
    const val room = "2.4.0-alpha01"

    const val coroutines = "1.3.7"

    const val timber = "4.7.1"

    const val hiltAndroid = "2.38.1"
    const val hiltAndroidCompiler = "2.37"
    const val hiltLifecycleViewmodel = "1.0.0-alpha03"
    const val hiltCompiler = "1.0.0"

    const val testJunit = "4.13.2"
    const val testCore = "1.4.0"
    const val testMockk = "1.12.0"
    const val testEspressoCore = "3.4.0"
    const val testJunitExt = "1.1.3"
    const val testCoroutines = "1.5.0"

    const val compose = "1.0.4"
    const val composeNav = "1.0.0-alpha03"
    const val composeActivity = "1.4.0"

    const val accompanist = "0.22.0-rc"

    const val materialDialogs = "0.5.1"

    const val firebase = "27.1.0"

    const val splashScreen = "1.0.0-beta01"
    const val moshi = "1.13.0"
}

object Deps {
    const val timber = "com.jakewharton.timber:timber:${Versions.timber}"
    val android = AndroidDeps
    val coroutines = CoroutinesDeps
    val hilt = HiltDeps
    val compose = ComposeDeps
    val accompanist = AccompanistDeps
    val test = TestDeps
    val firebase = FirebaseDeps
    val room = RoomDeps
    val dialog = DialogDeps
    val splash = SplashScreenDeps
    val moshi = "com.squareup.moshi:moshi-kotlin:${Versions.moshi}"
}

object AndroidDeps {
    const val material = "com.google.android.material:material:${Versions.material}"
    const val ktx = "androidx.core:core-ktx:${Versions.ktx}"
    val room = RoomDeps
}

object CoroutinesDeps {
    const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
    const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
}

object RoomDeps {
    const val runtime = "androidx.room:room-runtime:${Versions.room}"
    const val compiler = "androidx.room:room-compiler:${Versions.room}"
    const val ktx = "androidx.room:room-ktx:${Versions.room}"
}

object HiltDeps {
    const val androidCompiler = "com.google.dagger:hilt-android-compiler:${Versions.hiltAndroidCompiler}"
    const val android = "com.google.dagger:hilt-android:${Versions.hiltAndroid}"
    const val lifecycleVm = "androidx.hilt:hilt-lifecycle-viewmodel:${Versions.hiltLifecycleViewmodel}"
    const val compiler = "androidx.hilt:hilt-compiler:${Versions.hiltCompiler}"
}

object ComposeDeps {
    const val ui = "androidx.compose.ui:ui:${Versions.compose}"
    const val material = "androidx.compose.material:material:${Versions.compose}"
    const val tooling = "androidx.compose.ui:ui-tooling:${Versions.compose}"
    const val icons = "androidx.compose.material:material-icons-extended:${Versions.compose}"
    const val runtime = "androidx.compose.runtime:runtime:${Versions.compose}"
    const val runtimeSaveable = "androidx.compose.runtime:runtime-saveable:${Versions.compose}"
    const val runtimeLivedata = "androidx.compose.runtime:runtime-livedata:${Versions.compose}"
    const val activity = "androidx.activity:activity-compose:${Versions.composeActivity}"
    const val navigation = "androidx.hilt:hilt-navigation-compose:${Versions.composeNav}"
}

object AccompanistDeps {
    const val navigationMaterial =
        "com.google.accompanist:accompanist-navigation-material:${Versions.accompanist}"
}

object TestDeps {
    const val junit = "junit:junit:${Versions.testJunit}"
    const val core = "androidx.test:core:${Versions.testCore}"
    const val coreKtx = "androidx.test:core-ktx:${Versions.testCore}"
    const val junitExt = "androidx.test.ext:junit:${Versions.testJunitExt}"
    const val mockk = "io.mockk:mockk:${Versions.testMockk}"
    const val coroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.testCoroutines}"
    const val espressoCore = "androidx.test.espresso:espresso-core:${Versions.testEspressoCore}"
    const val hiltAndroidTesting = "com.google.dagger:hilt-android-testing:${Versions.hiltAndroidCompiler}"
    const val hiltAndroidCompiler = "com.google.dagger:hilt-android-compiler:${Versions.hiltAndroidCompiler}"
    const val composeUiTest = "androidx.compose.ui:ui-test-junit4:${Versions.compose}"
}

object FirebaseDeps {
    const val bom = "com.google.firebase:firebase-bom:${Versions.firebase}"
    const val analytics = "com.google.firebase:firebase-analytics-ktx"
    const val crashlytics = "com.google.firebase:firebase-crashlytics"
    const val firebaseMessaging = "com.google.firebase:firebase-messaging"
}

object DialogDeps {
    const val datetime = "io.github.vanpra.compose-material-dialogs:datetime:${Versions.materialDialogs}"
}

object SplashScreenDeps {
    const val core = "androidx.core:core-splashscreen:${Versions.splashScreen}"
}

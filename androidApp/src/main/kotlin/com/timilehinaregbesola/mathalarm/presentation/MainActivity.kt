package com.timilehinaregbesola.mathalarm.presentation

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.review.ReviewManagerFactory
import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.notification.ActiveAlarmManager
import com.timilehinaregbesola.mathalarm.presentation.review.InAppReviewCoordinator
import com.timilehinaregbesola.mathalarm.presentation.review.ReviewEligibilityStore
import com.timilehinaregbesola.mathalarm.presentation.review.ReviewVisitViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import cafe.adriel.lyricist.Lyricist
import cafe.adriel.lyricist.ProvideStrings
import cafe.adriel.lyricist.rememberStrings
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.coroutines.AppCoroutineScope
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.navigation.NavGraph
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.shouldUseDarkColors
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.utils.strings.Strings
import kotlinx.coroutines.InternalCoroutinesApi
import org.koin.android.ext.android.inject

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@InternalCoroutinesApi
@ExperimentalAnimationApi
class MainActivity : AppCompatActivity() {
    val preferences: AlarmPreferencesImpl by inject()
    private lateinit var lyricist: Lyricist<Strings>
    private val logger = Logger.withTag("MainActivity")
    private val reviewStore: ReviewEligibilityStore by inject()
    private val alarmRepository: AlarmRepository by inject()
    private val reviewSession: ReviewVisitViewModel by viewModels()
    private val reviewCoordinator get() = reviewSession.coordinator
    private var reviewUiReady = false
    private var deeplinkInfo by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setupLockScreenFlags()

        deeplinkInfo = intent.extractAlarmJson()
        runCatching {
            reviewStore.recordFirstUse()
        }.onFailure { logger.w(it) { "Unable to initialize in-app reviews" } }

        setContent {
            val isDarkTheme = preferences.shouldUseDarkColors()
            updateStatusBarColor(isDarkTheme)
            lyricist = rememberStrings()
            ProvideStrings(lyricist) {
                MathAlarmTheme(darkTheme = isDarkTheme) {
                    NavGraph(
                        preferences = preferences,
                        deeplinkInfo = deeplinkInfo,
                        onDeeplinkConsumed = ::consumeAlarmDeeplink,
                        reviewVisit = reviewSession.visit,
                        onReviewOpportunityChanged = {
                            if (reviewUiReady && !it) reviewCoordinator?.invalidatePendingRequest()
                            reviewUiReady = it
                        },
                        onRequestReview = {
                            lifecycleScope.launch {
                                reviewCoordinator?.request(this@MainActivity) {
                                    reviewUiReady && deeplinkInfo == null &&
                                        lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                                        !isFinishing && !isDestroyed && hasWindowFocus() &&
                                        !(getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    private fun consumeAlarmDeeplink() {
        deeplinkInfo = null
        intent = intent.apply { data = null }
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    private fun updateStatusBarColor(darkTheme: Boolean) {
        window.apply {
            WindowInsetsControllerCompat(this, this.decorView).isAppearanceLightStatusBars =
                !darkTheme
        }
    }

    override fun onStart() {
        super.onStart()
        reviewUiReady = false
        runCatching {
            reviewSession.onStart {
                // An alarm-created task can later be reopened from the launcher with its
                // original ACTION_VIEW intent. Gate on the pending alarm, not that stale action.
                if (deeplinkInfo != null) return@onStart null
                // The retained coordinator must never capture an Activity across rotation.
                val repository = alarmRepository
                InAppReviewCoordinator(
                    reviewStore,
                    ReviewManagerFactory.create(applicationContext),
                    hasPendingAlarm = {
                        ActiveAlarmManager.hasActiveAlarm() || repository.getAlarms().first().any {
                            it.activeAt != null || it.snoozedUntil != null
                        }
                    },
                )
            }
        }.onFailure { logger.w(it) { "Unable to initialize in-app reviews" } }
    }

    override fun onResume() {
        super.onResume()
        val scope: AppCoroutineScope by inject()
        val usecases: Usecases by inject()
        scope.launch { usecases.command { rescheduleFutureAlarms.onAppResume() } }
    }

    override fun onStop() {
        reviewSession.onStop(isChangingConfigurations)
        super.onStop()
    }

    private fun Intent.extractAlarmJson(): String? {
        return data?.lastPathSegment
            ?.takeIf { it.startsWith("$PARAM=") }
            ?.substringAfter("$PARAM=")
            ?.let { base64String ->
                try {
                    val decodedBytes = Base64.decode(base64String, Base64.URL_SAFE or Base64.NO_WRAP)
                    String(decodedBytes, Charsets.UTF_8)
                } catch (e: Exception) {
                    logger.e("Failed to decode Base64 alarm data", e)
                    null
                }
            }
    }

    companion object {
        private const val PARAM = "alarmId"
    }
}

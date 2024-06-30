package com.timilehinaregbesola.mathalarm.presentation.appsettings.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Announcement
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.lyricist.strings
import com.timilehinaregbesola.mathalarm.BuildConfig
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.Theme
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.Theme.DARK
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.Theme.LIGHT
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.Theme.SYSTEM
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AppThemeOptionsMapper
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.APP_BAR_SHADOW
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.DEFAULT_SETTINGS_CORNER_SHAPE
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.HELP_ICON_SIZE
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.HELP_ITEM_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.SEND_INTENT_TYPE
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.SEND_TEXT
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.SETTINGS_ICON_END_PADDING
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.SETTINGS_WIDTH
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.TOP_BAR_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.appsettings.shouldUseDarkColors
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.utils.email
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    pref: AlarmPreferencesImpl,
    onBackPress: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isDark = pref.shouldUseDarkColors()
    val options = listOf(
        Triple(strings.light, Icons.Filled.WbSunny, LIGHT),
        Triple(strings.dark, Icons.Filled.DarkMode, DARK),
        Triple(strings.system, Icons.Filled.Smartphone, SYSTEM)
    )
    var selectedOption = pref.loadAppTheme().collectAsState(initial = SYSTEM).value
    val onSelectionChange = { newTheme: Theme ->
        selectedOption = newTheme
        scope.launch {
            pref.updateAppTheme(newTheme)
        }
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    modifier = Modifier.shadow(APP_BAR_SHADOW),
                    title = {
                        Text(
                            text = strings.appSettings,
                            fontSize = TOP_BAR_FONT_SIZE
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            modifier = Modifier.padding(start = MaterialTheme.spacing.medium),
                            onClick = onBackPress
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.back
                            )
                        }
                    }
                )
            }
        ) { paddingVals ->
            Column(Modifier.padding(paddingVals)) {
                Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)) {
                    Text(
                        modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
                        text = strings.colorTheme,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    Row(
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isDark) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.LightGray
                                    },
                                    shape = RoundedCornerShape(DEFAULT_SETTINGS_CORNER_SHAPE)
                                )
                                .padding(MaterialTheme.spacing.extraSmall)
                        ) {
                            Row {
                                options.forEach { triple ->
                                    Row(
                                        modifier = Modifier
                                            .width(SETTINGS_WIDTH)
                                            .clip(
                                                shape = RoundedCornerShape(
                                                    DEFAULT_SETTINGS_CORNER_SHAPE
                                                )
                                            )
                                            .clickable {
                                                onSelectionChange(triple.third)
                                            }
                                            .background(
                                                if (triple.third == selectedOption) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    if (isDark) {
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    } else {
                                                        Color.LightGray
                                                    }
                                                }
                                            )
                                            .padding(vertical = MaterialTheme.spacing.extraSmall),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            modifier = Modifier.padding(end = SETTINGS_ICON_END_PADDING),
                                            imageVector = triple.second,
                                            contentDescription = triple.first
                                        )
                                        if (triple.third == selectedOption) {
                                            Text(
                                                text = triple.first,
                                                style = typography.bodyLarge.merge(),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                HorizontalDivider(color = Color.LightGray)
                Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)) {
                    val context = LocalContext.current
                    val emailChooserTitle = strings.emailChooserTitle
                    val email = strings.supportEmail
                    val shareTitle = strings.shareMathAlarm
                    Text(
                        modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
                        text = strings.help,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    HelpItem(
                        image = Icons.AutoMirrored.Filled.Announcement,
                        primaryText = strings.sendFeedback,
                        detailText = strings.sendFeedbackMessage
                    ) {
                        context.email(
                            emailChooserTitle,
                            email
                        )
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    HelpItem(
                        image = Icons.Default.Share,
                        primaryText = strings.share,
                        detailText = strings.shareWithOthers
                    ) {
                        val sendIntent = Intent()
                        sendIntent.action = Intent.ACTION_SEND
                        sendIntent.putExtra(
                            Intent.EXTRA_TEXT,
                            SEND_TEXT + BuildConfig.APPLICATION_ID
                        )
                        sendIntent.type = SEND_INTENT_TYPE
                        context.startActivity(Intent.createChooser(sendIntent, shareTitle))
                    }
                }
            }
        }
    }
}

@Composable
fun HelpItem(
    modifier: Modifier = Modifier,
    image: ImageVector,
    primaryText: String,
    detailText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.medium)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .width(HELP_ICON_SIZE)
                .height(HELP_ICON_SIZE)
                .padding(end = MaterialTheme.spacing.medium),
            imageVector = image,
            contentDescription = null
        )
        Column {
            Text(
                text = primaryText,
                fontSize = HELP_ITEM_FONT_SIZE,
                fontWeight = FontWeight.Bold
            )
            Text(
                fontWeight = FontWeight.Normal,
                text = detailText
            )
        }
    }
}

@Preview
@Composable
private fun PreviewSettingsScreen() {
    MathAlarmTheme {
        AppSettingsScreen(
            pref = AlarmPreferencesImpl(LocalContext.current, AppThemeOptionsMapper())
        ) {}
    }
}

private object AppSettingsScreen {
    const val SEND_TEXT = "MathAlarm Clock\nSolve math problems to wake up!" +
            " https://play.google.com/store/apps/details?id="
    const val SEND_INTENT_TYPE = "text/plain"
    val HELP_ICON_SIZE = 50.dp
    val APP_BAR_SHADOW = 4.dp
    val SETTINGS_WIDTH = 100.dp
    val DEFAULT_SETTINGS_CORNER_SHAPE = 16.dp
    val SETTINGS_ICON_END_PADDING = 2.dp
    val HELP_ITEM_FONT_SIZE = 18.sp
    val TOP_BAR_FONT_SIZE = 16.sp
}

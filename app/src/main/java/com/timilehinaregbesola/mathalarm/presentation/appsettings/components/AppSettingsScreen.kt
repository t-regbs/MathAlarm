package com.timilehinaregbesola.mathalarm.presentation.appsettings.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.BuildConfig
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.Theme
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.Theme.DARK
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.Theme.LIGHT
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.Theme.SYSTEM
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.APP_SETTINGS
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.BACK
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.COLOR_THEME
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.DARK_TEXT
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.DEFAULT_SETTINGS_CORNER_SHAPE
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.EMAIL
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.EMAIL_CHOOSER_TITLE
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.HELP
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.HELP_ICON_SIZE
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.HELP_ITEM_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.LIGHT_TEXT
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.SEND_FEEDBACK
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.SEND_FEEDBACK_DETAIL
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.SEND_INTENT_TYPE
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.SEND_TEXT
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.SETTINGS_ICON_END_PADDING
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.SETTINGS_WIDTH
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.SHARE
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.SHARE_MATHALARM_TITLE
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.SHARE_WITH_OTHERS
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.SYSTEM_TEXT
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.TOP_BAR_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.appsettings.shouldUseDarkColors
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.utils.email
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    onBackPress: () -> Unit,
    pref: AlarmPreferencesImpl
) {
    val scope = rememberCoroutineScope()
    val isDark = pref.shouldUseDarkColors()
    val options = listOf(
        Triple(LIGHT_TEXT, Icons.Filled.WbSunny, LIGHT),
        Triple(DARK_TEXT, Icons.Filled.DarkMode, DARK),
        Triple(SYSTEM_TEXT, Icons.Filled.Smartphone, SYSTEM)
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
                    title = {
                        Text(
                            text = APP_SETTINGS,
                            fontSize = TOP_BAR_FONT_SIZE
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            modifier = Modifier.padding(start = MaterialTheme.spacing.medium),
                            onClick = onBackPress
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = BACK
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
                        text = COLOR_THEME,
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
                                            .clip(shape = RoundedCornerShape(DEFAULT_SETTINGS_CORNER_SHAPE))
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
                    Text(
                        modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
                        text = HELP,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    HelpItem(
                        image = Icons.Default.Announcement,
                        primaryText = SEND_FEEDBACK,
                        detailText = SEND_FEEDBACK_DETAIL
                    ) {
                        context.email(
                            EMAIL_CHOOSER_TITLE,
                            EMAIL
                        )
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    HelpItem(
                        image = Icons.Default.Share,
                        primaryText = SHARE,
                        detailText = SHARE_WITH_OTHERS
                    ) {
                        val sendIntent = Intent()
                        sendIntent.action = Intent.ACTION_SEND
                        sendIntent.putExtra(
                            Intent.EXTRA_TEXT,
                            SEND_TEXT + BuildConfig.APPLICATION_ID
                        )
                        sendIntent.type = SEND_INTENT_TYPE
                        context.startActivity(Intent.createChooser(sendIntent, SHARE_MATHALARM_TITLE))
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

private object AppSettingsScreen {
    const val SEND_TEXT = "MathAlarm Clock\nSolve math problems to wake up!" +
            " https://play.google.com/store/apps/details?id="
    const val SHARE = "Share"
    const val SHARE_WITH_OTHERS = "Share app with others"
    const val SHARE_MATHALARM_TITLE = "Share Mathalarm"
    const val SEND_INTENT_TYPE = "text/plain"
    const val EMAIL_CHOOSER_TITLE = "Feedback to MathAlarm"
    const val EMAIL = "aregbestimi@gmail.com"
    const val SEND_FEEDBACK = "Send feedback"
    const val SEND_FEEDBACK_DETAIL = "Report technical issues or suggest new features"
    const val HELP = "Help"
    const val COLOR_THEME = "Color Theme"
    const val BACK = "Back"
    const val APP_SETTINGS = "App Settings"
    const val LIGHT_TEXT = "Light"
    const val DARK_TEXT = "Dark"
    const val SYSTEM_TEXT = "System"
    val HELP_ICON_SIZE = 50.dp
    val SETTINGS_WIDTH = 100.dp
    val DEFAULT_SETTINGS_CORNER_SHAPE = 16.dp
    val SETTINGS_ICON_END_PADDING = 2.dp
    val HELP_ITEM_FONT_SIZE = 18.sp
    val TOP_BAR_FONT_SIZE = 16.sp
}

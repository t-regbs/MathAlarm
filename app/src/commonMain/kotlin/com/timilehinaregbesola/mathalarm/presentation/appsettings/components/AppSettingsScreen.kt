package com.timilehinaregbesola.mathalarm.presentation.appsettings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.lyricist.strings
import com.timilehinaregbesola.mathalarm.platform.getApplicationId
import com.timilehinaregbesola.mathalarm.platform.sendEmail
import com.timilehinaregbesola.mathalarm.platform.shareText
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.AlarmSortOrder.CREATION
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.AlarmSortOrder.TIME
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.Theme
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.Theme.DARK
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.Theme.LIGHT
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.Theme.SYSTEM
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.APP_BAR_SHADOW
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.DEFAULT_SETTINGS_CORNER_SHAPE
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.HELP_ICON_SIZE
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.HELP_ITEM_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.SEND_TEXT
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.SETTINGS_CONTENT_MAX_WIDTH
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.SETTINGS_ICON_END_PADDING
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.SETTINGS_WIDTH
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.SORT_SETTINGS_WIDTH
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen.TOP_BAR_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.appsettings.shouldUseDarkColors
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.Announcement
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.ArrowBack
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.DarkMode
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.Share
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.Smartphone
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.WbSunny
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    pref: AlarmPreferencesImpl,
    onBackPress: () -> Unit
) {
    val isDark = pref.shouldUseDarkColors()
    val themeOptions = listOf(
        Triple(strings.light, WbSunny, LIGHT),
        Triple(strings.dark, DarkMode, DARK),
        Triple(strings.system, Smartphone, SYSTEM)
    )
    val sortOptions = listOf(
        CREATION to strings.creationOrder,
        TIME to strings.timeOrder
    )
    val selectedThemeOption = themeOptions.first { it.third == pref.themeState.value }
    val selectedSortOption = sortOptions.first { it.first == pref.alarmSortOrderState.value }
    val onSelectionChange = { newTheme: Theme ->
        pref.updateAppTheme(newTheme)
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
                                imageVector = ArrowBack,
                                contentDescription = strings.back
                            )
                        }
                    }
                )
            }
        ) { paddingVals ->
            BoxWithConstraints(
                modifier = Modifier
                    .padding(paddingVals)
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                val contentWidthModifier = if (maxWidth > SETTINGS_CONTENT_MAX_WIDTH) {
                    Modifier.width(SETTINGS_CONTENT_MAX_WIDTH)
                } else {
                    Modifier.fillMaxWidth()
                }
                Column(contentWidthModifier) {
                    Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)) {
                        SegmentedSettingSection(
                            title = strings.colorTheme,
                            options = themeOptions,
                            selectedOption = selectedThemeOption,
                            isDark = isDark,
                            optionWidth = SETTINGS_WIDTH,
                            onOptionSelected = { onSelectionChange(it.third) }
                        ) { option, isSelected ->
                            Icon(
                                modifier = Modifier.padding(end = SETTINGS_ICON_END_PADDING),
                                imageVector = option.second,
                                contentDescription = option.first
                            )
                            if (isSelected) {
                                Text(
                                    text = option.first,
                                    style = typography.bodyLarge.merge(),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        SegmentedSettingSection(
                            title = strings.alarmOrder,
                            supportingText = strings.alarmOrderDescription,
                            options = sortOptions,
                            selectedOption = selectedSortOption,
                            isDark = isDark,
                            optionWidth = SORT_SETTINGS_WIDTH,
                            onOptionSelected = { pref.updateAlarmSortOrder(it.first) }
                        ) { option, isSelected ->
                            Text(
                                text = option.second,
                                style = typography.bodyLarge.merge(),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                    HorizontalDivider(color = Color.LightGray)
                    Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)) {
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
                            image = Announcement,
                            primaryText = strings.sendFeedback,
                            detailText = strings.sendFeedbackMessage
                        ) {
                            sendEmail(
                                chooserTitle = emailChooserTitle,
                                email = email
                            )
                        }
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                        HelpItem(
                            image = Share,
                            primaryText = strings.share,
                            detailText = strings.shareWithOthers
                        ) {
                            shareText(
                                title = shareTitle,
                                text = SEND_TEXT + getApplicationId()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> SegmentedSettingSection(
    title: String,
    options: List<T>,
    selectedOption: T,
    isDark: Boolean,
    optionWidth: androidx.compose.ui.unit.Dp,
    supportingText: String? = null,
    onOptionSelected: (T) -> Unit,
    optionContent: @Composable RowScope.(option: T, isSelected: Boolean) -> Unit
) {
    Text(
        modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
        text = title,
        color = MaterialTheme.colorScheme.secondary
    )
    supportingText?.let {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
        Text(
            text = it,
            style = typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    Row(horizontalArrangement = Arrangement.Center) {
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
                options.forEach { option ->
                    val isSelected = option == selectedOption
                    Row(
                        modifier = Modifier
                            .width(optionWidth)
                            .clip(RoundedCornerShape(DEFAULT_SETTINGS_CORNER_SHAPE))
                            .clickable { onOptionSelected(option) }
                            .background(
                                if (isSelected) {
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
                        optionContent(option, isSelected)
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
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
private fun PreviewHelpItem() {
    MathAlarmTheme {
        Surface {
            HelpItem(
                image = Announcement,
                primaryText = "Send Feedback",
                detailText = "Let us know what you think"
            ) {}
        }
    }
}

private object AppSettingsScreen {
    const val SEND_TEXT = "MathAlarm Clock\nSolve math problems to wake up!" +
            " https://play.google.com/store/apps/details?id="
    val HELP_ICON_SIZE = 50.dp
    val APP_BAR_SHADOW = 4.dp
    val SETTINGS_WIDTH = 100.dp
    val SORT_SETTINGS_WIDTH = 140.dp
    val DEFAULT_SETTINGS_CORNER_SHAPE = 16.dp
    val SETTINGS_ICON_END_PADDING = 2.dp
    val HELP_ITEM_FONT_SIZE = 18.sp
    val TOP_BAR_FONT_SIZE = 16.sp
    val SETTINGS_CONTENT_MAX_WIDTH = 560.dp
}

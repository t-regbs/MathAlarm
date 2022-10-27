package com.timilehinaregbesola.mathalarm.presentation.appsettings.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.typography
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
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.shouldUseDarkColors
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.utils.email
import kotlinx.coroutines.launch

@Composable
fun AppSettingsScreen(
    onBackPress: () -> Unit,
    pref: AlarmPreferencesImpl
) {
    val scope = rememberCoroutineScope()
    val isDark = pref.shouldUseDarkColors()
    val options = listOf(
        Triple("Light", Icons.Filled.WbSunny, Theme.LIGHT),
        Triple("Dark", Icons.Filled.DarkMode, Theme.DARK),
        Triple("System", Icons.Filled.Smartphone, Theme.SYSTEM)
    )
    var selectedOption = pref.loadAppTheme().collectAsState(initial = Theme.SYSTEM).value
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
                            text = "App Settings",
                            fontSize = 16.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            modifier = Modifier.padding(start = MaterialTheme.spacing.medium),
                            onClick = onBackPress
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
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
                        text = "Color Theme",
                        color = MaterialTheme.colors.secondary
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    Row(
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isDark) {
                                        MaterialTheme.colors.primaryVariant
                                    } else {
                                        Color.LightGray
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(MaterialTheme.spacing.extraSmall)
                        ) {
                            Row {
                                options.forEach { triple ->
                                    Row(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .clip(shape = RoundedCornerShape(size = 16.dp))
                                            .clickable {
                                                onSelectionChange(triple.third)
                                            }
                                            .background(
                                                if (triple.third == selectedOption) {
                                                    MaterialTheme.colors.primary
                                                } else {
                                                    if (isDark) {
                                                        MaterialTheme.colors.primaryVariant
                                                    } else {
                                                        Color.LightGray
                                                    }
                                                }
                                            )
                                            .padding(vertical = MaterialTheme.spacing.extraSmall),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            modifier = Modifier.padding(end = 2.dp),
                                            imageVector = triple.second,
                                            contentDescription = triple.first
                                        )
                                        if (triple.third == selectedOption) {
                                            Text(
                                                text = triple.first,
                                                style = typography.body1.merge(),
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
                Divider(color = Color.LightGray)
                Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)) {
                    val context = LocalContext.current
                    Text(
                        modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
                        text = "Help",
                        color = MaterialTheme.colors.secondary
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    HelpItem(
                        image = Icons.Default.Announcement,
                        primaryText = "Send feedback",
                        detailText = "Report technical issues or suggest new features"
                    ) {
                        context.email(
                            "Feedback to MathAlarm",
                            "aregbestimi@gmail.com"
                        )
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    HelpItem(
                        image = Icons.Default.Share,
                        primaryText = "Share",
                        detailText = "Share app with others"
                    ) {
                        val sendIntent = Intent()
                        sendIntent.action = Intent.ACTION_SEND
                        sendIntent.putExtra(
                            Intent.EXTRA_TEXT,
                            "MathAlarm Clock\nSolve math problems to wake up! https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID
                        )
                        sendIntent.type = "text/plain"
                        context.startActivity(Intent.createChooser(sendIntent, "Share Mathalarm"))
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
                .width(50.dp)
                .height(50.dp)
                .padding(end = MaterialTheme.spacing.medium),
            imageVector = image,
            contentDescription = null
        )
        Column {
            Text(
                text = primaryText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                fontWeight = FontWeight.Normal,
                text = detailText
            )
        }
    }
}

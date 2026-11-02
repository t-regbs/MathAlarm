package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Match the checkbox touch target so settings icons and labels share a column. */
@Composable
internal fun SettingsLeadingIcon(image: ImageVector) {
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Icon(image, contentDescription = null)
    }
}

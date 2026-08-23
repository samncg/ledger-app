package com.ledger.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* Top bar — brand + log-spend, history & customize buttons */
@Composable
fun TopBar(
    onLogSpend: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDrawer: () -> Unit,
    isWallpaper: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val tick = rememberHapticTick()
    val barBg = if (isWallpaper) cs.background.copy(alpha = 0.82f) else cs.background
    Row(
        Modifier.fillMaxWidth().background(barBg).statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(cs.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text("L", color = cs.onPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            }
            Column {
                Text("Ledger", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.weight(1f))
        /* Log spend */
        Surface(
            onClick = { tick(); onLogSpend() },
            shape = RoundedCornerShape(10.dp),
            color = cs.primary,
            contentColor = cs.onPrimary,
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Outlined.Add, null, Modifier.size(13.dp))
                Text("Log spend", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.width(8.dp))
        /* History */
        Surface(
            onClick = { tick(); onOpenHistory() },
            shape = CircleShape,
            color = cs.surfaceVariant,
            contentColor = cs.onSurfaceVariant
        ) {
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.History, null, Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        /* Customize */
        Surface(
            onClick = { tick(); onOpenDrawer() },
            shape = CircleShape,
            color = cs.surfaceVariant,
            contentColor = cs.onSurfaceVariant
        ) {
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Palette, null, Modifier.size(18.dp))
            }
        }
    }
}

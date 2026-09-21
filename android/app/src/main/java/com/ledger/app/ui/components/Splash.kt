package com.ledger.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.ui.t

/* ═══════════════════════════════════════════
   INTRO SPLASH — shown for the moment before the stored data loads, so a cold
   start shows the brand instead of a black frame. The background matches the
   window background (@color/ledger_bg = #0A0A0A) so there is no visible seam.
   ═══════════════════════════════════════════ */
@Composable
fun LedgerSplash() {
    val bg = Color(0xFF0A0A0A)
    val ink = Color(0xFFF2F2F2)

    Box(Modifier.fillMaxSize().background(bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "L",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = ink,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Ledger", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ink, letterSpacing = 0.4.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                t("app.tagline"),
                fontSize = 11.5.sp,
                color = Color.White.copy(alpha = 0.42f),
                letterSpacing = 0.3.sp,
            )
            Spacer(Modifier.height(26.dp))
            LoadingDots()
        }
    }
}

@Composable
private fun LoadingDots() {
    val transition = rememberInfiniteTransition(label = "splash")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            // Each dot peaks in turn as `phase` sweeps past it.
            val d = (phase - i + 3f) % 3f
            val a = if (d < 1f) 0.9f - d * 0.6f else 0.3f
            Box(Modifier.size(5.dp).clip(CircleShape).alpha(a).background(Color.White))
        }
    }
}

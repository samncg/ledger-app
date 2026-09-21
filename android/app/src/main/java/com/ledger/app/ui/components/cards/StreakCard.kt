package com.ledger.app.ui.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.components.CardContainer
import com.ledger.app.ui.t

/* ═══════════════════════════════════════════
   STREAK — log a spend every day to keep the run alive
   ═══════════════════════════════════════════ */
@Composable
fun StreakCard(s: LedgerState) {
    val cs = MaterialTheme.colorScheme
    val best = s.bestStreak

    CardContainer(
        title = t("card.streak.title"),
        icon = Icons.Outlined.LocalFireDepartment,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            /* The current run is the headline; the rest is context beside it. */
            Column(Modifier.width(72.dp)) {
                Text(
                    "${s.spendStreak}",
                    fontSize = 44.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, color = cs.onSurface, maxLines = 1,
                )
                Text(
                    (if (s.spendStreak == 1) t("card.streak.dayWord") else t("card.streak.daysWord")).uppercase(),
                    fontSize = 10.sp, color = cs.onSurfaceVariant, letterSpacing = 1.sp, maxLines = 1,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t("card.streak.best"), fontSize = 11.5.sp, color = cs.onSurfaceVariant)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        daysLabel(best),
                        fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace, color = cs.onSurface, maxLines = 1,
                    )
                }
                Text(
                    if (s.loggedToday) t("card.streak.loggedToday") else t("card.streak.notLoggedToday"),
                    fontSize = 11.5.sp, color = cs.onSurfaceVariant,
                )
                Text(t("card.streak.hint"), fontSize = 11.sp, color = cs.onSurfaceVariant)
                if (s.prefs.streakGrace > 0) {
                    Text(
                        t("card.streak.graceOn", "n" to s.prefs.streakGrace),
                        fontSize = 11.sp, color = cs.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/* Singular/plural of a streak length, e.g. "3 days" but "1 day". */
private fun daysLabel(n: Int): String =
    t(if (n == 1) "card.streak.day" else "card.streak.days", "n" to n)

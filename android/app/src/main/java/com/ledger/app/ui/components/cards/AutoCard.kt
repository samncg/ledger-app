package com.ledger.app.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.ui.FREQ_OPTIONS
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.components.AppTextField
import com.ledger.app.ui.components.Btn
import com.ledger.app.ui.components.CardContainer
import com.ledger.app.ui.components.EmptyState
import com.ledger.app.ui.components.SelectField
import com.ledger.app.ui.components.DateField
import com.ledger.app.util.fmt
import com.ledger.app.util.relativeDate

/* Automations — recurring entries (spending, top-ups, balance) */
@Composable
fun AutoCard(vm: LedgerViewModel, s: LedgerState) {
    val cs = MaterialTheme.colorScheme
    var autoType by remember { mutableStateOf("expense") }
    var autoAmount by remember { mutableStateOf("") }
    var autoCat by remember { mutableStateOf("food") }
    var autoFreq by remember { mutableStateOf("monthly") }
    var autoStart by remember { mutableStateOf(s.today) }
    var autoNote by remember { mutableStateOf("") }

    CardContainer(
        title = "Automations",
        icon = Icons.Outlined.Bolt,
        count = if (s.recurring.isNotEmpty()) "(${s.recurring.size})" else null,
        trailing = if (s.recurring.isNotEmpty()) {
            {
                Text(
                    "Run now",
                    color = cs.primary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { vm.runRecurringNow() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        } else null,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectField(
                value = autoType, modifier = Modifier.weight(1f),
                options = listOf(
                    "expense" to "Spending",
                    "budget" to "Top up budget",
                ) + if (s.balancesOn) listOf("balance" to "Top up balance") else emptyList(),
                onChange = { autoType = it },
            )
            AppTextField(
                value = autoAmount,
                onChange = { autoAmount = it },
                modifier = Modifier.weight(1f),
                placeholder = "Amount",
                mono = true,
                numeric = true
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (autoType == "expense") {
                SelectField(
                    value = autoCat, modifier = Modifier.weight(1f),
                    options = s.cats.map { it.id to "${it.glyph} ${it.label}" },
                    onChange = { autoCat = it },
                )
            }
            SelectField(
                value = autoFreq, modifier = Modifier.weight(1f),
                options = FREQ_OPTIONS.toList(),
                onChange = { autoFreq = it },
            )
            DateField(value = autoStart, onChange = { autoStart = it }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextField(
                value = autoNote,
                onChange = { autoNote = it },
                modifier = Modifier.weight(1f),
                placeholder = "Note (optional)"
            )
            Btn(
                "Add",
                onClick = {
                    vm.addAutomation(autoType, autoAmount, autoCat, autoFreq, autoStart, autoNote); autoAmount =
                    ""; autoNote = ""
                },
                icon = Icons.Outlined.Add
            )
        }

        if (s.recurring.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            s.recurring.forEach { r ->
                val cat = s.cats.find { it.id == r.category }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                            .background(cs.surfaceVariant.copy(alpha = if (r.active) 1f else 0.5f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            when {
                                r.type == "expense" -> cat?.glyph ?: "◌"
                                r.type == "budget" -> if (s.balancesOn) "⇄" else "⚡"
                                else -> "+"
                            },
                            fontSize = 13.sp,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${FREQ_OPTIONS[r.freq] ?: r.freq} · ${fmt(r.amount, s.cur)} " +
                                    when (r.type) {
                                        "expense" -> "· ${cat?.label ?: r.category}"
                                        "budget" -> "to budget"
                                        else -> "to balance"
                                    },
                            fontSize = 12.5.sp, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        if (r.note.isNotEmpty()) {
                            Text(
                                r.note,
                                fontSize = 11.sp,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            "Started ${relativeDate(r.start, s.today)} · ${vm.nextRun(r)}",
                            fontSize = 10.5.sp, color = cs.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { vm.toggleAutomation(r.id) }) {
                        Icon(
                            if (r.active) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            if (r.active) "Pause" else "Resume",
                            Modifier.size(16.dp),
                            tint = cs.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { vm.removeAutomation(r.id) }) {
                        Icon(Icons.Outlined.Delete, "Remove", Modifier.size(16.dp), tint = cs.onSurfaceVariant)
                    }
                }
            }
        } else {
            EmptyState("↻", "Repeating entries appear here.", "e.g. rent on the 1st, salary on the 25th.")
        }
    }
}

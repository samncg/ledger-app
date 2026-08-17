package com.ledger.app.ui.components.cards

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.components.AppTextField
import com.ledger.app.ui.components.Btn
import com.ledger.app.ui.components.CardContainer
import com.ledger.app.ui.components.FieldLabel
import com.ledger.app.util.fmt

/* Piggy bank — savings goal with progress */
@Composable
fun PiggyCard(vm: LedgerViewModel, s: LedgerState) {
    val cs = MaterialTheme.colorScheme
    var editingGoal by remember { mutableStateOf(false) }
    var goalDraft by remember { mutableStateOf("") }
    var addOpen by remember { mutableStateOf(false) }
    var piggyAmount by remember { mutableStateOf("") }

    CardContainer(
        title = "Piggy bank",
        icon = Icons.Outlined.Wallet,
        count = if (s.piggy.target > 0) "${Math.round(s.piggyPct)}%" else null,
        trailing = if (!editingGoal) {
            {
                TextButton(onClick = {
                    goalDraft = if (s.piggy.target > 0) s.piggy.target.toString() else ""; editingGoal = true
                }) {
                    Text("Set goal", fontSize = 12.5.sp)
                }
            }
        } else null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            /* 🐷 emoji stands in for the web app's flying-piggy GIF */
            Box(
                Modifier.size(width = 120.dp, height = 90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text("🐷", fontSize = 52.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                if (editingGoal) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppTextField(
                            value = goalDraft, onChange = { goalDraft = it }, mono = true, numeric = true,
                            placeholder = "200", modifier = Modifier.weight(1f),
                            onDone = { vm.savePiggyTarget(goalDraft); editingGoal = false },
                        )
                        Btn(
                            "",
                            onClick = { vm.savePiggyTarget(goalDraft); editingGoal = false },
                            small = true,
                            icon = Icons.Outlined.Check
                        )
                        Btn(
                            "",
                            onClick = { editingGoal = false },
                            variant = "ghost",
                            small = true,
                            icon = Icons.Outlined.Close
                        )
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            if (s.piggy.target > 0) "Saved" else "No goal yet",
                            fontSize = 12.sp,
                            color = cs.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                fmt(s.piggy.saved, s.cur),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            if (s.piggy.target > 0) {
                                Text(
                                    " / ${fmt(s.piggy.target, s.cur)}",
                                    fontSize = 11.5.sp,
                                    color = cs.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(cs.surfaceVariant)) {
                    Box(
                        Modifier.fillMaxWidth((s.piggyPct / 100).toFloat().coerceIn(0f, 1f)).height(8.dp)
                            .background(cs.primary, CircleShape),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (s.piggy.target > 0)
                        if (s.piggy.saved >= s.piggy.target) "Goal complete! 🎉" else "${
                            fmt(
                                s.piggy.target - s.piggy.saved,
                                s.cur
                            )
                        } to go"
                    else "Set a goal and watch it fill up.",
                    fontSize = 11.sp, color = cs.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                if (addOpen) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppTextField(
                            value = piggyAmount, onChange = { piggyAmount = it }, mono = true, numeric = true,
                            placeholder = "20", modifier = Modifier.weight(1f),
                            onDone = { vm.depositPiggy(piggyAmount); addOpen = false },
                        )
                        Btn("Add", onClick = { vm.depositPiggy(piggyAmount); addOpen = false }, small = true)
                        Btn(
                            "",
                            onClick = { addOpen = false },
                            variant = "ghost",
                            small = true,
                            icon = Icons.Outlined.Close
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Btn(
                            "Add",
                            onClick = { addOpen = true },
                            variant = "secondary",
                            small = true,
                            icon = Icons.Outlined.Add
                        )
                        Btn(
                            "Break",
                            onClick = vm::breakPiggy,
                            variant = "ghost",
                            small = true,
                            enabled = s.piggy.saved > 0
                        )
                    }
                }
            }
        }
    }
}

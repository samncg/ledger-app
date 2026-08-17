package com.ledger.app.ui.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.components.AppTextField
import com.ledger.app.ui.components.Btn
import com.ledger.app.ui.components.CardContainer
import com.ledger.app.ui.components.CatChip
import com.ledger.app.ui.components.ChipFlow
import com.ledger.app.ui.components.DateField
import com.ledger.app.ui.components.FieldLabel
import com.ledger.app.ui.components.SectionDesc
import com.ledger.app.ui.components.SmallChip
import com.ledger.app.util.symbol

/* Log a spend — quick-log form with frequent suggestions */
@Composable
fun LogCard(vm: LedgerViewModel, s: LedgerState) {
    CardContainer(
        title = if (vm.editingId != null) "Edit spend" else "Log a spend",
        icon = Icons.Outlined.Wallet,
        trailing = if (vm.editingId != null) {
            { SmallChip("Editing") }
        } else null,
    ) {
        if (vm.editingId == null && s.expenses.isEmpty()) {
            SectionDesc("Welcome — log your first spend to start tracking.")
            Spacer(Modifier.height(10.dp))
        }
        if (vm.editingId == null && s.frequentEntries.isNotEmpty()) {
            FieldLabel("Frequent")
            Spacer(Modifier.height(6.dp))
            ChipFlow {
                s.frequentEntries.forEach { f ->
                    val fc = s.cats.find { it.id == f.category }
                    CatChip(
                        label = "${f.note.ifEmpty { fc?.label ?: f.category }} · ${symbol(s.cur)}${f.amount}",
                        dotColor = fc?.color,
                        selected = false,
                        onClick = {
                            vm.amount = f.amount.toString()
                            vm.selCats = listOf(f.category)
                            vm.note = f.note
                        },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        FieldLabel("Amount")
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextField(
                value = vm.amount, onChange = { vm.amount = it },
                modifier = Modifier.weight(1f), placeholder = "0.00", mono = true, numeric = true,
                onDone = { if (vm.editingId != null) vm.updateExpense() else vm.addExpense() },
            )
            AppTextField(
                value = vm.note, onChange = { vm.note = it },
                modifier = Modifier.weight(1.2f), placeholder = "Note (optional)",
                onDone = { if (vm.editingId != null) vm.updateExpense() else vm.addExpense() },
            )
        }
        Spacer(Modifier.height(8.dp))
        ChipFlow {
            listOf(5, 10, 15, 20, 50, 100).forEach { v ->
                CatChip(
                    label = "${symbol(s.cur)}$v",
                    dotColor = null,
                    selected = false,
                    onClick = { vm.amount = v.toString() })
            }
        }

        Spacer(Modifier.height(12.dp))
        FieldLabel("Date")
        Spacer(Modifier.height(6.dp))
        DateField(value = vm.entryDate, onChange = { vm.entryDate = it }, maxDate = s.today)

        Spacer(Modifier.height(12.dp))
        FieldLabel("Categories — pick one or more")
        Spacer(Modifier.height(6.dp))
        ChipFlow {
            s.cats.forEach { c ->
                CatChip(
                    label = c.label,
                    dotColor = c.color,
                    selected = vm.selCats.contains(c.id),
                    onClick = { vm.toggleSelCat(c.id) })
            }
        }

        Spacer(Modifier.height(14.dp))
        if (vm.editingId != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(
                    "Update",
                    onClick = vm::updateExpense,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Check,
                    enabled = vm.amount.toDoubleOrNull()?.let { it > 0 } ?: false)
                Btn("Cancel", onClick = vm::cancelEdit, variant = "ghost")
            }
        } else {
            Btn(
                "Add spend",
                onClick = vm::addExpense,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Outlined.Add,
                enabled = vm.amount.toDoubleOrNull()?.let { it > 0 } ?: false)
        }
    }
}

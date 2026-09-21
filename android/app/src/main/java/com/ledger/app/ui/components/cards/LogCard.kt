package com.ledger.app.ui.components.cards

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.ledger.app.ui.components.ReceiptThumbnail
import com.ledger.app.ui.components.SectionDesc
import com.ledger.app.ui.components.SmallChip
import com.ledger.app.ui.t
import com.ledger.app.util.symbol

/* Log a spend — quick-log form with frequent + smart suggestions and receipt photos */
@Composable
fun LogCard(vm: LedgerViewModel, s: LedgerState) {
    val context = LocalContext.current
    val pickReceipt = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.setReceiptFromUri(context, uri)
    }
    var tagDraft by remember { mutableStateOf("") }
    /* While travelling the typed figure is the foreign amount, so the quick-amount
       chips and the amount label follow the trip's currency. */
    /* While editing, the amount field follows the currency that entry was logged in, so an
       older entry is never re-expressed in the trip's current currency. */
    val editing = vm.editingId?.let { id -> s.expenses.find { it.id == id } }
    val abroad = if (vm.editingId != null) editing?.currency != null else s.travelActive
    val cur = when {
        editing?.currency != null -> editing.currency
        abroad -> s.travel.currency
        else -> s.cur
    }

    CardContainer(
        title = if (vm.editingId != null) t("log.edit") else t("log.title"),
        icon = Icons.Outlined.Wallet,
        trailing = if (vm.editingId != null) {
            { SmallChip(t("log.editing")) }
        } else null,
    ) {
        if (vm.editingId == null && s.expenses.isEmpty()) {
            SectionDesc(t("log.welcomeFirst"))
            Spacer(Modifier.height(10.dp))
        }
        if (vm.editingId == null && s.frequentEntries.isNotEmpty()) {
            FieldLabel(t("log.frequent"))
            Spacer(Modifier.height(6.dp))
            ChipFlow {
                s.frequentEntries.forEach { f ->
                    val fc = s.cats.find { it.id == f.category }
                    CatChip(
                        label = "${f.note.ifEmpty { fc?.label ?: f.category }} · ${symbol(cur)}${f.amount}",
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

        FieldLabel(if (abroad) t("travel.foreignAmount", "currency" to cur) else t("log.amount"))
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextField(
                value = vm.amount, onChange = { vm.amount = it },
                modifier = Modifier.weight(1f), placeholder = "0.00", mono = true, numeric = true,
                onDone = { if (vm.editingId != null) vm.updateExpense() else vm.addExpense() },
            )
            AppTextField(
                value = vm.note, onChange = { vm.onNoteChange(it) },
                modifier = Modifier.weight(1.2f), placeholder = t("log.notePlaceholder"),
                onDone = { if (vm.editingId != null) vm.updateExpense() else vm.addExpense() },
            )
        }
        if (abroad && s.travelRate > 0) {
            Spacer(Modifier.height(4.dp))
            SectionDesc("≈ ${symbol(s.cur)}${"%.2f".format((vm.amount.toDoubleOrNull() ?: 0.0) * s.travelRate)} ${t("travel.homeEquivalent").lowercase()}")
        }
        Spacer(Modifier.height(8.dp))
        ChipFlow {
            listOf(5, 10, 15, 20, 50, 100).forEach { v ->
                CatChip(
                    label = "${symbol(cur)}$v",
                    dotColor = null,
                    selected = false,
                    onClick = { vm.amount = v.toString() })
            }
        }

        Spacer(Modifier.height(12.dp))
        FieldLabel(t("log.date"))
        Spacer(Modifier.height(6.dp))
        DateField(value = vm.entryDate, onChange = { vm.entryDate = it }, maxDate = s.today)

        Spacer(Modifier.height(12.dp))
        FieldLabel("${t("log.tags")} (${t("log.optional")})")
        Spacer(Modifier.height(6.dp))
        if (vm.tags.isNotEmpty()) {
            ChipFlow {
                vm.tags.forEach { tg ->
                    CatChip(label = "#$tg  ✕", dotColor = null, selected = true, onClick = { vm.removeTag(tg) })
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AppTextField(
                value = tagDraft, onChange = { tagDraft = it },
                modifier = Modifier.weight(1f), placeholder = t("log.tagPlaceholder"),
                onDone = { vm.addTag(tagDraft); tagDraft = "" },
            )
            Btn(
                "Add",
                onClick = { vm.addTag(tagDraft); tagDraft = "" },
                variant = "ghost",
                small = true,
                enabled = tagDraft.isNotBlank(),
            )
        }

        Spacer(Modifier.height(12.dp))
        FieldLabel("${t("log.categories")} — ${t("log.pickOne")}")
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

        Spacer(Modifier.height(12.dp))
        FieldLabel("${t("log.receipt")} (${t("log.optional")})")
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (vm.receipt != null) {
                ReceiptThumbnail(vm.receipt, size = 56)
                Btn(t("log.remove"), onClick = vm::clearReceipt, variant = "ghost", small = true)
            } else {
                Btn(
                    t("log.attachPhoto"),
                    onClick = { pickReceipt.launch("image/*") },
                    variant = "ghost",
                    small = true,
                    icon = Icons.Outlined.Image,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        if (vm.editingId != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(
                    t("log.update"),
                    onClick = vm::updateExpense,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Check,
                    enabled = vm.amount.toDoubleOrNull()?.let { it > 0 } ?: false)
                Btn(t("log.cancel"), onClick = vm::cancelEdit, variant = "ghost")
            }
        } else {
            Btn(
                t("log.addSpend"),
                onClick = vm::addExpense,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Outlined.Add,
                enabled = vm.amount.toDoubleOrNull()?.let { it > 0 } ?: false)
        }
    }
}

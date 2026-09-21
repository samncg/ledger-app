package com.ledger.app.ui.components.cards

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.components.Btn
import com.ledger.app.ui.components.CardContainer
import com.ledger.app.ui.t
import java.io.IOException

/* Data & backup — JSON / CSV export, import, budget & money shortcuts */
@Composable
fun BackupCard(
    vm: LedgerViewModel,
    s: LedgerState,
    onEditBudget: () -> Unit,
    onMoveMoney: () -> Unit,
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportJson().toByteArray()) }
                vm.showToast(t("card.backup.downloaded"), "success")
            } catch (e: IOException) {
                vm.showToast(t("card.backup.writeBackupFailed"), "error")
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportCsv().toByteArray()) }
                vm.showToast(t("card.backup.csvExported"), "success")
            } catch (e: IOException) {
                vm.showToast(t("card.backup.writeCsvFailed"), "error")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: return@rememberLauncherForActivityResult
                val error = vm.importData(text)
                if (error != null) vm.showToast(error, "error")
            } catch (e: Exception) {
                vm.showToast(t("card.backup.readFailed"), "error")
            }
        }
    }

    CardContainer(title = t("card.backup.title"), icon = Icons.Outlined.Storage) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(
                    t("card.backup.backupJson"),
                    onClick = { exportJsonLauncher.launch("ledger-backup-${s.today}.json") },
                    variant = "secondary",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Download
                )
                Btn(
                    t("card.backup.exportCsv"),
                    onClick = { exportCsvLauncher.launch("ledger-export-${s.today}.csv") },
                    variant = "secondary",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Download
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(
                    t("card.backup.loadBackup"),
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    variant = "ghost",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Upload
                )
                Btn(
                    t("card.backup.editBudget"),
                    onClick = onEditBudget,
                    variant = "ghost",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Edit,
                )
            }
            Btn(
                if (s.balancesOn) t("card.backup.moveMoney") else t("card.backup.topUp"),
                onClick = onMoveMoney,
                variant = "ghost",
                modifier = Modifier.fillMaxWidth(),
                icon = if (s.balancesOn) Icons.Outlined.Wallet else Icons.Outlined.Bolt,
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            val footerText = if (s.authUser != null) {
                t(
                    "card.backup.cloudActive",
                    "who" to (s.authUser.email ?: s.authUser.name ?: t("card.backup.signedIn"))
                )
            } else {
                t("card.backup.localOnly")
            }
            Text(
                footerText,
                fontSize = 11.sp, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.height(0.dp))
            Btn(
                t("card.backup.clearAll"),
                onClick = vm::clearAll,
                variant = "danger",
                small = true,
                icon = Icons.Outlined.Delete
            )
        }
    }
}

package com.ledger.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.ui.HistoryEntry
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.components.GlassScreenBackground
import com.ledger.app.ui.components.GlassStyle
import com.ledger.app.ui.components.LocalGlassStyle
import com.ledger.app.ui.components.cards.HistoryCard
import com.ledger.app.ui.components.cards.LogCard

/* ─── Log a spend — full-screen view with a glass backdrop ─── */
@Composable
fun LogScreen(vm: LedgerViewModel, s: LedgerState, onClose: () -> Unit) {
    GlassScreenBackground {
        CompositionLocalProvider(LocalGlassStyle provides GlassStyle()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                ScreenHeader("Log a spend")
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 88.dp),
                ) {
                    LogCard(vm, s)
                }
            }
        }
    }
}

/* ─── Spending history — full-screen view with a glass backdrop ─── */
@Composable
fun HistoryScreen(
    vm: LedgerViewModel,
    s: LedgerState,
    onClose: () -> Unit,
    onEditEntry: (HistoryEntry) -> Unit,
) {
    GlassScreenBackground {
        CompositionLocalProvider(LocalGlassStyle provides GlassStyle()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                ScreenHeader("History")
                Column(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 88.dp)
                        .weight(1f),
                ) {
                    HistoryCard(vm, s, expand = true, onEditEntry = onEditEntry)
                }
            }
        }
    }
}

@Composable
private fun ScreenHeader(title: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.padding(start = 4.dp), fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

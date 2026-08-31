package com.ledger.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.components.AppTextField
import com.ledger.app.ui.components.Btn
import com.ledger.app.ui.components.DateField
import com.ledger.app.ui.components.FieldLabel
import com.ledger.app.ui.components.SectionDesc
import com.ledger.app.ui.components.SelectField
import com.ledger.app.ui.components.ToastOverlay
import com.ledger.app.ui.parseColor
import com.ledger.app.util.CURRENCIES
import com.ledger.app.util.daysInMonth
import com.ledger.app.util.firstOfMonthKey

/* ─── Setup — first-run budget creation ─── */
@Composable
fun SetupScreen(vm: LedgerViewModel, s: LedgerState) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var budget by remember { mutableStateOf("") }
    var days by remember { mutableStateOf(daysInMonth().toString()) }
    var startDate by remember { mutableStateOf(firstOfMonthKey()) }
    var currency by remember { mutableStateOf("MYR") }
    var balance by remember { mutableStateOf("") }

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
                vm.showToast("Couldn't read that file.", "error")
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                vm.signInWithGoogleToken(idToken)
            } else {
                vm.showToast("Couldn't retrieve Google ID token.", "error")
            }
        } catch (e: ApiException) {
            if (e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                vm.showToast("Sign-in cancelled.", "info")
            } else {
                vm.showToast("Google sign-in error (${e.statusCode}): ${e.localizedMessage ?: ""}", "error")
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(cs.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text("L", color = cs.onPrimary, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(16.dp))
            Text("Welcome to Ledger", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                if (s.balancesOn) "Set a bank balance, track your daily allowance,\nand bank whatever you don't spend each day."
                else "Set your budget, track your daily allowance,\nand carry over what you don't spend.",
                fontSize = 13.sp, textAlign = TextAlign.Center, color = cs.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column {
                    FieldLabel("Budget amount")
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectField(
                            value = currency, modifier = Modifier.weight(1f),
                            options = CURRENCIES.toList().map { (k, v) -> k to "${v.symbol} $k" },
                            onChange = { currency = it },
                        )
                        AppTextField(
                            value = budget,
                            onChange = { budget = it },
                            modifier = Modifier.weight(1.5f),
                            placeholder = "600",
                            mono = true,
                            numeric = true
                        )
                    }
                }
                if (s.balancesOn) {
                    Column {
                        FieldLabel("Starting bank balance")
                        Spacer(Modifier.height(6.dp))
                        AppTextField(
                            value = balance,
                            onChange = { balance = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "1000",
                            mono = true,
                            numeric = true
                        )
                        Spacer(Modifier.height(4.dp))
                        SectionDesc("The money you have right now — you can move it into your budget whenever you need it.")
                    }
                }
                Column {
                    FieldLabel("Period length (days)")
                    Spacer(Modifier.height(6.dp))
                    AppTextField(
                        value = days,
                        onChange = { days = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = daysInMonth().toString(),
                        mono = true,
                        numeric = true
                    )
                }
                Column {
                    FieldLabel("Start date")
                    Spacer(Modifier.height(6.dp))
                    DateField(value = startDate, onChange = { startDate = it }, maxDate = s.today)
                }
            }

            Spacer(Modifier.height(18.dp))
            Btn(
                "Start tracking",
                onClick = { vm.saveSetup(budget, days, startDate, currency, balance) },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Outlined.Add
            )

            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(1.dp).background(cs.outline))
                Text("or", Modifier.padding(horizontal = 10.dp), fontSize = 11.sp, color = cs.onSurfaceVariant)
                Box(Modifier.weight(1f).height(1.dp).background(cs.outline))
            }

            Btn(
                "Restore from a backup",
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                variant = "ghost",
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Outlined.Upload
            )

            if (s.isFirebaseConfigured) {
                Spacer(Modifier.height(8.dp))
                Btn(
                    "Sign in with Google",
                    onClick = {
                        findActivity(context)?.let { vm.signInGoogle(it, googleSignInLauncher) }
                    },
                    variant = "secondary",
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Outlined.Cloud
                )
            }

            Spacer(Modifier.height(10.dp))
            SectionDesc("Already have a ledger-backup .json file or cloud account? Load it to pick up right where you left off.")
            Spacer(Modifier.height(24.dp))
        }

        ToastOverlay(
            toast = vm.toast,
            dotColor = when (vm.toast?.type) {
                "success" -> parseColor(s.theme.positive) ?: cs.primary
                "error" -> parseColor(s.theme.negative) ?: cs.error
                else -> parseColor(s.theme.accent) ?: cs.primary
            },
            onDismiss = vm::dismissToast,
        )
    }
}

private fun findActivity(context: Context): Activity? {
    var ctx = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

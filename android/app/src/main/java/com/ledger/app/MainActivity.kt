package com.ledger.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ledger.app.data.Repository
import com.ledger.app.ui.LedgerTheme
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.screens.DashboardScreen
import com.ledger.app.ui.screens.SetupScreen

class MainActivity : ComponentActivity() {

    private var vmRef: LedgerViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: LedgerViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { LedgerViewModel(Repository(applicationContext)) }
                },
            )
            vmRef = vm
            val state by vm.state.collectAsState()

            if (!state.ready) {
                Box(Modifier.fillMaxSize().background(Color(0xFF0a0a0a)))
                return@setContent
            }

            /* Keep status-bar icons legible on light/dark themes. */
            LaunchedEffect(state.theme) {
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightStatusBars = !vm.isDark(state.theme)
            }

            LedgerTheme(theme = state.theme, fontId = state.prefs.font) {
                if (state.settings == null) {
                    SetupScreen(vm, state)
                } else {
                    DashboardScreen(vm, state)
                }
            }
        }
    }

    /* Recompute derived values on resume so the date rolls over. */
    override fun onResume() {
        super.onResume()
        vmRef?.refresh()
    }
}

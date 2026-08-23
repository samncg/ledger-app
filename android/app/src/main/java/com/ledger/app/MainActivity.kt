package com.ledger.app

import android.Manifest
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ledger.app.data.Repository
import com.ledger.app.ui.LedgerTheme
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.components.GlassStyle
import com.ledger.app.ui.components.LocalGlassBackdrop
import com.ledger.app.ui.components.LocalGlassStyle
import com.ledger.app.ui.parseColor
import com.ledger.app.ui.screens.DashboardScreen
import com.ledger.app.ui.screens.SetupScreen
import com.ledger.app.util.NotificationHelper
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.backdrops.LayerBackdrop
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop

class MainActivity : ComponentActivity() {

    private var vmRef: LedgerViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openLogInitially = intent?.getBooleanExtra(NotificationHelper.EXTRA_OPEN_LOG, false) ?: false

        setContent {
            val vm: LedgerViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { LedgerViewModel(Repository(applicationContext)) }
                },
            )
            vmRef = vm
            val state by vm.state.collectAsState()

            // Request POST_NOTIFICATIONS permission on Android 13+ if enabled
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted && state.prefs.notificationsEnabled) {
                        vm.updatePrefs { it.copy(notificationsEnabled = false) }
                    }
                }
                LaunchedEffect(state.prefs.notificationsEnabled) {
                    if (state.prefs.notificationsEnabled) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

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
                val glassBackdrop = rememberLayerBackdrop()
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalGlassStyle provides GlassStyle(
                        enabled = state.prefs.glassEnabled,
                        blur = state.prefs.glassBlur,
                        opacity = state.prefs.glassOpacity,
                        refraction = state.prefs.glassRefraction,
                        refractionHeight = state.prefs.glassRefractionHeight,
                        chromaticAberration = state.prefs.glassChromaticAberration
                    ),
                    LocalGlassBackdrop provides glassBackdrop
                ) {
                    Box(Modifier.fillMaxSize()) {
                        WallpaperBackdrop(
                            wallpaperPath = state.prefs.wallpaper,
                            wallpaperDim = state.prefs.wallpaperDim,
                            wallBlur = state.prefs.wallBlur,
                            themeBg = state.theme.bg,
                            backdrop = glassBackdrop
                        )
                        if (state.settings == null) {
                            SetupScreen(vm, state)
                        } else {
                            DashboardScreen(vm, state, initialShowLog = openLogInitially)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    /* Recompute derived values on resume so the date rolls over. */
    override fun onResume() {
        super.onResume()
        vmRef?.refresh()
    }
}

@Composable
fun WallpaperBackdrop(
    wallpaperPath: String?,
    wallpaperDim: Int,
    wallBlur: Int,
    themeBg: String,
    backdrop: LayerBackdrop? = null,
) {
    val bgColor = parseColor(themeBg) ?: Color.Black
    val dimAlpha = (wallpaperDim.coerceIn(0, 90) / 100f)
    val bitmap = remember(wallpaperPath) {
        if (wallpaperPath.isNullOrEmpty()) null
        else try {
            BitmapFactory.decodeFile(wallpaperPath)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
    ) {
        // Base theme background with subtle depth so liquid glass always refracts light
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            bgColor,
                            Color(0xFF06080B),
                            Color.Black
                        ),
                        center = androidx.compose.ui.geometry.Offset(300f, 400f),
                        radius = 1200f
                    )
                )
        )

        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (wallBlur > 0) Modifier.blur(wallBlur.dp) else Modifier)
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(bgColor.copy(alpha = dimAlpha))
            )
        }
    }
}

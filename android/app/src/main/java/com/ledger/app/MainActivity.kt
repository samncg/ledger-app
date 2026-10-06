package com.ledger.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ledger.app.data.Repository
import com.ledger.app.ui.LedgerTheme
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.autoWallpaperIsWhite
import com.ledger.app.ui.components.GlassStyle
import com.ledger.app.ui.components.LocalGlassBackdrop
import com.ledger.app.ui.components.LocalGlassStyle
import com.ledger.app.ui.components.LedgerSplash
import com.ledger.app.ui.parseColor
import com.ledger.app.ui.screens.DashboardScreen
import com.ledger.app.ui.screens.LockScreen
import com.ledger.app.ui.screens.SetupScreen
import com.ledger.app.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.backdrops.LayerBackdrop
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop

class MainActivity : ComponentActivity() {

    private var vmRef: LedgerViewModel? = null

    /* App lock state — Compose reads these to show/refresh the lock overlay. */
    private var unlocked by mutableStateOf(false)
    private var lockEpoch by mutableStateOf(0)
    private var lockPromptInFlight = false
    private var unlockLauncher: ActivityResultLauncher<Intent>? = null

    /* POST_NOTIFICATIONS: asked at most once per launch, and only while this screen is resumed. */
    private var notifAskDone = false
    private var notifLauncher: ActivityResultLauncher<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep the home-screen widget fresh on a 15-minute cadence.
        LedgerWidget.scheduleRefresh(applicationContext)

        unlockLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            lockPromptInFlight = false
            unlocked = result.resultCode == RESULT_OK
        }

        notifLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted && vmRef?.state?.value?.prefs?.notificationsEnabled == true) {
                vmRef?.updatePrefs { it.copy(notificationsEnabled = false) }
            }
        }

        val openLogInitially = intent?.getBooleanExtra(NotificationHelper.EXTRA_OPEN_LOG, false) ?: false

        setContent {
            val vm: LedgerViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { LedgerViewModel(Repository(applicationContext)) }
                },
            )
            vmRef = vm
            val state by vm.state.collectAsState()

            /* The POST_NOTIFICATIONS request itself goes out from onResume (see
               maybeAskForNotifications). Asking from composition runs before the activity is
               resumed, and used to race the lock activity taking focus, so the system dropped the
               dialog or dismissed it immediately and it could not be answered. This only re-checks
               when the pref or the lock state changes. */
            /* `state.ready` is a key on purpose: until the stored prefs have loaded, `prefs` is
               the default object, and the readiness flip is the moment the real value of
               `notificationsEnabled` becomes known — without it, a cold start that resumes
               before the DataStore read finishes would never ask at all. */
            LaunchedEffect(
                state.ready,
                state.prefs.notificationsEnabled,
                state.prefs.appLockEnabled,
                unlocked,
            ) {
                maybeAskForNotifications()
            }

            val appLockOn = state.prefs.appLockEnabled
            // Prompt automatically whenever the app is locked (cold start, or after
            // returning from the background). A cancelled prompt is not retried
            // automatically, so the user can always tap "Unlock" themselves.
            LaunchedEffect(appLockOn, unlocked, lockEpoch) {
                if (appLockOn && !unlocked && !lockPromptInFlight) {
                    lockPromptInFlight = true
                    unlockLauncher?.launch(Intent(this@MainActivity, LockActivity::class.java))
                }
            }

            /* Keep status-bar and navigation-bar icons legible on light/dark themes. */
            LaunchedEffect(state.theme) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                val isLight = !vm.isDark(state.theme)
                insetsController.isAppearanceLightStatusBars = isLight
                insetsController.isAppearanceLightNavigationBars = isLight
            }

            LedgerTheme(theme = state.theme, fontId = state.prefs.font) {
                val glassBackdrop = rememberLayerBackdrop()
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalGlassStyle provides GlassStyle(
                        enabled = state.prefs.glassEnabled,
                        screensGlass = state.prefs.glassScreens,
                        insideGlass = state.prefs.glassScreensInside,
                        blur = state.prefs.glassBlur,
                        barBlur = state.prefs.glassBarBlur,
                        opacity = state.prefs.glassOpacity,
                        barGlass = state.prefs.glassBar,
                        barOpacity = state.prefs.glassBarOpacity,
                        refraction = state.prefs.glassRefraction,
                        refractionHeight = state.prefs.glassRefractionHeight,
                        chromaticAberration = state.prefs.glassChromaticAmount / 100f,
                        innerOpacity = state.prefs.glassInnerOpacity
                    ),
                    LocalGlassBackdrop provides glassBackdrop
                ) {
                    Box(Modifier.fillMaxSize()) {
                        WallpaperBackdrop(
                            wallpaperPath = state.prefs.wallpaper,
                            wallpaperDim = state.prefs.wallpaperDim,
                            wallBlur = state.prefs.wallBlur,
                            themeBg = state.theme.bg,
                            autoIsWhite = autoWallpaperIsWhite(state.theme, state.categories),
                            backdrop = glassBackdrop
                        )
                        /* Nothing renders until the stored data has loaded, otherwise the
                           setup screen would flash before the real state arrives. */
                        if (state.ready) {
                            if (state.settings == null) {
                                SetupScreen(vm, state)
                            } else {
                                DashboardScreen(vm, state, initialShowLog = openLogInitially)
                            }
                            if (appLockOn && !unlocked) {
                                LockScreen(onUnlock = {
                                    if (!lockPromptInFlight) {
                                        lockPromptInFlight = true
                                        unlockLauncher?.launch(Intent(this@MainActivity, LockActivity::class.java))
                                    }
                                })
                            }
                        }
                        /* Intro splash — stays composed so it can fade away over the app
                           once the stored data is ready, instead of vanishing instantly. */
                        AnimatedVisibility(
                            visible = !state.ready,
                            enter = EnterTransition.None,
                            exit = fadeOut(tween(340)),
                        ) {
                            LedgerSplash()
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
        maybeAskForNotifications()
    }

    /**
     * Ask for POST_NOTIFICATIONS — at most once per launch, and only with this screen actually in
     * front and unlocked. Asking before the activity is resumed, or while the lock activity holds
     * focus, makes the system drop the dialog or dismiss it straight away, which is why the prompt
     * used to appear with nothing the user could press.
     */
    private fun maybeAskForNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (notifAskDone) return
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        val st = vmRef?.state?.value ?: return
        /* Until the stored prefs have loaded, `prefs` is still the default object, whose
           notificationsEnabled is true — asking then would prompt users who turned it off. */
        if (!st.ready) return
        if (!st.prefs.notificationsEnabled) return
        /* Never over the lock screen — the dialog has to be answerable. */
        if (st.prefs.appLockEnabled && !unlocked) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notifAskDone = true
        notifLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /* Re-lock the app once it has actually gone to the background. */
    override fun onStop() {
        super.onStop()
        if (vmRef?.state?.value?.prefs?.appLockEnabled == true) {
            unlocked = false
            lockEpoch++
        }
        // Keep the home-screen widgets in step with the latest data.
        LedgerWidget.refresh(this)
        WidgetRefresher.refreshNew(this)
    }
}

/**
 * Decode the wallpaper, bounded to roughly the size it is actually drawn at.
 *
 * `inJustDecodeBounds` reads only the header first, so a camera photo (which decodes to tens of
 * megabytes) is never held at full resolution just to be drawn a few hundred dp wide.
 */
private fun decodeWallpaper(path: String): ImageBitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    val longest = maxOf(bounds.outWidth, bounds.outHeight)
    var sample = 1
    while (longest / sample > 2048) sample *= 2
    BitmapFactory.decodeFile(
        path,
        BitmapFactory.Options().apply { inSampleSize = sample },
    )?.asImageBitmap()
} catch (e: Exception) {
    null
}

/** The wallpaper the app ships with, for anyone who never picked one. */
private fun decodeDefaultWallpaper(res: android.content.res.Resources): ImageBitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(res, R.drawable.default_wallpaper, bounds)
    val longest = maxOf(bounds.outWidth, bounds.outHeight)
    var sample = 1
    while (longest / sample > 2048) sample *= 2
    BitmapFactory.decodeResource(
        res,
        R.drawable.default_wallpaper,
        BitmapFactory.Options().apply { inSampleSize = sample },
    )?.asImageBitmap()
} catch (e: Exception) {
    null
}

@Composable
fun WallpaperBackdrop(
    wallpaperPath: String?,
    wallpaperDim: Int,
    wallBlur: Int,
    themeBg: String,
    /* Which solid the "auto" wallpaper resolves to for the current theme. */
    autoIsWhite: Boolean = true,
    backdrop: LayerBackdrop? = null,
) {
    val bgColor = parseColor(themeBg) ?: Color.Black
    val dimAlpha = (wallpaperDim.coerceIn(0, 90) / 100f)
    /* The stored value is a preset id ("auto" / "white" / "black" / "forest"), a custom file
       path, or absent — which means "auto". */
    val mode = wallpaperPath?.takeIf { it.isNotBlank() } ?: "auto"
    val solid = when (mode) {
        "auto" -> if (autoIsWhite) Color(0xFFFFFFFF) else Color(0xFF000000)
        "white" -> Color(0xFFFFFFFF)
        "black" -> Color(0xFF000000)
        else -> null
    }
    /* A flat preset needs no bitmap at all; "forest" is the photo the app ships with, anything
       else is a path to a custom image. Decoded off the composition thread and downsampled —
       inline it blocked the first frame of every cold start on a full-resolution decode. */
    val context = LocalContext.current
    var bitmap by remember(mode) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(mode) {
        if (solid != null) {
            bitmap = null
            return@LaunchedEffect
        }
        bitmap = withContext(Dispatchers.IO) {
            if (mode == "forest") decodeDefaultWallpaper(context.resources)
            else decodeWallpaper(mode)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
    ) {
        if (solid != null) {
            // A flat white / black wallpaper: one sheet, no photo and no dimming.
            Box(Modifier.fillMaxSize().background(solid))
        } else {
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

            val shown = bitmap
            if (shown != null) {
                Image(
                    bitmap = shown,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (wallBlur > 0) Modifier.blur(wallBlur.dp) else Modifier)
                )
            }

            if (bitmap != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(bgColor.copy(alpha = dimAlpha))
                )
            }
        }
    }
}

@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

@Composable
fun MagicAppNavigation(onPurchaseRequested: (String) -> Unit) {
    val context = LocalContext.current
    val prefs: SharedPreferences = remember { MagicPrefsFactory.create(context) }
    val locManager = remember { MagicLocalizationManager(context) }

    val state = rememberMagicNavigationState(context = context, prefs = prefs)

    val currentPassword = remember(state.currentScreen) {
        prefs.getString("custom_wall_password", MagicSecretBypass.WALL_PASSWORD) ?: MagicSecretBypass.WALL_PASSWORD
    }

    var hasMicPermission by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasMicPermission = isGranted
        state.currentScreen = "wall"
    }

    val speechTools = remember { MagicSpeechTools(context) }

    MagicAppEffectsObserver(context = context, prefs = prefs, state = state, speechTools = speechTools)
    MagicSpeechProcessor(context = context, prefs = prefs, state = state, speechTools = speechTools, currentPassword = currentPassword)

    BackHandler(enabled = state.currentScreen == "main_hub" || state.currentScreen == "wall") {
        if (state.currentScreen == "main_hub" && state.isHubCombatMode) {
            state.isHubCombatMode = false
            state.activeSpellId = ""
            state.currentLiveSpeechText = ""
            state.currentTorchLevel = 0
            speechTools.setTorchStrength(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        when (state.currentScreen) {
            "splash_logo" -> {
                VideoBackground(
                    videoRawId = R.raw.logo,
                    zoomLevel = 0,
                    isWallLoop = false,
                    isMutedByDefault = true,
                    onVideoEnd = { state.currentScreen = "splash_intro" }
                )
            }
            "splash_intro" -> {
                VideoBackground(
                    videoRawId = R.raw.intro,
                    zoomLevel = 0,
                    isWallLoop = false,
                    isMutedByDefault = false,
                    onVideoEnd = {
                        state.currentScreen = if (!prefs.getBoolean("has_accepted_disclaimer", false)) "language"
                        else if (!prefs.getBoolean("has_read_instruction", false)) "instruction"
                        else if (state.isWallUnlocked) "main_hub"
                        else "wall"
                    }
                )
            }
            "language" -> {
                MagicLanguageSelectionScreen(
                    state = state,
                    locManager = locManager,
                    onLanguageSelected = { state.currentScreen = "disclaimer" }
                )
            }
            "disclaimer" -> {
                MagicDisclaimerScreen(
                    locManager = locManager,
                    onBackToLanguage = { state.currentScreen = "language" },
                    onAccepted = {
                        prefs.edit { putBoolean("has_accepted_disclaimer", true) }
                        state.currentScreen = "instruction"
                    }
                )
            }
            "instruction" -> {
                MagicInstructionScreen(currentPassword, locManager) {
                    prefs.edit { putBoolean("has_read_instruction", true) }
                    if (!hasMicPermission) launcher.launch(Manifest.permission.RECORD_AUDIO)
                    else state.currentScreen = "wall"
                }
            }
            "wall" -> {
                MagicWallScreen(
                    wallState = state.wallState,
                    zoomLevel = prefs.getInt("zoom_level_entry", 1),
                    isSleepingExternal = state.isWallSleeping,
                    speechTools = speechTools,
                    onWakeUpRequested = { state.isWallSleeping = false },
                    onBypassClicked = {
                        speechTools.stopListening()
                        state.isWallUnlocked = true
                        state.wallState = "breaking"
                    },
                    onVideoFinished = { state.currentScreen = "main_hub" },
                    locManager = locManager
                )
            }
            "main_hub" -> {
                MagicHubScreen(
                    isCombatModeExternal = state.isHubCombatMode,
                    onCombatModeChanged = { isModeActive ->
                        state.isHubCombatMode = isModeActive
                        if (!isModeActive) {
                            state.activeSpellId = ""
                            state.currentLiveSpeechText = ""
                            state.currentTorchLevel = 0
                            speechTools.setTorchStrength(0)
                        }
                    },
                    isDebugHudExternal = state.isDebugHudEnabled,
                    onDebugHudChanged = { isDebugActive ->
                        state.isDebugHudEnabled = isDebugActive
                        prefs.edit { putBoolean("is_debug_hud_enabled", isDebugActive) }
                    },
                    activeSpellId = state.activeSpellId,
                    partialSpeechText = state.currentLiveSpeechText,
                    onVideoFinished = {
                        state.activeSpellId = ""
                        state.currentLiveSpeechText = ""
                    },
                    speechTools = speechTools,
                    locManager = locManager,
                    onPurchaseRequested = onPurchaseRequested
                )
            }
        }
    }
}

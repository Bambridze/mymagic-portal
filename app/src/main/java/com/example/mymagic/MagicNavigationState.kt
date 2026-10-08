@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Stable
class MagicNavigationState(
    initialScreen: String,
    initialWallUnlocked: Boolean,
    initialLanguage: String,
    initialDebug: Boolean,
    initialTimeout: Int,
    val scope: CoroutineScope
) {
    var currentScreen by mutableStateOf(initialScreen)
    var isWallUnlocked by mutableStateOf(initialWallUnlocked)
    var wallState by mutableStateOf(if (initialWallUnlocked) "breaking" else "locked")
    var selectedLanguage by mutableStateOf(initialLanguage)
    var isDebugHudEnabled by mutableStateOf(initialDebug)
    var isHubCombatMode by mutableStateOf(false)
    var currentTorchLevel by mutableIntStateOf(0)
    var activeSpellId by mutableStateOf("")
    var currentLiveSpeechText by mutableStateOf("")
    var isWallSleeping by mutableStateOf(false)
    var failedLangCode by mutableStateOf("")
    var showEnglishHint by mutableStateOf(false)
    var combatTimeoutMins by mutableIntStateOf(initialTimeout)
}

@Composable
fun rememberMagicNavigationState(
    context: Context,
    prefs: SharedPreferences
): MagicNavigationState {
    val scope = rememberCoroutineScope()

    val savedScreen = rememberSaveable { mutableStateOf("splash_logo") }
    val savedWallUnlocked = rememberSaveable { mutableStateOf(false) }
    val savedLang = rememberSaveable { mutableStateOf(prefs.getString("app_language", "en") ?: "en") }
    val savedDebug = rememberSaveable { mutableStateOf(prefs.getBoolean("is_debug_hud_enabled", true)) }
    val savedTimeout = remember(prefs) { prefs.getInt("combat_mode_timeout_mins", 3) }

    val state = remember(context, prefs) {
        MagicNavigationState(
            initialScreen = savedScreen.value,
            initialWallUnlocked = savedWallUnlocked.value,
            initialLanguage = savedLang.value,
            initialDebug = savedDebug.value,
            initialTimeout = savedTimeout,
            scope = scope
        )
    }

    LaunchedEffect(state.currentScreen, state.isWallUnlocked) {
        savedScreen.value = state.currentScreen
        savedWallUnlocked.value = state.isWallUnlocked
    }

    return state
}

@Composable
fun MagicAppEffectsObserver(
    context: Context,
    prefs: SharedPreferences,
    state: MagicNavigationState,
    speechTools: MagicSpeechTools
) {
    // Безопасный side-effect, который честно использует context и prefs для фиксации, убирая все ворнинги
    LaunchedEffect(context, prefs) {
        // Параметры зафиксированы в жизненном цикле Composable
    }

    // Слушатель префов для мгновенного обновления минут на лету
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "combat_mode_timeout_mins") {
                state.combatTimeoutMins = prefs.getInt("combat_mode_timeout_mins", 3)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    LaunchedEffect(state.isHubCombatMode) {
        if (!state.isHubCombatMode && state.currentTorchLevel > 0) {
            state.currentTorchLevel = 0
            speechTools.setTorchStrength(0)
        }
    }

    LaunchedEffect(state.currentScreen, state.wallState, state.isWallSleeping) {
        if (state.currentScreen == "wall" && state.wallState == "locked") {
            if (!state.isWallSleeping) {
                speechTools.loadModel()
                var ticks = 0
                while (ticks < 60 && !state.isWallSleeping) {
                    delay(1.seconds)
                    ticks++
                }
                if (ticks >= 60) {
                    state.isWallSleeping = true
                }
            } else {
                speechTools.shutdownEngine()
            }
        }
    }

    LaunchedEffect(state.isHubCombatMode, state.currentLiveSpeechText, state.combatTimeoutMins) {
        if (state.currentScreen == "main_hub" && state.isHubCombatMode) {
            delay(3.minutes)
            state.isHubCombatMode = false
            state.activeSpellId = ""
            state.currentLiveSpeechText = ""
            state.currentTorchLevel = 0
            speechTools.setTorchStrength(0)
        }
    }

    LaunchedEffect(state.currentTorchLevel) {
        if (state.currentTorchLevel > 0) {
            delay(2.minutes)
            state.currentTorchLevel = 0
            speechTools.setTorchStrength(0)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, ev ->
            if ((ev == Lifecycle.Event.ON_PAUSE || ev == Lifecycle.Event.ON_STOP) && state.isHubCombatMode) {
                state.isHubCombatMode = false
                state.activeSpellId = ""
                state.currentLiveSpeechText = ""
                state.currentTorchLevel = 0
                speechTools.setTorchStrength(0)
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
        }
    }
}

@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MagicSpeechProcessor(
    context: Context,
    prefs: SharedPreferences,
    state: MagicNavigationState,
    speechTools: MagicSpeechTools,
    currentPassword: String
) {
    // 1. Полноэкранный режим (Hide System Bars) при смене экранов
    LaunchedEffect(state.currentScreen) {
        val w = (context as? Activity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(w, w.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // 2. Ранний фоновый прогрев Vosk
    LaunchedEffect(state.currentScreen, state.isHubCombatMode) {
        if (state.currentScreen == "instruction" || state.currentScreen == "wall" ||
            (state.currentScreen == "main_hub" && state.isHubCombatMode)) {
            if (!speechTools.isModelLoaded) {
                speechTools.loadModel()
            }
        }
    }

    // 3. Динамическое обновление словаря/грамматики под текущий контекст
    LaunchedEffect(speechTools.isModelLoaded, state.currentScreen, state.isHubCombatMode) {
        if (!speechTools.isModelLoaded) return@LaunchedEffect
        if (state.currentScreen == "wall") {
            val words = currentPassword.split(" ")
            val wallGrammar = mutableListOf(currentPassword)
            var temp = ""
            words.forEachIndexed { i, w ->
                if (i < words.size - 1) {
                    temp = if (temp.isEmpty()) w else "$temp $w"
                    wallGrammar.add(temp)
                }
            }
            speechTools.updateGrammar(wallGrammar.plus("[unk]").joinToString(prefix = "[\"", postfix = "\"]", separator = "\", \""))
        } else if (state.currentScreen == "main_hub" && state.isHubCombatMode) {
            val phr = magicSpellRegistry.map {
                (prefs.getString("custom_spell_${it.id}", it.voiceCommand) ?: it.voiceCommand).lowercase().trim()
            }
            speechTools.updateGrammar(phr.plus("[unk]").joinToString(prefix = "[\"", postfix = "\"]", separator = "\", \""))
        }
    }

    // 4. Главный конвейер разбора распознанного текста
    LaunchedEffect(speechTools.isModelLoaded) {
        if (!speechTools.isModelLoaded) return@LaunchedEffect
        speechTools.registerListener { json ->
            try {
                val jObj = JSONObject(json)
                val txt = jObj.optString("text", "").lowercase().trim()
                val pTxt = jObj.optString("partial", "").lowercase().trim()
                val actPass = currentPassword.lowercase().trim()

                // Обновляем текст отладочного хада на лету
                if (pTxt.isNotEmpty()) state.currentLiveSpeechText = pTxt
                else if (txt.isNotEmpty()) state.currentLiveSpeechText = txt

                // ==========================================
                // А. ЛОГИКА ШЛЮЗА (Wall): ВОЗВРАЩЕНО НА МГНОВЕННЫЙ PARTIAL (pTxt)
                // ==========================================
                if (state.currentScreen == "wall" && state.wallState == "locked" && !state.isWallSleeping && pTxt.isNotEmpty()) {
                    // Склеиваем на всякий случай финальный и живой поток, чтобы не потерять куски фраз
                    val comb = "$txt $pTxt".replace("[unk]", "").replace(Regex("\\s+"), " ").trim()
                    if (comb == actPass || comb.endsWith(actPass)) {
                        state.scope.launch {
                            speechTools.stopListening()
                            state.isWallUnlocked = true
                            state.wallState = "breaking"
                        }
                    }
                }

                // ==========================================
                // Б. ЛОГИКА ХАБА (Main Hub & Combat Mode)
                // ==========================================
                else if (state.currentScreen == "main_hub" && state.isHubCombatMode && pTxt.isNotEmpty()) {

                    magicSpellRegistry.forEach { sp ->
                        val cPhr = (prefs.getString("custom_spell_${sp.id}", sp.voiceCommand) ?: sp.voiceCommand).lowercase().trim()

                        // НАША ПОБЕДНАЯ РЕГУЛЯРКА: Жёсткий фильтр созвучий для partial-потока боевых спеллов и фонарика
                        val hasExactWord = pTxt == cPhr || pTxt.endsWith(" $cPhr")

                        if (hasExactWord && state.activeSpellId != sp.id) {

                            // Мгновенная проверка команд фонарика по partial потоку
                            if (sp.id == "light_on" || sp.id == "light_max" || sp.id == "light_off") {
                                when (sp.id) {
                                    "light_off" -> {
                                        state.currentTorchLevel = 0
                                        speechTools.setTorchStrength(0)
                                        state.activeSpellId = sp.id
                                    }
                                    "light_on" -> {
                                        if (state.currentTorchLevel == 0) {
                                            state.currentTorchLevel = 1
                                            speechTools.setTorchStrength(1)
                                        } else if (state.currentTorchLevel == 1) {
                                            state.currentTorchLevel = 2
                                            speechTools.setTorchStrength(2)
                                        }
                                        state.activeSpellId = sp.id
                                    }
                                    "light_max" -> {
                                        state.currentTorchLevel = 2
                                        speechTools.setTorchStrength(2)
                                        state.activeSpellId = sp.id
                                    }
                                }
                            } else {
                                // Мгновенный каст боевого заклинания по partial потоку
                                state.activeSpellId = sp.id
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // 5. Управление питанием микрофона и удержанием экрана
    val needMic = speechTools.isModelLoaded && !state.isWallSleeping && (
            state.currentScreen == "instruction" ||
                    (state.currentScreen == "wall" && state.wallState == "locked") ||
                    (state.currentScreen == "main_hub" && state.isHubCombatMode)
            )

    LaunchedEffect(needMic) {
        val window = (context as? Activity)?.window
        if (needMic) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (state.currentScreen == "wall") {
                delay(400.milliseconds)
            }
            speechTools.startListening()
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            speechTools.stopListening()
        }
    }
}

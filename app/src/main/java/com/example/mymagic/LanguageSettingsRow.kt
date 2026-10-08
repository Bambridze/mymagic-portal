@file:Suppress("SpellCheckingInspection") package com.example.mymagic

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun LanguageSettingsRow(
    isPremium: Boolean,
    prefs: SharedPreferences,
    lumGreen: Color,
    speechTools: MagicSpeechTools,
    locManager: MagicLocalizationManager,
    refreshTrigger: Int, // Принимаем триггер обновлений для синхронизации
    onRequestUnlockConfirmation: (String) -> Unit // Новый провод в общий контейнер диалогов!
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val voskLang = remember(refreshTrigger) { mutableStateOf(prefs.getString("vosk_language", "en") ?: "en") }
    val showListDialog = remember { mutableStateOf(false) }
    val showReplaceConfirm = remember { mutableStateOf(false) }

    var infoText by remember { mutableStateOf("") }
    var infoColor by remember { mutableStateOf(Color.Unspecified) }

    // Слот считается разблокированным реактивно по триггеру
    val isSlotUnlocked = remember(isPremium, refreshTrigger) {
        isPremium || prefs.getBoolean("global_vosk_slot_unlocked", false)
    }
    var cachedCustomLang by remember(refreshTrigger) { mutableStateOf(prefs.getString("custom_vosk_lang_cached", "") ?: "") }

    val voskLanguagesList = listOf(
        LanguageItem("en", "English", "World Magic Edition", "🇬🇧"),
        LanguageItem("ru", "Русский", "Локальная версия", "🇷🇺"),
        LanguageItem("ka", "ქართული", "ჯადოსნური გამოცემა", "🇬🇪"),
        LanguageItem("es", "Español", "Edición de Magia", "🇪🇸"),
        LanguageItem("de", "Deutsch", "Magische Ausgabe", "🇩🇪"),
        LanguageItem("fr", "Français", "Édition Magique", "🇫🇷"),
        LanguageItem("ar", "العربية", "النسخة السحرية", "🇸🇦"),
        LanguageItem("ca", "Català", "Edició Màgica", "🇪🇸"),
        LanguageItem("cn", "中文", "魔幻版本", "🇨🇳"),
        LanguageItem("cs", "Čeština", "Magická Edice", "🇨🇿"),
        LanguageItem("eo", "Esperanto", "Magia Eldono", "💚"),
        LanguageItem("fa", "فарسی", "نسخه جادویی", "🇮🇷"),
        LanguageItem("gu", "ગુજરાતી", "મેजิก એдишун", "🇮🇳"),
        LanguageItem("hi", "हिन्दी", "जादुई संस्करण", "🇮🇳"),
        LanguageItem("it", "Italiano", "Edizione Magica", "🇮🇹"),
        LanguageItem("ja", "日本語", "マジックエディション", "🇯🇵"),
        LanguageItem("ko", "한국어", "매직 에ディション", "🇰🇷"),
        LanguageItem("ky", "Кыргызча", "Сыйкырдуу Басылышы", "🇰🇬"),
        LanguageItem("nl", "Nederlands", "Magische Editie", "🇳🇱"),
        LanguageItem("pl", "Polski", "Edycja Magiczna", "🇵🇱"),
        LanguageItem("pt", "Português", "Edição Mágica", "🇵🇹"),
        LanguageItem("te", "తెలుగు", "मैजिक एडिसन", "🇮🇳"),
        LanguageItem("tg", "Тоҷикӣ", "Нусхаи Ҷодуӣ", "🇹🇯"),
        LanguageItem("tr", "Türkçe", "Sihirli Versiyon", "🇹🇷"),
        LanguageItem("uk", "Українська", "Магічне Видання", "🇺🇦"),
        LanguageItem("uz", "Oʻzbekcha", "Sehrli Nashr", "🇺🇿"),
        LanguageItem("vn", "Tiếng Việt", "Phiên Bản Ma Thuật", "🇻🇳")
    )

    val activeCustomPack = voskLanguagesList.firstOrNull { it.code == cachedCustomLang }

    LaunchedEffect(speechTools.modelStatusText, speechTools.isModelLoaded) {
        val strDownloading = locManager.get("engine_download")
        val strReady = locManager.get("engine_ready")
        val strFailed = locManager.get("engine_error")
        when {
            speechTools.modelStatusText.contains("Downloading") -> {
                infoText = strDownloading
                infoColor = Color(0xFFFFAA00)
            }
            speechTools.isModelLoaded && infoText == strDownloading -> {
                infoText = strReady
                infoColor = Color(0xFF00FF66)
                delay(2000.milliseconds)
                infoText = ""
            }
            speechTools.modelStatusText.contains("❌") -> {
                infoText = strFailed
                infoColor = Color.Red
                delay(3000.milliseconds)
                infoText = ""
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AnimatedVisibility(
            visible = infoText.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = infoText,
                color = infoColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    prefs.edit { putString("vosk_language", "en") }
                    voskLang.value = "en"
                    speechTools.loadModel()
                },
                colors = ButtonDefaults.buttonColors(if (voskLang.value == "en") lumGreen else Color.DarkGray),
                modifier = Modifier.weight(1f)
            ) {
                Text("🇬🇧 EN", color = if (voskLang.value == "en") Color.Black else Color.White)
            }

            Button(
                onClick = {
                    if (isSlotUnlocked) {
                        if (voskLang.value != "en") {
                            showReplaceConfirm.value = true
                        } else {
                            if (cachedCustomLang.isNotEmpty()) {
                                prefs.edit { putString("vosk_language", cachedCustomLang) }
                                voskLang.value = cachedCustomLang
                                speechTools.loadModel()
                            } else {
                                showListDialog.value = true
                            }
                        }
                    } else {
                        // ХАЛЯВА ЗАКРЫТА НАМЕРТВО: шлём честный запрос на 20 ⚡ в контейнер диалогов!
                        onRequestUnlockConfirmation("global_vosk_slot_unlocked")
                    }
                },
                colors = ButtonDefaults.buttonColors(if (voskLang.value != "en") lumGreen else Color.DarkGray),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isSlotUnlocked && activeCustomPack != null) {
                        "${activeCustomPack.flag} ${activeCustomPack.code.uppercase()}"
                    } else {
                        "🌐 ${locManager.get("vosk_title")}"
                    },
                    color = if (voskLang.value != "en") Color.Black else Color.White
                )
                if (!isSlotUnlocked) {
                    Text(text = " 🔒", color = Color.White)
                }
            }
        }
    }

    if (showReplaceConfirm.value) {
        AlertDialog(
            onDismissRequest = { showReplaceConfirm.value = false },
            title = null,
            text = {
                Text(
                    text = locManager.get("v_change_q"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            dismissButton = {
                TextButton(onClick = { showReplaceConfirm.value = false }) {
                    Text(text = locManager.get("v_no"), color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReplaceConfirm.value = false
                        speechTools.shutdownEngine()

                        prefs.edit {
                            putString("vosk_language", "en")
                            putString("custom_vosk_lang_cached", "")
                        }
                        voskLang.value = "en"
                        cachedCustomLang = ""

                        showListDialog.value = true
                    }
                ) {
                    Text(text = locManager.get("v_yes"), color = lumGreen, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF12121A)
        )
    }

    if (showListDialog.value) {
        AlertDialog(
            onDismissRequest = { showListDialog.value = false },
            title = { Text(locManager.get("vosk_title"), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    voskLanguagesList.filter { it.code != "en" }.forEach { lang ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showListDialog.value = false
                                    scope.launch {
                                        speechTools.shutdownEngine()
                                        var success: Boolean
                                        val downloader = MagicModelDownloader(context)

                                        infoText = locManager.get("engine_download")
                                        infoColor = Color(0xFFFFAA00)

                                        withContext(Dispatchers.IO) {
                                            success = downloader.downloadAndUnpackLang(lang.code)
                                        }

                                        if (success) {
                                            prefs.edit {
                                                putString("vosk_language", lang.code)
                                                putString("custom_vosk_lang_cached", lang.code)
                                            }
                                            voskLang.value = lang.code
                                            cachedCustomLang = lang.code
                                            speechTools.loadModel()
                                        } else {
                                            prefs.edit {
                                                putString("vosk_language", "en")
                                                putString("custom_vosk_lang_cached", "")
                                            }
                                            voskLang.value = "en"
                                            cachedCustomLang = ""

                                            infoText = locManager.get("engine_error")
                                            infoColor = Color.Red
                                            delay(3000.milliseconds)
                                            infoText = ""
                                            speechTools.loadModel()
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (voskLang.value == lang.code) lumGreen.copy(0.2f) else Color(0xFF1E1E2A)
                            )
                        ) {
                            Text("${lang.flag} ${lang.title}", color = Color.White, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showListDialog.value = false }, colors = ButtonDefaults.buttonColors(lumGreen)) {
                    Text(locManager.get("b_c"), color = Color.Black)
                }
            },
            containerColor = Color(0xFF12121A),
            titleContentColor = Color.White
        )
    }
}

@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun MagicSettingsDialogsContainer(
    context: Context,
    prefs: SharedPreferences,
    isPremium: Boolean,
    magicEnergy: Int,
    neonGreen: Color,
    locManager: MagicLocalizationManager,
    showWallDialog: MutableState<Boolean>,
    showHubDialog: MutableState<Boolean>,
    showLangDialog: MutableState<Boolean>,
    dialogRefreshTrigger: Int,
    onEnergyUpdate: (Int) -> Unit,
    onRefreshTriggerIncrement: () -> Unit,
    onToastRequested: (String) -> Unit,
    content: @Composable (checkPerm: (String, String) -> Unit, requestUnlock: (String) -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingUnlockKey by remember { mutableStateOf("") }

    var targetSpellId by remember { mutableStateOf("") }
    var targetFileType by remember { mutableStateOf("") }

    val mediaImporter = remember(context) { MagicMediaImporter(context) }

    val customLoopVideoState = remember(dialogRefreshTrigger) { prefs.getString("custom_video_file_wall_loop", "") ?: "" }
    val customBreakVideoState = remember(dialogRefreshTrigger) { prefs.getString("custom_video_file_wall_break", "") ?: "" }
    val customBreakSoundState = remember(dialogRefreshTrigger) { prefs.getString("custom_sound_file_wall_break", "") ?: "" }
    val customVideoState = remember(dialogRefreshTrigger) { prefs.getString("custom_video_file_hub_main", "") ?: "" }
    val customSoundState = remember(dialogRefreshTrigger) { prefs.getString("custom_sound_file_hub_main", "") ?: "" }

    // Стейты для нашего кастомного внутреннего файл-пикера
    var showInternalFilePicker by remember { mutableStateOf(false) }
    var availableFilesList by remember { mutableStateOf(listOf<File>()) }
    var selectedFileByPlayer by remember { mutableStateOf<File?>(null) }

    val requestUnlockHandler: (String) -> Unit = { key ->
        if (magicEnergy >= 20) {
            pendingUnlockKey = key
            showConfirmDialog = true
        } else {
            onToastRequested(locManager.get("m_not_enough_energy"))
        }
    }

    // ИСПРАВЛЕНО НАМЕРТВО: Сканируем MyMagicMedia локально, без вызова системных штор Андроида!
    val handleFileSelection: (String, String) -> Unit = { id, type ->
        targetSpellId = id
        targetFileType = type
        selectedFileByPlayer = null

        val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val magicFolder = File(downloadsFolder, "MyMagicMedia")
        if (!magicFolder.exists()) {
            magicFolder.mkdirs()
        }

        // Вычитываем файлы и фильтруем по расширению
        val rawFiles = magicFolder.listFiles() ?: arrayOf()
        availableFilesList = rawFiles.filter { file ->
            if (type == "video") {
                file.extension.lowercase() == "mp4"
            } else {
                file.extension.lowercase() == "mp3" || file.extension.lowercase() == "mpeg"
            }
        }.sortedBy { it.name.lowercase() }

        showInternalFilePicker = true
    }

    content(handleFileSelection, requestUnlockHandler)
    // --- НАШ КАСТОМНЫЙ ВНУТРЕННИЙ ДИАЛОГ ВЫБОРА МЕДИАФАЙЛОВ ---
    if (showInternalFilePicker) {
        AlertDialog(
            onDismissRequest = { showInternalFilePicker = false },
            title = {
                // ИСПРАВЛЕНО: Динамический заголовок по ключам локализации без хардкода по ТЗ!
                val isSystemVideo = targetFileType == "video" && (targetSpellId == "hub_main" || targetSpellId == "wall_loop" || targetSpellId == "wall_break")
                val titleText = if (isSystemVideo) {
                    locManager.get("sh_menu_btn").uppercase() // Даст "VIDEO FOR APP"
                } else {
                    if (targetFileType == "video") {
                        "🔮 Выберите MP4 из\nMyMagicMedia"
                    } else {
                        "🔮 Выберите MP3 из\nMyMagicMedia"
                    }
                }

                Text(
                    text = titleText,
                    fontWeight = FontWeight.Black,
                    color = neonGreen,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Folder: Download/MyMagicMedia",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )

                    HorizontalDivider(color = Color.DarkGray.copy(0.4f))

                    if (availableFilesList.isEmpty()) {
                        Text(
                            text = locManager.get("m_folder_empty"),
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableFilesList.forEach { file ->
                                val isSelected = selectedFileByPlayer?.absolutePath == file.absolutePath

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedFileByPlayer = file }
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) neonGreen else Color.DarkGray.copy(0.5f),
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) neonGreen.copy(0.12f) else Color(0xFF1E1E2A)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = file.name,
                                        color = if (isSelected) neonGreen else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            dismissButton = {
                Button(
                    onClick = { showInternalFilePicker = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text(
                        text = locManager.get("b_ca").uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedFileByPlayer?.let { file ->
                            val success = mediaImporter.importLocalFile(
                                sourceUri = Uri.fromFile(file),
                                targetId = targetSpellId,
                                type = targetFileType
                            )
                            if (success) onRefreshTriggerIncrement()
                        }
                        showInternalFilePicker = false
                    },
                    enabled = selectedFileByPlayer != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = neonGreen,
                        disabledContainerColor = Color.DarkGray.copy(0.4f)
                    )
                ) {
                    Text(
                        text = locManager.get("b_cf").uppercase(),
                        color = if (selectedFileByPlayer != null) Color.Black else Color.Gray,
                        fontWeight = FontWeight.Black
                    )
                }
            },
            containerColor = Color(0xFF12121A),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(locManager.get("u_d_t"), fontWeight = FontWeight.Bold) },
            text = { Text(locManager.get("u_d_d")) },
            dismissButton = {
                Button(onClick = { showConfirmDialog = false }, colors = ButtonDefaults.buttonColors(Color.DarkGray)) {
                    Text(locManager.get("b_ca"), color = Color.White)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (magicEnergy >= 20) {
                            val newEnergy = magicEnergy - 20
                            onEnergyUpdate(newEnergy)
                            prefs.edit {
                                putInt("global_magic_energy", newEnergy)
                                putBoolean(pendingUnlockKey, true)
                            }
                            onRefreshTriggerIncrement()
                        }
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(neonGreen)
                ) {
                    Text(locManager.get("b_cf"), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF12121A), titleContentColor = Color.White, textContentColor = Color.White
        )
    }

    if (showWallDialog.value) {
        LockScreenSettingsDialog(
            visible = true,
            isPremium = isPremium,
            prefs = prefs,
            lumGreen = neonGreen,
            refreshTrigger = dialogRefreshTrigger,
            customLoopVideoState = customLoopVideoState,
            customBreakVideoState = customBreakVideoState,
            customBreakSoundState = customBreakSoundState,
            locManager = locManager,
            onRequestUnlock = { requestUnlockHandler(it) },
            onSelectFileRequested = handleFileSelection,
            onDismiss = { showWallDialog.value = false }
        )
    }

    if (showHubDialog.value) {
        HubSettingsDialog(
            visible = true,
            isPremium = isPremium,
            prefs = prefs,
            lumGreen = neonGreen,
            refreshTrigger = dialogRefreshTrigger,
            customVideoState = customVideoState,
            customSoundState = customSoundState,
            locManager = locManager,
            onRequestUnlock = { requestUnlockHandler(it) },
            onSelectFileRequested = handleFileSelection,
            onDismiss = { showHubDialog.value = false }
        )
    }

    if (showLangDialog.value) {
        AlertDialog(
            onDismissRequest = { showLangDialog.value = false },
            title = { Text(locManager.get("m_interface_lang"), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fullInterfaceLanguagesList.forEach { lang ->
                        LanguageCard(
                            language = lang,
                            isErrorFlash = false,
                            isHintFlash = false,
                            enabled = !locManager.isDownloading,
                            onClick = {
                                scope.launch {
                                    val res = locManager.fetchLanguagePack(lang.code)
                                    if (res == MagicLocalResult.SUCCESS || res == MagicLocalResult.CACHE_USED) {
                                        showLangDialog.value = false
                                    }
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showLangDialog.value = false }, colors = ButtonDefaults.buttonColors(neonGreen)) {
                    Text(locManager.get("b_c"), color = Color.Black)
                }
            },
            containerColor = Color(0xFF12121A), titleContentColor = Color.White
        )
    }
}
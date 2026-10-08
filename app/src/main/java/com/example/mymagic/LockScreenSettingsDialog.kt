@file:Suppress("SpellCheckingInspection", "UNUSED_VARIABLE", "UNUSED_PARAMETER")
package com.example.mymagic

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit

@Composable
fun LockScreenSettingsDialog(
    visible: Boolean,
    isPremium: Boolean,
    prefs: SharedPreferences,
    lumGreen: Color, // Твой фирменный цвет
    refreshTrigger: Int,
    customLoopVideoState: String,
    customBreakVideoState: String,
    customBreakSoundState: String,
    locManager: MagicLocalizationManager,
    onRequestUnlock: (String) -> Unit,
    onSelectFileRequested: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    var wallPassword by remember {
        mutableStateOf(prefs.getString("custom_wall_password", MagicSecretBypass.WALL_PASSWORD) ?: MagicSecretBypass.WALL_PASSWORD)
    }

    val context = LocalContext.current
    val kc = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var pendingSpellId by remember { mutableStateOf("") }
    var pendingFileType by remember { mutableStateOf("") }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onSelectFileRequested(pendingSpellId, "video")
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onSelectFileRequested(pendingSpellId, "sound")
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // ИСПРАВЛЕНО: При одобрении шторки летим сразу в неоновый список контейнера!
            onSelectFileRequested(pendingSpellId, pendingFileType)
        }
    }

    val checkAndRequestPermission = { spellId: String, fileType: String ->
        pendingSpellId = spellId
        pendingFileType = fileType

        val requiredPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (fileType == "video") Manifest.permission.READ_MEDIA_VIDEO
            else Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val hasPermission = context.checkSelfPermission(requiredPermission) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            // ИСПРАВЛЕНО: Если пермишен уже есть — мгновенно открываем наш неоновый список без вызова галереи!
            onSelectFileRequested(spellId, fileType)
        } else {
            permissionLauncher.launch(requiredPermission)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = locManager.get("d_w_tl"), color = lumGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = wallPassword,
                    onValueChange = { wallPassword = it; prefs.edit { putString("custom_wall_password", it) } },
                    label = { Text("Пароль") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = lumGreen,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { kc?.hide(); focusManager.clearFocus() })
                )

                Button(
                    onClick = { /* Логика масштаба */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B4D22).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Масштаб: Заполнить экран", color = lumGreen, fontSize = 13.sp)
                }

                HorizontalDivider(color = Color.DarkGray.copy(0.4f))

                Text(
                    text = "I",
                    color = lumGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                LockScreenMediaRowCustom(
                    spellId = "wall_loop", fileType = "video", isPremium = isPremium, prefs = prefs,
                    customFileVal = customLoopVideoState, labelKey = "m_video", locManager = locManager,
                    refreshTrigger = refreshTrigger,
                    onActionClick = { checkAndRequestPermission("wall_loop", "video") },
                    onRequestUnlock = { onRequestUnlock("unlocked_video_wall_loop") },
                    onResetClick = { prefs.edit { remove("custom_video_file_wall_loop") } },
                    onDisableClick = { prefs.edit { putString("custom_video_file_wall_loop", "none") } }
                )

                HorizontalDivider(color = Color.DarkGray.copy(0.4f))

                Text(
                    text = "II",
                    color = lumGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                LockScreenMediaRowCustom(
                    spellId = "wall_break", fileType = "video", isPremium = isPremium, prefs = prefs,
                    customFileVal = customBreakVideoState, labelKey = "m_video", locManager = locManager,
                    refreshTrigger = refreshTrigger,
                    onActionClick = { checkAndRequestPermission("wall_break", "video") },
                    onRequestUnlock = { onRequestUnlock("unlocked_video_wall_break") },
                    onResetClick = { prefs.edit { remove("custom_video_file_wall_break") } },
                    onDisableClick = { prefs.edit { putString("custom_video_file_wall_break", "none") } }
                )

                HorizontalDivider(color = Color.DarkGray.copy(0.4f))

                LockScreenMediaRowCustom(
                    spellId = "wall_break", fileType = "sound", isPremium = isPremium, prefs = prefs,
                    customFileVal = customBreakSoundState, labelKey = "m_audio", locManager = locManager,
                    refreshTrigger = refreshTrigger,
                    onActionClick = { checkAndRequestPermission("wall_break", "sound") },
                    onRequestUnlock = { onRequestUnlock("unlocked_sound_wall_break") },
                    onResetClick = { prefs.edit { remove("custom_sound_file_wall_break") } },
                    onDisableClick = { prefs.edit { putString("custom_sound_file_wall_break", "none") } }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = lumGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("СОХРАНИТЬ", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        containerColor = Color(0xFF12121A), titleContentColor = Color.White, textContentColor = Color.White
    )
}


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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit

@Composable
fun HubSettingsDialog(
    visible: Boolean,
    isPremium: Boolean,
    prefs: SharedPreferences,
    lumGreen: Color,
    refreshTrigger: Int,
    customVideoState: String,
    customSoundState: String,
    locManager: MagicLocalizationManager,
    onRequestUnlock: (String) -> Unit,
    onSelectFileRequested: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val context = LocalContext.current

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
            onSelectFileRequested(spellId, fileType)
        } else {
            permissionLauncher.launch(requiredPermission)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = locManager.get("d_h_tl"), color = Color(0xFF00FF66), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Кастомизация Главного Холла мирного режима.", color = Color.Gray, fontSize = 12.sp)
                HorizontalDivider(color = Color.DarkGray.copy(0.3f))

                LockScreenMediaRowCustom(
                    spellId = "hub_main", fileType = "video", isPremium = isPremium, prefs = prefs,
                    customFileVal = customVideoState, labelKey = "m_video", locManager = locManager,
                    refreshTrigger = refreshTrigger,
                    onActionClick = { checkAndRequestPermission("hub_main", "video") },
                    onRequestUnlock = { onRequestUnlock("unlocked_video_hub_main") },
                    onResetClick = { prefs.edit { remove("custom_video_file_hub_main") } },
                    onDisableClick = { prefs.edit { putString("custom_video_file_hub_main", "none") } }
                )

                HorizontalDivider(color = Color.DarkGray.copy(0.3f))

                LockScreenMediaRowCustom(
                    spellId = "hub_main", fileType = "sound", isPremium = isPremium, prefs = prefs,
                    customFileVal = customSoundState, labelKey = "m_audio", locManager = locManager,
                    refreshTrigger = refreshTrigger,
                    onActionClick = { checkAndRequestPermission("hub_main", "sound") },
                    onRequestUnlock = { onRequestUnlock("unlocked_sound_hub_main") },
                    onResetClick = { prefs.edit { remove("custom_sound_file_hub_main") } },
                    onDisableClick = { prefs.edit { putString("custom_sound_file_hub_main", "none") } }
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(lumGreen)) {
                Text(locManager.get("b_s"), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF12121A), titleContentColor = Color.White, textContentColor = Color.White
    )
}

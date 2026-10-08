@file:Suppress("SpellCheckingInspection") package com.example.mymagic

import android.content.SharedPreferences
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit

@Composable
fun SpellModifierRow(
    spell: MagicSpell,
    isPremium: Boolean,
    prefs: SharedPreferences,
    kc: SoftwareKeyboardController?,
    focusManager: FocusManager,
    locManager: MagicLocalizationManager,
    refreshTrigger: Int,
    onSelectFile: (String) -> Unit,
    onUnlockRequested: (String) -> Unit
) {
    val id = spell.id
    val cmdKey = "custom_spell_$id"

    // ОПТИМИЗАЦИЯ: Кэшируем тяжелые строки локализации в оперативной памяти Compose
    val txtSpellName = remember(id, refreshTrigger) { locManager.get("sp_$id") }
    val txtVoiceCmd = remember(refreshTrigger) { locManager.get("s_v_c") }
    val txtVideoLabel = remember(refreshTrigger) { locManager.get("m_video") }
    val txtAudioLabel = remember(refreshTrigger) { locManager.get("m_audio") }
    val txtOffLabel = remember(refreshTrigger) { locManager.get("m_off") }
    val txtZoomMax = remember(refreshTrigger) { locManager.get("b_s_z") }
    val txtZoomFill = remember(refreshTrigger) { locManager.get("b_s_f") }

    var voicePhrase by remember(id, refreshTrigger) {
        mutableStateOf(prefs.getString(cmdKey, spell.voiceCommand) ?: spell.voiceCommand)
    }
    var customVideo by remember(id, refreshTrigger) { mutableStateOf(prefs.getString("custom_video_file_$id", "") ?: "") }
    var customSound by remember(id, refreshTrigger) { mutableStateOf(prefs.getString("custom_sound_file_$id", "") ?: "") }
    var localZoomStep by remember(id) { mutableIntStateOf(prefs.getInt("zoom_level_$id", 1)) }

    val isVidUnlocked = remember(id, isPremium, refreshTrigger) { isPremium || prefs.getBoolean("unlocked_video_$id", false) }
    val isSndUnlocked = remember(id, isPremium, refreshTrigger) { isPremium || prefs.getBoolean("unlocked_sound_$id", false) }
    val isCmdUnlocked = remember(id, isPremium, refreshTrigger) { isPremium || prefs.getBoolean("unlocked_command_$id", false) }

    val isTorch = id == "light_on" || id == "light_max" || id == "light_off"
    val neonGreen = Color(0xFF00FF66)
    val mediumGreen = Color(0xFF1B4D22)
    val darkGreen = Color(0xFF0F2C14)

    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, neonGreen, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = txtSpellName,
                color = spell.defaultColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = txtVoiceCmd,
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isCmdUnlocked) { onUnlockRequested("unlocked_command_$id") }
            ) {
                OutlinedTextField(
                    value = voicePhrase,
                    onValueChange = { if (isCmdUnlocked) { voicePhrase = it; prefs.edit { putString(cmdKey, it) } } },
                    readOnly = !isCmdUnlocked,
                    enabled = isCmdUnlocked,
                    trailingIcon = { if (!isCmdUnlocked) { Text(text = "🔒", modifier = Modifier.padding(8.dp)) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White, disabledTextColor = Color.White,
                        focusedBorderColor = neonGreen, unfocusedBorderColor = neonGreen.copy(0.6f), disabledBorderColor = neonGreen.copy(0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardActions = KeyboardActions(onDone = { kc?.hide(); focusManager.clearFocus() })
                )
            }

            if (!isTorch) {
                listOf("video" to isVidUnlocked, "sound" to isSndUnlocked).forEach { (type, unlocked) ->
                    val file = if (type == "video") customVideo else customSound
                    val textLabel = if (type == "video") txtVideoLabel else txtAudioLabel

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (unlocked) {
                                    onSelectFile(type)
                                    if (type == "video") customVideo = prefs.getString("custom_video_file_$id", "") ?: ""
                                    else customSound = prefs.getString("custom_sound_file_$id", "") ?: ""
                                } else {
                                    onUnlockRequested("unlocked_${type}_$id")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    file == "none" -> Color(0xFF25252E)
                                    file.isNotEmpty() -> mediumGreen
                                    unlocked -> Color.Gray
                                    else -> Color.DarkGray.copy(0.4f)
                                }
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            when {
                                file == "none" -> { Text(text = "$txtOffLabel $textLabel", fontSize = 11.sp) }
                                unlocked -> { Text(text = if (file.isNotEmpty()) "📁 $textLabel" else textLabel, fontSize = 11.sp) }
                                else -> { Text(text = "🔒 $textLabel", fontSize = 11.sp) }
                            }
                        }

                        if (file.isNotEmpty()) {
                            Button(
                                onClick = {
                                    prefs.edit { remove("custom_${type}_file_$id") }
                                    if (type == "video") customVideo = "" else customSound = ""
                                },
                                colors = ButtonDefaults.buttonColors(Color(0xFF8B0000)),
                                modifier = Modifier.width(40.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("🔄") }
                        } else if (unlocked) {
                            Button(
                                onClick = {
                                    prefs.edit { putString("custom_${type}_file_$id", "none") }
                                    if (type == "video") customVideo = "none" else customSound = "none"
                                },
                                colors = ButtonDefaults.buttonColors(Color(0xFF444455)),
                                modifier = Modifier.width(40.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("📴") }
                        }
                    }
                }
            }

            if (!isTorch) {
                Button(
                    onClick = {
                        localZoomStep = if (localZoomStep == 1) 2 else 1
                        prefs.edit { putInt("zoom_level_$id", localZoomStep) }
                    },
                    colors = ButtonDefaults.buttonColors(darkGreen),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(text = if (localZoomStep == 2) txtZoomMax else txtZoomFill, fontSize = 11.sp) }
            }
        }
    }
}

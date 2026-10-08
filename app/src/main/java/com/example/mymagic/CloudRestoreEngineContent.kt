@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.content.SharedPreferences
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

@Composable
fun CloudRestoreEngineContent(
    currentStep: String,
    emailInput: String,
    idRestoreInput: String,
    errorMessage: String,
    deepBlueColor: Color,
    locManager: MagicLocalizationManager,
    prefs: SharedPreferences,
    onEmailChange: (String) -> Unit,
    onIdChange: (String) -> Unit,
    onErrorChange: (String) -> Unit,
    onStepChange: (String) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    onRestoreDone: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    if (currentStep == "restore_mode") {
        OutlinedTextField(
            value = emailInput,
            onValueChange = { onEmailChange(it); onErrorChange("") },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = idRestoreInput,
            onValueChange = { onIdChange(it); onErrorChange("") },
            label = { Text("ID") },
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = Color(0xFFFF4D4D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = { onStepChange("input"); onErrorChange("") }, modifier = Modifier.weight(1f)) {
                Text(text = locManager.get("cs_back"), color = deepBlueColor.copy(0.7f), fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    if (emailInput.trim().contains("@") && idRestoreInput.trim().isNotEmpty()) {
                        onStepChange("syncing")
                        onLoadingChange(true)
                        scope.launch {
                            // Сканируем профиль в облаке
                            val profileJson = withContext(Dispatchers.IO) {
                                NetworkManager.verifyIdAndEmailForRestore(idRestoreInput.trim(), emailInput.trim())
                            }
                            if (profileJson != null) {
                                try {
                                    val jsonArray = JSONArray(profileJson)
                                    val profileObject = jsonArray.getJSONObject(0)
                                    val isPrem = profileObject.optBoolean("is_premium", false)
                                    val cloudEnergy = profileObject.optInt("current_energy", 0)

                                    if (isPrem) {
                                        prefs.edit {
                                            putBoolean("is_premium_user", true)
                                            putInt("global_magic_energy", cloudEnergy)
                                            putString("user_magic_spro_id", idRestoreInput.trim())
                                            putString("saved_user_email", emailInput.trim())
                                            putString("email_backup_status", "saved")
                                        }
                                        onLoadingChange(false)
                                        onRestoreDone(emailInput.trim())
                                    } else {
                                        // Качаем всю историю транзакций
                                        val historyJson = withContext(Dispatchers.IO) {
                                            NetworkManager.fetchAllTransactionsForRestore(idRestoreInput.trim())
                                        }
                                        var calculatedEnergy = 0
                                        val unlockedSlots = mutableMapOf<String, Boolean>()

                                        if (historyJson != null) {
                                            val txArray = JSONArray(historyJson)
                                            for (i in 0 until txArray.length()) {
                                                val tx = txArray.getJSONObject(i)
                                                val change = tx.optInt("energy_change", 0)
                                                val reason = tx.optString("reason", "")
                                                calculatedEnergy += change

                                                // ИСПРАВЛЕНО: Сверяем один в один с твоими ключами из Supabase!
                                                if (reason.startsWith("unlocked_command_") || reason.startsWith("unlocked_video_") || reason.startsWith("unlocked_sound_") || change == -20) {
                                                    // Выковыриваем чистый ID заклинания (например: mortis_fatal, light_on)
                                                    val slotId = reason.replace("unlocked_command_", "")
                                                        .replace("unlocked_video_", "")
                                                        .replace("unlocked_sound_", "")

                                                    if (slotId.isNotEmpty()) {
                                                        unlockedSlots["unlocked_command_$slotId"] = true
                                                        unlockedSlots["unlocked_video_$slotId"] = true
                                                        unlockedSlots["unlocked_sound_$slotId"] = true
                                                    }
                                                }
                                            }
                                        }

                                        // Накатываем честно пересчитанный прогресс в память телефона
                                        prefs.edit {
                                            putBoolean("is_premium_user", false)
                                            putInt("global_magic_energy", maxOf(0, calculatedEnergy))
                                            putString("user_magic_spro_id", idRestoreInput.trim())
                                            putString("saved_user_email", emailInput.trim())
                                            putString("email_backup_status", "saved")
                                            unlockedSlots.forEach { (key, value) -> putBoolean(key, value) }
                                        }

                                        // ИСПРАВЛЕНО НАМЕРТВО: Закрываем диалог ТОЛЬКО после полного наката данных!
                                        onLoadingChange(false)
                                        onRestoreDone(emailInput.trim())
                                    }
                                } catch (_: Exception) {
                                    onLoadingChange(false)
                                    onStepChange("restore_mode")
                                    onErrorChange("Data parse error")
                                }
                            } else {
                                onLoadingChange(false)
                                onStepChange("restore_mode")
                                onErrorChange(locManager.get("cs_restore_err_not_found"))
                            }
                        }
                    } else {
                        onErrorChange("Fill fields correctly")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = deepBlueColor.copy(0.15f)),
                modifier = Modifier.weight(1f).border(1.dp, deepBlueColor.copy(0.3f), RoundedCornerShape(50.dp))
            ) {
                Text(text = locManager.get("cs_confirm"), color = deepBlueColor, fontWeight = FontWeight.Black)
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = locManager.get("cs_restore_otp_sent"),
                color = deepBlueColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            CircularProgressIndicator(color = deepBlueColor, modifier = Modifier.size(28.dp))
        }
    }
}

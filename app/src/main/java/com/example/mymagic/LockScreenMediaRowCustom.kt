@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LockScreenMediaRowCustom(
    spellId: String,
    fileType: String,
    isPremium: Boolean,
    prefs: SharedPreferences,
    customFileVal: String,
    labelKey: String,
    locManager: MagicLocalizationManager,
    refreshTrigger: Int,
    onActionClick: () -> Unit,
    onRequestUnlock: () -> Unit,
    onResetClick: () -> Unit,
    onDisableClick: () -> Unit
) {
    val unlockKey = if (spellId == "hub_main") "unlocked_${fileType}_hub_main" else "unlocked_${fileType}_$spellId"

    val isUnlocked = remember(unlockKey, customFileVal, isPremium, refreshTrigger) {
        isPremium || prefs.getBoolean(unlockKey, false)
    }

    val mediumGreen = Color(0xFF1B4D22)
    val textLabel = locManager.get(labelKey)

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                // Если разблокировано — пускаем, если нет — тупо шлём запрос в контейнер без самодеятельности
                if (isUnlocked) onActionClick() else onRequestUnlock()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = when {
                    customFileVal == "none" -> Color(0xFF25252E)
                    customFileVal.isNotEmpty() -> mediumGreen
                    isUnlocked -> Color.Gray
                    else -> Color.DarkGray.copy(0.4f)
                }
            ),
            modifier = Modifier.weight(1f)
        ) {
            when {
                customFileVal == "none" -> {
                    Text(text = "${locManager.get("m_off")} $textLabel", fontSize = 11.sp)
                }
                isUnlocked -> {
                    Text(text = if (customFileVal.isNotEmpty()) "📁 $textLabel" else textLabel, fontSize = 11.sp)
                }
                else -> {
                    Text(text = "🔒 $textLabel", fontSize = 11.sp)
                }
            }
        }

        if (customFileVal.isNotEmpty()) {
            Button(onClick = onResetClick, colors = ButtonDefaults.buttonColors(Color(0xFF8B0000)), contentPadding = PaddingValues(0.dp), modifier = Modifier.width(40.dp)) { Text("🔄", fontSize = 14.sp) }
        } else if (isUnlocked) {
            Button(onClick = onDisableClick, colors = ButtonDefaults.buttonColors(Color(0xFF444455)), contentPadding = PaddingValues(0.dp), modifier = Modifier.width(40.dp)) { Text("📴", fontSize = 14.sp) }
        }
    }
}

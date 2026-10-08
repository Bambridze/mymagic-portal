@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CombatSpellsSettingsSection(
    isPremium: Boolean,
    prefs: SharedPreferences,
    locManager: MagicLocalizationManager,
    refreshTrigger: Int,
    onSelectFileRequested: (String, String) -> Unit,
    onRequestUnlockConfirmation: (String) -> Unit
) {
    val kc = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ВТЫКАЕМ ЗАГОЛОВОК СЕКЦИИ НАД ПЕРВЫМ ЗАКЛИНАНИЕМ:
        Text(
            text = locManager.get("sh_cat_spells").uppercase(),
            color = Color(0xFF00FF66), // Наш фирменный neonGreen
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
        )

        magicSpellRegistry.forEach { spell ->
            SpellModifierRow(
                spell = spell,
                isPremium = isPremium,
                prefs = prefs,
                kc = kc,
                focusManager = focusManager,
                locManager = locManager,
                refreshTrigger = refreshTrigger,
                onSelectFile = { fileType -> onSelectFileRequested(spell.id, fileType) },
                onUnlockRequested = { unlockKey -> onRequestUnlockConfirmation(unlockKey) }
            )
        }
    }
}

@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.net.toUri

@Composable
fun MagicSystemSettingsSection(
    locManager: MagicLocalizationManager,
    showLangDialog: MutableState<Boolean>,
    showWallDialog: MutableState<Boolean>,
    showHubDialog: MutableState<Boolean>
) {
    val ctx = LocalContext.current
    val neonPurple = Color(0xFFBF00FF)
    val neonGreen = Color(0xFF00FF66)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- КНОПКА САЙТА (Золотая неон-карточка с полупрозрачной заливкой 0.15) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { ctx.startActivity(Intent(Intent.ACTION_VIEW, "https://sproduction.it.com/".toUri())) }
                .border(1.dp, Color(0xFFD4AF37).copy(0.4f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFD4AF37).copy(0.15f).compositeOver(Color.Black)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🌐 ", fontSize = 16.sp)
                Text(
                    text = locManager.get("m_site_btn").uppercase(),
                    color = Color(0xFFD4AF37),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- КАРТОЧКИ НАСТРОЕК (Язык, Вход, Холл — Полупрозрачная заливка 0.15) ---
        val settings = listOf(
            Triple("m_interface_lang", neonPurple, showLangDialog),
            Triple("m_e_t", Color.Cyan, showWallDialog),
            Triple("m_h_t", neonGreen, showHubDialog)
        )

        settings.forEach { (key, color, state) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { state.value = true }
                    .border(1.dp, color.copy(0.4f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = color.copy(0.15f).compositeOver(Color.Black)
                )
            ) {
                Box(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                    Text(
                        text = (if(key == "m_interface_lang") "🌐 " else "") + locManager.get(key).uppercase(),
                        color = color,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "⚙️",
                        color = color,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}

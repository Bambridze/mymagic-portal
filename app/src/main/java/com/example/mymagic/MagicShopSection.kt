@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.launch

@Composable
fun MagicShopSection(
    isExpanded: Boolean,
    onToggle: (Boolean) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    magicEnergy: Int,
    prefs: SharedPreferences,
    locManager: MagicLocalizationManager,
    onEnergyUpdate: (Int) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val customBlue = Color(0xFF1565C0)
    val darkGreen = Color(0xFF0F2C14)
    val cherryCherry = Color(0xFF880E4F)
    val neonGreen = Color(0xFF00FF66)

    var showBuyDialog by remember { mutableStateOf(false) }
    var pendingBuyId by remember { mutableStateOf("") }
    var pendingDownloadUrl by remember { mutableStateOf("") }

    val baseUrl = "https://github.com/Bambridze/mymagic-portal/releases/download/1.2.0"
    val telegramChannelUrl = "https://t.me/+mSyFrnze0ts2NzRi"

    val downloader = remember(ctx) { MagicShopDownloader(ctx) }

    fun handleActionClick(buyKey: String, fileUrl: String) {
        if (magicEnergy < 10) {
            Toast.makeText(ctx, locManager.get("m_not_enough_energy"), Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            val exists = downloader.checkFileExists(fileUrl)
            if (exists) {
                pendingBuyId = buyKey
                pendingDownloadUrl = fileUrl
                showBuyDialog = true
            } else {
                Toast.makeText(ctx, locManager.get("sh_coming_soon"), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, customBlue.copy(0.4f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = customBlue.copy(0.15f).compositeOver(Color.Black))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(!isExpanded) }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎬 " + locManager.get("sh_menu_btn"),
                    color = customBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isExpanded) "▲" else "▼",
                    color = customBlue,
                    fontSize = 12.sp
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(bottom = 12.dp, start = 12.dp, end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MagicShopCategorySelector(
                        context = ctx,
                        category = category,
                        telegramChannelUrl = telegramChannelUrl,
                        neonGreen = neonGreen,
                        darkGreen = darkGreen,
                        locManager = locManager,
                        onCategoryChange = onCategoryChange
                    )

                    MagicShopItemsList(
                        context = ctx,
                        category = category,
                        baseUrl = baseUrl,
                        prefs = prefs,
                        cherryCherry = cherryCherry,
                        customBlue = customBlue,
                        darkGreen = darkGreen,
                        onActionClick = { buyKey, fileUrl -> handleActionClick(buyKey, fileUrl) }
                    )
                }
            }
        }
    }

    if (showBuyDialog) {
        AlertDialog(
            onDismissRequest = { showBuyDialog = false },
            containerColor = Color(0xFF12121A),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            text = { Text(locManager.get("sh_buy_q"), fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            dismissButton = {
                TextButton(onClick = { showBuyDialog = false }) {
                    Text(locManager.get("v_no"), color = Color.Gray)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (magicEnergy >= 10) {
                        val newEnergy = magicEnergy - 10
                        onEnergyUpdate(newEnergy)
                        prefs.edit {
                            putInt("global_magic_energy", newEnergy)
                            putBoolean(pendingBuyId, true)
                        }

                        // ЧАСТЬ 3 ТЗ: Бухгалтерский учёт списания энергии в облаке Supabase
                        val backupStatus = prefs.getString("email_backup_status", "none") ?: "none"
                        if (backupStatus == "saved") {
                            val sproId = prefs.getString("user_magic_spro_id", "") ?: ""
                            if (sproId.isNotEmpty()) {
                                scope.launch {
                                    NetworkManager.logEnergyTransaction(
                                        sproId = sproId,
                                        energyChange = -10, // Честное списание в минус
                                        reason = pendingBuyId // Чистый уникальный ID видео-слота из кода!
                                    )
                                }
                            }
                        }
                    }
                    showBuyDialog = false
                }) {
                    Text(locManager.get("v_yes"), color = neonGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

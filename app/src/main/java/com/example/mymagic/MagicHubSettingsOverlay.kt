@file:Suppress("SpellCheckingInspection", "ConvertToStringTemplate")
package com.example.mymagic

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MagicHubSettingsOverlay(
    visible: Boolean,
    isDebugHudExternal: Boolean,
    onDebugHudChanged: (Boolean) -> Unit,
    onClose: () -> Unit,
    speechTools: MagicSpeechTools,
    locManager: MagicLocalizationManager,
    onPurchaseRequested: (String) -> Unit
) {
    if (!visible) return
    val ctx = LocalContext.current
    val prefs = remember { MagicPrefsFactory.create(ctx) }

    val neonGreen = Color(0xFF00FF66)
    val customBlue = Color(0xFF1565C0)

    var dialogRefreshTrigger by remember { mutableIntStateOf(0) }
    var magicEnergy by remember { mutableIntStateOf(prefs.getInt("global_magic_energy", 0)) }
    var isPremium by remember { mutableStateOf(prefs.getBoolean("is_premium_user", false)) }

    val showWallDialog = remember { mutableStateOf(false) }
    val showHubDialog = remember { mutableStateOf(false) }
    val showLangDialog = remember { mutableStateOf(false) }

    var showBackupDialogInSettings by remember { mutableStateOf(false) }
    var backupStatusState by remember { mutableStateOf(prefs.getString("email_backup_status", "none") ?: "none") }

    var isShopMenuExpanded by remember { mutableStateOf(false) }
    var shopCategory by remember { mutableStateOf("") }

    var customToastText by remember { mutableStateOf("") }
    var showCustomToast by remember { mutableStateOf(false) }

    fun triggerCustomToast(text: String) {
        customToastText = text
        showCustomToast = true
    }

    LaunchedEffect(showCustomToast) {
        if (showCustomToast) {
            delay(2000.milliseconds)
            showCustomToast = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                magicEnergy = prefs.getInt("global_magic_energy", 0)
                isPremium = prefs.getBoolean("is_premium_user", false)
                backupStatusState = prefs.getString("email_backup_status", "none") ?: "none"
                dialogRefreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.9f)).padding(24.dp), Alignment.Center) {
        MagicSettingsDialogsContainer(
            context = ctx,
            prefs = prefs,
            isPremium = isPremium,
            magicEnergy = magicEnergy,
            neonGreen = neonGreen,
            locManager = locManager,
            showWallDialog = showWallDialog,
            showHubDialog = showHubDialog,
            showLangDialog = showLangDialog,
            dialogRefreshTrigger = dialogRefreshTrigger,
            onEnergyUpdate = { newAmount ->
                magicEnergy = newAmount
                prefs.edit { putInt("global_magic_energy", newAmount) }
            },
            onRefreshTriggerIncrement = { dialogRefreshTrigger++ },
            onToastRequested = { triggerCustomToast(it) }
        ) { checkPerm, requestUnlock ->
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.95f)
                    .background(Color(0xFF12121A), RoundedCornerShape(16.dp))
                    .border(2.dp, neonGreen, RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(locManager.get("s_t"), color = neonGreen, fontSize = 20.sp, fontWeight = FontWeight.Black)

                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, neonGreen.copy(0.5f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24).copy(0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = locManager.get("m_spells_lang"), color = neonGreen.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        LanguageSettingsRow(isPremium = isPremium, prefs = prefs, lumGreen = neonGreen, speechTools = speechTools, locManager = locManager, refreshTrigger = dialogRefreshTrigger, onRequestUnlockConfirmation = { requestUnlock(it) })
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.Black.copy(0.4f), RoundedCornerShape(8.dp)).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(locManager.get("t_d_m"), color = Color.White, fontSize = 12.sp)
                    Switch(checked = isDebugHudExternal, onCheckedChange = onDebugHudChanged, colors = SwitchDefaults.colors(checkedThumbColor = neonGreen))
                }

                MagicEnergyBillingCard(
                    magicEnergy = magicEnergy,
                    isPremium = isPremium,
                    prefs = prefs,
                    locManager = locManager,
                    customBlue = customBlue,
                    onEnergyUpdate = { magicEnergy = it; dialogRefreshTrigger++ },
                    onToastRequested = { triggerCustomToast(it) },
                    onPurchaseRequested = onPurchaseRequested
                )

                MagicShopSection(isExpanded = isShopMenuExpanded, onToggle = { isShopMenuExpanded = it }, category = shopCategory, onCategoryChange = { shopCategory = it }, magicEnergy = magicEnergy, prefs = prefs, locManager = locManager, onEnergyUpdate = { magicEnergy = it; dialogRefreshTrigger++ })

                val garnetNeon = Color(0xFF960018)
                Card(modifier = Modifier.fillMaxWidth().clickable { ctx.startActivity(Intent(Intent.ACTION_VIEW, "https://pixabay.com".toUri())) }.border(1.dp, garnetNeon.copy(0.5f), RoundedCornerShape(12.dp)), colors = CardDefaults.cardColors(containerColor = garnetNeon.copy(0.15f).compositeOver(Color.Black))) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🎵 ", fontSize = 16.sp)
                        Text(text = locManager.get("m_free_audio").uppercase(), color = Color(0xFFFF4D6A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                MagicSystemSettingsSection(locManager = locManager, showLangDialog = showLangDialog, showWallDialog = showWallDialog, showHubDialog = showHubDialog)

                if (backupStatusState != "saved") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showBackupDialogInSettings = true }
                            .border(1.dp, Color(0xFF64B5F6), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = painterResource(id = R.drawable.btn_cloud_sky),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "🔄 " + locManager.get("cloud_sync").uppercase(),
                                    color = Color.Blue,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(text = "⚙️", color = Color.White.copy(0.6f), fontSize = 14.sp)
                            }
                        }
                    }
                }
                CombatSpellsSettingsSection(isPremium = isPremium, prefs = prefs, locManager = locManager, refreshTrigger = dialogRefreshTrigger, onSelectFileRequested = { spellId, fileType -> checkPerm(spellId, fileType) }, onRequestUnlockConfirmation = { requestUnlock(it) })
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = neonGreen), modifier = Modifier.width(180.dp).padding(vertical = 8.dp)) {
                    Text(locManager.get("b_c"), color = Color.Black, fontWeight = FontWeight.Black)
                }
            }
        }
        AnimatedVisibility(visible = showCustomToast, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2A)), shape = RoundedCornerShape(12.dp), modifier = Modifier.width(240.dp).border(2.dp, Color.Red.copy(0.7f), RoundedCornerShape(12.dp))) {
                Text(text = customToastText, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp).fillMaxWidth())
            }
        }
        val currentSproId = prefs.getString("user_magic_spro_id", "") ?: ""
        CloudSaveEmailDialog(
            visible = showBackupDialogInSettings,
            sproId = currentSproId,
            locManager = locManager,
            onDismissRequested = { showBackupDialogInSettings = false },
            onSaveSuccess = { _ ->
                magicEnergy = prefs.getInt("global_magic_energy", 0)
                isPremium = prefs.getBoolean("is_premium_user", false)
                backupStatusState = "saved"
                showBackupDialogInSettings = false
                triggerCustomToast("✓ Synced")
                dialogRefreshTrigger++
            }
        )



    }
}
@file:Suppress("SpellCheckingInspection", "CascadeIf")
package com.example.mymagic

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CloudSaveEmailDialog(
    visible: Boolean,
    sproId: String,
    locManager: MagicLocalizationManager,
    onDismissRequested: () -> Unit,
    onSaveSuccess: (String) -> Unit
) {
    if (!visible) return

    val scope = rememberCoroutineScope()
    val currentContext = LocalContext.current
    val prefs = remember { MagicPrefsFactory.create(currentContext) }

    var currentStep by remember { mutableStateOf("input") } // input, warn_decline, restore_mode, confirm, otp, syncing
    var emailInput by remember { mutableStateOf("") }
    var idRestoreInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var isNetworkLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val deepBlueColor = Color(0xFF1565C0)
    val dialogBorderColor = Color(0xFF64B5F6)

    Dialog(
        onDismissRequest = { if (currentStep == "input") onDismissRequested() },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, dialogBorderColor, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                Image(
                    painter = painterResource(id = R.drawable.bg_dialog_sky),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (currentStep) {
                        "input" -> {
                            Text(text = locManager.get("cs_title"), color = deepBlueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it; errorMessage = "" },
                                label = { Text("Email", color = deepBlueColor.copy(0.6f)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (errorMessage.isNotEmpty()) {
                                Text(text = errorMessage, color = Color(0xFFFF4D4D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TextButton(onClick = { currentStep = "warn_decline"; errorMessage = "" }) { Text(text = locManager.get("cs_decline"), color = deepBlueColor.copy(0.7f), fontWeight = FontWeight.Bold) }
                                TextButton(onClick = { currentStep = "restore_mode"; errorMessage = "" }) { Text(text = locManager.get("cs_restore_btn"), color = deepBlueColor, fontWeight = FontWeight.Black) }
                                Button(
                                    onClick = { if (emailInput.trim().contains("@")) currentStep = "confirm" else errorMessage = locManager.get("cs_err_format") },
                                    colors = ButtonDefaults.buttonColors(containerColor = deepBlueColor.copy(0.15f)),
                                    modifier = Modifier.border(1.dp, deepBlueColor.copy(0.3f), RoundedCornerShape(50.dp))
                                ) { Text(text = locManager.get("cs_send"), color = deepBlueColor, fontWeight = FontWeight.Black) }
                            }
                        }
                        "warn_decline" -> {
                            Text(text = locManager.get("cs_warn_text"), color = Color(0xFFFF4D4D), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { currentStep = "input" }, colors = ButtonDefaults.buttonColors(containerColor = deepBlueColor), modifier = Modifier.weight(1f)) { Text(text = locManager.get("cs_return"), color = Color.White) }
                                TextButton(onClick = { onDismissRequested() }, modifier = Modifier.weight(1f)) { Text(text = locManager.get("cs_accept"), color = Color.Gray) }
                            }
                        }
                        "confirm" -> {
                            Text(text = locManager.get("cs_verify") + "\n\n" + emailInput, color = deepBlueColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            if (isNetworkLoading) CircularProgressIndicator(color = deepBlueColor, modifier = Modifier.size(24.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TextButton(onClick = { currentStep = "input" }) { Text(text = locManager.get("cs_change"), color = deepBlueColor.copy(0.6f)) }
                                Button(onClick = {
                                    isNetworkLoading = true
                                    scope.launch {
                                        val isSent = withContext(Dispatchers.IO) { NetworkManager.sendOtpToEmail(emailInput.trim()) }
                                        isNetworkLoading = false
                                        if (isSent) currentStep = "otp" else { errorMessage = locManager.get("cs_err_net"); currentStep = "input" }
                                    }
                                }) { Text(text = locManager.get("b_o"), color = deepBlueColor) }
                            }
                        }
                        "otp" -> {
                            Text(text = locManager.get("cs_otp_sent"), color = deepBlueColor, fontSize = 14.sp, textAlign = TextAlign.Center)
                            OutlinedTextField(value = otpInput, onValueChange = { if (it.length <= 8) otpInput = it; errorMessage = "" }, label = { Text(text = locManager.get("cs_hint_code")) }, modifier = Modifier.fillMaxWidth())
                            Row(modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { currentStep = "input" }) { Text(text = locManager.get("cs_back")) }
                                Button(onClick = {
                                    if (otpInput.length in 6..8) {
                                        isNetworkLoading = true
                                        scope.launch {
                                            val isVerified = withContext(Dispatchers.IO) { NetworkManager.verifyOtpAndSaveMonolithic(sproId, emailInput.trim(), otpInput.trim(), prefs.getInt("global_magic_energy", 0), prefs.getBoolean("is_premium_user", false)) }
                                            isNetworkLoading = false
                                            if (isVerified) {
                                                prefs.edit { putString("saved_user_email", emailInput.trim()); putString("email_backup_status", "saved"); putString("user_magic_spro_id", sproId) }
                                                onSaveSuccess(emailInput.trim())
                                            } else errorMessage = locManager.get("cs_err_code")
                                        }
                                    } else errorMessage = locManager.get("cs_err_length")
                                }) { Text(text = locManager.get("cs_confirm")) }
                            }
                        }
                        "restore_mode", "syncing" -> {
                            // Вызов вынесенного движка восстановления
                            // УДАЛИ СТАРЫЙ ВЫЗОВ И ВСТАВЬ ЭТОТ:
                            CloudRestoreEngineContent(
                                currentStep = currentStep,
                                emailInput = emailInput,
                                idRestoreInput = idRestoreInput,
                                errorMessage = errorMessage,
                                deepBlueColor = deepBlueColor,
                                locManager = locManager,
                                prefs = prefs,
                                onEmailChange = { emailInput = it },
                                onIdChange = { idRestoreInput = it },
                                onErrorChange = { errorMessage = it },
                                onStepChange = { currentStep = it },
                                onLoadingChange = { isNetworkLoading = it },
                                onRestoreDone = onSaveSuccess
                            )

                        }
                    }
                }
            }
        }
    }
}

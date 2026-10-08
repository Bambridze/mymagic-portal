@file:Suppress("SpellCheckingInspection", "DEPRECATION")

package com.example.mymagic

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var onUnlockSuccessCallback: (() -> Unit)? = null
    private lateinit var systemUnlockLauncher: ActivityResultLauncher<android.content.Intent>

    lateinit var billingHelper: MagicBillingHelper
        private set

    val isCloudDialogVisibleState = mutableStateOf(false)
    val temporaryGeneratedIdState = mutableStateOf("")
    val isFraudWarningVisibleState = mutableStateOf(false)

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        val prefs = MagicPrefsFactory.create(this)

        if (!prefs.contains("is_premium_user")) prefs.edit { putBoolean("is_premium_user", false) }
        if (!prefs.contains("global_magic_energy")) prefs.edit { putInt("global_magic_energy", 0) }
        if (!prefs.contains("energy_pack_purchased_flag")) prefs.edit { putBoolean("energy_pack_purchased_flag", false) }
        if (!prefs.contains("email_backup_status")) prefs.edit { putString("email_backup_status", "none") }
        if (!prefs.contains("saved_user_email")) prefs.edit { putString("saved_user_email", "") }

        billingHelper = MagicBillingHelper { itemId, paymentId ->
            when (itemId) {
                "premium_unlocked", "premium_test" -> {
                    prefs.edit { putBoolean("is_premium_user", true) }
                    var sproId = prefs.getString("user_magic_spro_id", "") ?: ""
                    if (sproId.isEmpty()) {
                        sproId = IdManager.generateMagicId(this)
                        prefs.edit { putString("user_magic_spro_id", sproId) }
                    }
                    lifecycleScope.launch {
                        NetworkManager.logEnergyTransaction(sproId, 0, itemId)
                    }
                }
                "energy_pack_100" -> {
                    val currentEnergy = prefs.getInt("global_magic_energy", 0)
                    prefs.edit {
                        putInt("global_magic_energy", currentEnergy + 100)
                        putBoolean("energy_pack_purchased_flag", true)
                    }
                    var sproId = prefs.getString("user_magic_spro_id", "") ?: ""
                    if (sproId.isEmpty()) {
                        sproId = IdManager.generateMagicId(this)
                        prefs.edit { putString("user_magic_spro_id", sproId) }
                    }
                    lifecycleScope.launch {
                        val cloudLogged = NetworkManager.logEnergyTransaction(sproId, 100, "pack_100_id_$paymentId")
                        if (cloudLogged) {
                            billingHelper.consumePurchasedItem(paymentId)
                        }
                    }
                }
            }

            val finalSproId = prefs.getString("user_magic_spro_id", "") ?: ""
            val currentBackupStatus = prefs.getString("email_backup_status", "none") ?: "none"
            if (currentBackupStatus == "none" && finalSproId.isNotEmpty()) {
                temporaryGeneratedIdState.value = finalSproId
                isCloudDialogVisibleState.value = true
            }
        }

        billingHelper.registerLauncher(this)
        systemUnlockLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                onUnlockSuccessCallback?.invoke()
                onUnlockSuccessCallback = null
            }
        }

        billingHelper.checkOwnedItems(this@MainActivity, prefs)

        setContent {
            val currentLocManager = remember { MagicLocalizationManager(this) }

            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                MagicAppNavigation(
                    onPurchaseRequested = { productId ->
                        billingHelper.launchPurchase(productId)
                    }
                )

                CloudSaveEmailDialog(
                    visible = isCloudDialogVisibleState.value,
                    sproId = temporaryGeneratedIdState.value,
                    locManager = currentLocManager,
                    onDismissRequested = {
                        prefs.edit { putString("email_backup_status", "declined") }
                        isCloudDialogVisibleState.value = false
                    },
                    onSaveSuccess = { verifiedEmail ->
                        prefs.edit {
                            putString("user_magic_spro_id", temporaryGeneratedIdState.value)
                            putString("email_backup_status", "saved")
                            putString("saved_user_email", verifiedEmail)
                        }
                        isCloudDialogVisibleState.value = false
                        Log.d("MagicFix", "Связка успешно сохранена для: $verifiedEmail")
                    }
                )

                FraudWarningDialog(
                    visible = isFraudWarningVisibleState.value,
                    locManager = currentLocManager,
                    onDismissRequested = {
                        isFraudWarningVisibleState.value = false
                    }
                )
            }
        }
    }
    fun triggerAntiFraudRollback(purchaseDateIso: String) {
        val prefs = MagicPrefsFactory.create(this)
        val backupStatus = prefs.getString("email_backup_status", "none") ?: "none"
        val sproId = prefs.getString("user_magic_spro_id", "") ?: ""
        val userEmail = prefs.getString("saved_user_email", "") ?: ""

        if (backupStatus == "saved" && sproId.isNotEmpty()) {
            lifecycleScope.launch {
                val rawJson = NetworkManager.fetchTransactionsAfterRefund(sproId, purchaseDateIso)
                var currentEnergy = prefs.getInt("global_magic_energy", 0)
                currentEnergy -= 100

                if (rawJson != null) {
                    prefs.edit {
                        magicSpellRegistry.forEach { spell ->
                            val slotKey = "unlocked_command_${spell.id}"
                            if (rawJson.contains(spell.id) && prefs.getBoolean(slotKey, false)) {
                                putBoolean("unlocked_video_${spell.id}", false)
                                putBoolean("unlocked_sound_${spell.id}", false)
                                putBoolean(slotKey, false)
                                currentEnergy += 20
                            }
                        }
                    }
                }

                prefs.edit {
                    putInt("global_magic_energy", maxOf(0, currentEnergy))
                }

                if (userEmail.contains("@")) {
                    val currentLocManager = MagicLocalizationManager(this@MainActivity)
                    val fraudMessage = currentLocManager.get("fraud_message")
                    NetworkManager.sendFraudEmailNotification(userEmail, fraudMessage)
                } else {
                    Log.e("MagicAntiFraud", "Не удалось отправить письмо: email пуст или некорректен.")
                }

                isFraudWarningVisibleState.value = true
            }
        }
    }

    fun checkEmailBackupOnAdRewarded(prefs: android.content.SharedPreferences, onTriggerToShow: (String) -> Unit) {
        lifecycleScope.launch {
            var silentSproId = prefs.getString("user_magic_spro_id", "") ?: ""
            if (silentSproId.isEmpty()) {
                silentSproId = IdManager.generateMagicId(this@MainActivity)
                prefs.edit { putString("user_magic_spro_id", silentSproId) }
            }

            // ИСПРАВЛЕНО: Лишний хвост полностью стерт, передаются строго 3 аргумента
            NetworkManager.logEnergyTransaction(silentSproId, 1, "ad_rewarded")

            val currentStatus = prefs.getString("email_backup_status", "none") ?: "none"
            if (currentStatus == "none") {
                runOnUiThread {
                    onTriggerToShow(silentSproId)
                    temporaryGeneratedIdState.value = silentSproId
                    isCloudDialogVisibleState.value = true
                }
            }
        }
    }

    fun showSystemUnlock(localizationManager: MagicLocalizationManager, onSuccess: () -> Unit) {
        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (km.isDeviceSecure) {
            onUnlockSuccessCallback = onSuccess
            val title = localizationManager.get("b_e_b")
            val intent = km.createConfirmDeviceCredentialIntent(title, " ")
            if (intent != null) {
                systemUnlockLauncher.launch(intent)
            } else {
                onSuccess()
                onUnlockSuccessCallback = null
            }
        } else {
            onSuccess()
        }
    }
}

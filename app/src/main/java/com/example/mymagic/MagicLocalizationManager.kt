@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

enum class MagicLocalResult {
    SUCCESS, CACHE_USED, NETWORK_ERROR
}

class MagicLocalizationManager(private val context: Context) {
    val translations = mutableStateMapOf<String, String>()
    private val prefs = MagicPrefsFactory.create(context)
    private val basePortalUrl = "https://github.com/Bambridze/mymagic-portal/releases/download/v1.1.0"

    var isDownloading by mutableStateOf(false)
        private set

    private val fallbackMap = mapOf(
        "i_v_p" to "Password",
        "d_w_t" to "Please read the instructions for our application carefully",
        "d_l_t" to "LEGAL AGREEMENT",
        "d_a_l" to "⚠️ AGE RESTRICTION (13+):\nBy accepting these terms of use, you officially confirm that you are already 13 years old.",
        "d_p" to "🔒 PRIVACY POLICY:\nWe respect your privacy. The application may request your email address solely to ensure that you do not lose your progress inside the application in case of phone loss or other similar cases. Your email address will be stored on highly secure servers and will not be shared with third parties. You can also decline to provide your email address.",
        "d_s" to "⚠️ REAL WORLD:\nBy using the app, you realize where you are, and what you are doing. Your safety and the safety of your device is your own responsibility.",
        "d_instr_btn" to "Instruction",
        "d_tos_btn" to "Terms (ToS)",
        "d_priv_btn" to "Privacy Policy",
        "d_c_legal" to "I have read and accept the Terms of Service and Privacy Policy",
        "d_b_e" to "CONFIRM & ENTER",
        "i_t" to "ENTRY INSTRUCTIONS",
        "i_d" to "To enter your World of Magic, speak the activation spell CORRECTLY in English:\n\n",
        "i_f" to "\n\nThe system triggers when you finish the phrase completely and make a pause.",
        "b_o" to "OK",
        "b_c" to "CLOSE",
        "b_s" to "SAVE",
        "b_ca" to "CANCEL",
        "b_cf" to "CONFIRM",
        "w_s" to "I'm sleeping with one eye open)",
        "b_e_b" to "Backdoor Entry",
        "s_t" to "SETTINGS",
        "b_b_p" to "💎 BUY PREMIUM",
        "t_d_m" to "Debug mode",
        "m_site_btn" to "Visit Website",
        "m_e_t" to "🔑 ENTRY",
        "m_h_t" to "🏰 HALL",
        "d_w_tl" to "Lock Screen Settings",
        "d_h_tl" to "Main Hall Settings",
        "b_s_f" to "Scale: Fill Screen",
        "b_s_z" to "Scale: Max Zoom (+25%)",
        "u_d_t" to "Confirm Unlock",
        "u_d_d" to "Spend 20 ⚡️?",
        "s_v_c" to "Voice Command",
        "m_video" to "Video",
        "m_audio" to "Audio",
        "m_off" to "OFF",
        "m_spells_lang" to "Language of Spells",
        "m_folder_empty" to "The Download/MyMagicMedia folder is empty.\nPlease put your .mp4 and .mp3 files there.",
        "d_h_h" to "Heard: ",
        "vosk_title" to "Your Choice",
        "m_interface_lang" to "Interface Language",
        "engine_init" to "Waiting for initialization...",
        "engine_download" to "Downloading language pack...",
        "engine_ready" to "Magic Engine Ready!",
        "engine_error" to "❌ Connection Error",
        "engine_sleep" to "Engine sleeping...",
        "m_minutes" to "minutes",
        "m_select_lang_title" to "SELECT INTERFACE LANGUAGE",
        "m_timeout_label" to "Magic Mode Timeout: ",
        "v_change_q" to "Replace?",
        "v_yes" to "Yes",
        "v_no" to "No",
        "b_w_a" to "📺 Watch Ad (+1⚡️)",
        "sh_buy_q" to "Spend 10 ⚡️?",
        "m_free_audio" to "🎵 FREE AUDIO",
        "sh_cat_spells" to "🔮 SPELLS",
        "sh_menu_btn" to "VIDEO FOR APP",
        "sh_cat_wall" to "entry",
        "sh_cat_hub" to "hall",
        "sp_light_on" to "Turn On Light",
        "sp_light_max" to "Maximum Light",
        "sp_light_off" to "Turn Off Light",
        "sp_ignite_burst" to "Strike",
        "sp_defendo_shield" to "Shield",
        "sp_frizio_freeze" to "Freeze",
        "sp_repulso_disarm" to "Disarm",
        "sp_mortis_fatal" to "Forbidden Curse",
        "d_c_instr" to "I confirm that I have carefully read the instructions",
        "m_not_enough_energy" to "Not enough ⚡",
        "sh_coming_soon" to "Coming soon!",
        "d_c_not_legal" to "I do not accept the Terms of Service and Privacy Policy",
        "d_b_reject" to "REJECT & EXIT",
        "b_buy_energy" to "Buy 100 ⚡",
        "cs_title" to "To ensure your progress is not lost, please provide your email address",
        "cs_decline" to "Decline",
        "cs_send" to "Send",
        "cs_err_format" to "Invalid email format",
        "cs_verify" to "Please verify that your email is entered correctly:",
        "cs_change" to "Change",
        "cs_err_net" to "Network error. Please try again later.",
        "cs_otp_sent" to "A 6-digit confirmation code has been sent to your email. Enter it below:",
        "cs_hint_code" to "Confirmation Code",
        "cs_back" to "Back",
        "cs_confirm" to "Confirm",
        "cs_err_code" to "Incorrect code or time expired",
        "cs_err_length" to "The code must be exactly 6 digits",
        "cs_warn_text" to "Are you sure? If you do not provide your email, all your content unlock progress may be lost permanently.",
        "cs_return" to "Return",
        "cs_accept" to "Accept",
        "cloud_sync" to "Cloud Sync",
        "cs_restore_btn" to "Restore Progress",
        "cs_restore_title" to "Enter your unique identifier",
        "cs_restore_otp_sent" to "Enter the code sent to your email",
        "cs_restore_success" to "Saved progress restored successfully.",
        "cs_restore_err_not_found" to "ID not found. Please check and try again.",
        "fraud_title" to "Attention",
        "fraud_message" to "Refund detected for purchase(s), your progress will be updated in accordance with the recorded data. You can restore your progress using the established methods."
    )

    init {
        val cachedLang = prefs.getString("app_language", "en") ?: "en"
        applyCachedLanguage(cachedLang)
    }

    fun get(key: String): String {
        return translations[key] ?: fallbackMap[key] ?: key
    }

    private fun applyCachedLanguage(langCode: String): Boolean {
        translations.clear()
        if (langCode == "en") return true

        val cacheFile = File(context.cacheDir, "locale_$langCode.json")
        if (cacheFile.exists()) {
            return try {
                val jsonString = cacheFile.readText()
                val jsonObject = JSONObject(jsonString)
                val tempMap = mutableMapOf<String, String>()
                jsonObject.keys().forEach { key ->
                    tempMap[key] = jsonObject.getString(key)
                }
                translations.putAll(tempMap)
                true
            } catch (_: Exception) {
                false
            }
        }
        return false
    }

    suspend fun fetchLanguagePack(langCode: String): MagicLocalResult {
        if (langCode == "en") {
            translations.clear()
            prefs.edit { putString("app_language", "en") }
            return MagicLocalResult.SUCCESS
        }
        isDownloading = true
        val jsonString = downloadJsonFromPortal(langCode)
        if (jsonString == null) {
            isDownloading = false
            return if (applyCachedLanguage(langCode)) MagicLocalResult.CACHE_USED else MagicLocalResult.NETWORK_ERROR
        }
        val parseSuccess = withContext(Dispatchers.IO) {
            try {
                val jsonObject = JSONObject(jsonString)
                val tempMap = mutableMapOf<String, String>()
                jsonObject.keys().forEach { key ->
                    tempMap[key] = jsonObject.getString(key)
                }
                File(context.cacheDir, "locale_$langCode.json").writeText(jsonString)
                withContext(Dispatchers.Main) {
                    translations.clear()
                    translations.putAll(tempMap)
                    prefs.edit { putString("app_language", langCode) }
                }
                true
            } catch (_: Exception) {
                false
            }
        }
        isDownloading = false
        if (parseSuccess) return MagicLocalResult.SUCCESS
        return if (applyCachedLanguage(langCode)) MagicLocalResult.CACHE_USED else MagicLocalResult.NETWORK_ERROR
    }

    private suspend fun downloadJsonFromPortal(langCode: String): String? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = URL("$basePortalUrl/$langCode.json").openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}

package com.example.mymagic

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object MagicPrefsFactory {
    private const val PREFS_NAME = "magic_secure_prefs"

    fun create(context: Context): SharedPreferences {
        val appContext = context.applicationContext
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) { // ИСПРАВЛЕНО: Ловим абсолютно любые ошибки (Throwable), включая сбои KeyStore из-за R8!
            Log.e("MagicPrefs", "Критический сбой шифрования в релизе. Откат на обычные префы.", e)

            // Железный fallback: если шифрование упало, отдаем обычные префы, чтобы приложение ЖИЛО
            appContext.getSharedPreferences("magic_regular_prefs", Context.MODE_PRIVATE)
        }
    }
}

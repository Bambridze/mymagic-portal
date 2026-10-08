@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale

object IdManager {

    private const val SECRET_SALT = "Krokozyaba"

    /**
     * Получает системный "хвост" устройства (Android ID)
     */
    @SuppressLint("HardwareIds")
    fun getDeviceTail(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    /**
     * Генерирует красивый маскированный ID вида S⚡PRO-XXXX-XXXX на основе хвоста
     */
    fun generateMagicId(context: Context): String {
        val tail = getDeviceTail(context)
        if (tail == "unknown") return "S⚡PRO-ERROR-ID"

        return try {
            val input = tail + SECRET_SALT
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))

            // Переводим в строку и берем первые 8 символов для красоты
            val hexString = hashBytes.joinToString("") { "%02x".format(it) }
            val cleanHex = hexString.take(8).uppercase(Locale.ROOT)

            // Разбиваем дефисом на две части по 4 символа
            val part1 = cleanHex.substring(0, 4)
            val part2 = cleanHex.substring(4, 8)

            "S⚡PRO-$part1-$part2"
        } catch (_: Throwable) {
            "S⚡PRO-FALLBACK"
        }
    }
}


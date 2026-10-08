@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.edit
import java.io.File
import java.io.FileOutputStream

class MagicMediaImporter(private val context: Context) {

    init {
        // Оставляем фичу: приложение при старте само создает папку в Загрузках для лавки контента
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val magicFolder = File(downloadsDir, "MyMagicMedia")
            if (!magicFolder.exists()) {
                magicFolder.mkdirs()
            }
        } catch (e: Exception) {
            Log.e("MagicMedia", "Не удалось создать публичную папку MyMagicMedia", e)
        }
    }

    /**
     * Безопасное копирование выбранного пользователем медиафайла через системный Uri
     * во внутренний изолированный каталог приложения (песочницу), одобренную Google Play.
     */
    fun importLocalFile(sourceUri: Uri, targetId: String, type: String): Boolean {
        return try {
            val mediaDir = File(context.filesDir, "magic_media").apply {
                mkdirs()
            }
            val ext = if (type == "video") "mp4" else "mp3"
            val dest = File(mediaDir, "${targetId}_$ext.$ext")

            // Открываем безопасный системный поток чтения по Uri без каких-либо разрешений в Manifest
            context.contentResolver.openInputStream(sourceUri).use { input ->
                if (input == null) return false
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }

            val cryptoPrefs = MagicPrefsFactory.create(context)
            cryptoPrefs.edit {
                putString("custom_${type}_file_$targetId", dest.name)
            }

            Log.d("MagicMedia", "Успешный импорт из Uri во внутреннее хранилище: ${dest.name}")
            true
        } catch (e: Exception) {
            Log.e("MagicMedia", "Ошибка копирования файла через ContentResolver", e)
            false
        }
    }
}

@file:Suppress("SpellCheckingInspection") package com.example.mymagic

import android.content.Context
import android.util.Log
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class MagicModelDownloader(private val context: Context) {
    private val baseStorageUrl = "https://github.com/Bambridze/mymagic-portal/releases/download/v1.0.0"
    private val rootDir: File = File(context.filesDir, "vosk_models")

    init {
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }
    }

    fun getTargetDirForLang(lang: String): File {
        return if (lang == "en") {
            File(rootDir, "model_en")
        } else {
            File(rootDir, "model_custom")
        }
    }

    /**
     * Честная проверка целостности. Модель готова только если есть маркер
     * И физически существуют критически важные папки графа Vosk.
     */
    fun isModelReady(lang: String): Boolean {
        val folder = getTargetDirForLang(lang)
        val markerFile = File(folder, "model.ready")
        val graphFolder = File(folder, "graph")

        // Если маркера нет или папка графа пуста/отсутствует — модель НЕ готова
        return markerFile.exists() && graphFolder.exists() && graphFolder.isDirectory
    }

    fun clearCustomModel() {
        try {
            val customFolder = getTargetDirForLang("custom")
            if (customFolder.exists()) {
                customFolder.deleteRecursively()
            }
            customFolder.mkdirs()
        } catch (e: Exception) {
            Log.e("MagicDownload", "Clear custom slot failed", e)
        }
    }

    fun downloadAndUnpackLang(lang: String): Boolean {
        if (lang == "en") return false

        val fileUrl = "$baseStorageUrl/$lang.zip"
        val targetFolder = getTargetDirForLang(lang)

        clearCustomModel()

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        try {
            Log.d("MagicDownload", "Connecting to: $fileUrl")
            val url = URL(fileUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("MagicDownload", "Server returned HTTP $responseCode")
                return false
            }

            inputStream = connection.inputStream
            val zipIn = ZipInputStream(inputStream)
            var entry = zipIn.nextEntry

            while (entry != null) {
                val outFile = File(targetFolder, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fileOut ->
                        val buffer = ByteArray(4096)
                        var readBytes: Int
                        while (zipIn.read(buffer).also { readBytes = it } != -1) {
                            fileOut.write(buffer, 0, readBytes)
                        }
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }

            // Финальная проверка перед созданием маркера успеха
            val graphFolder = File(targetFolder, "graph")
            return graphFolder.exists() && graphFolder.isDirectory

        } catch (e: Exception) {
            Log.e("MagicDownload", "Download error for $lang: ${e.message}", e)
            return false
        } finally {
            inputStream?.close()
            connection?.disconnect()
        }
    }

    fun copyAssetFile(assetPath: String, targetFile: File): Boolean {
        try {
            targetFile.parentFile?.mkdirs()
            context.assets.open(assetPath).use { streamIn ->
                FileOutputStream(targetFile).use { streamOut ->
                    val buffer = ByteArray(4096)
                    var readBytes: Int
                    while (streamIn.read(buffer).also { readBytes = it } != -1) {
                        streamOut.write(buffer, 0, readBytes)
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e("MagicDownload", "Asset copy failed: $assetPath", e)
            return false
        }
    }
}

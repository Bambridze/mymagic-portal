@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MagicShopDownloader(private val context: Context) {

    // Честный HEAD-запрос для проверки наличия файла на серверах GitHub (с обработкой редиректов)
    suspend fun checkFileExists(urlStr: String): Boolean = withContext(Dispatchers.IO) {
        var currentUrl = urlStr
        var redirectsCount = 0
        val maxRedirects = 5
        while (redirectsCount < maxRedirects) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.instanceFollowRedirects = false

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return@withContext true
                } else if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == 307 || responseCode == 308) {
                    val location = connection.getHeaderField("Location")
                    if (!location.isNullOrEmpty()) {
                        currentUrl = location
                        redirectsCount++
                        continue
                    }
                    return@withContext false
                } else {
                    return@withContext false
                }
            } catch (_: Exception) {
                return@withContext false
            } finally {
                connection?.disconnect()
            }
        }
        false
    }

    // Скачивание байтов напрямую в системную папку Download/MyMagicMedia
    suspend fun downloadFileDirectly(urlStr: String, fileName: String): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = true

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val magicFolder = File(downloadsDir, "MyMagicMedia")
                if (!magicFolder.exists()) {
                    magicFolder.mkdirs()
                }

                val targetFile = File(magicFolder, fileName)
                connection.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Сканируем файл, чтобы Android MediaStore мгновенно добавил его в систему
                MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), null) { path, _ ->
                    Log.d("MagicShop", "Файл успешно проиндексирован: $path")
                }
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e("MagicShop", "Ошибка скачивания файла $fileName: ${e.message}")
            return@withContext false
        } finally {
            connection?.disconnect()
        }
    }
}



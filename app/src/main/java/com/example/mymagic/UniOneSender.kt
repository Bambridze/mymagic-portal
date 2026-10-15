@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

object UniOneSender {
    private const val TAG = "MagicUniOne"
    private val mailClient = HttpClient()

    fun sendMagicIdEmailAsync(email: String, sproId: String) {
        Thread {
            try {
                Log.d(TAG, "=== ЗАПУСК ОТПРАВКИ ИЗ ИЗОЛИРОВАННОГО ФАЙЛА ===")

                val cleanEmail = email.trim()
                val jsonBody = "{" +
                        "\"username\":\"7177152\"," +
                        "\"message\":{" +
                        "\"recipients\":[{\"email\":\"" + cleanEmail + "\"}]," +
                        "\"body\":{\"html\":\"<div style='font-family:sans-serif;padding:20px;color:#1565C0;'><p>Your email address has been successfully verified.</p><p>Your unique identifier for recovering your progress in app: <b>" + sproId.trim() + "</b></p><br><p style='color:#FF4D4D;'><b>IMPORTANT: This identifier can be used to restore data ONLY ONCE.</b></p></div>\"}," +
                        "\"subject\":\"Magic ID.\"," +
                        "\"from_email\":\"support@sproduction.it.com\"," +
                        "\"from_name\":\"MyMagic App\"" +
                        "}" +
                        "}"


                kotlinx.coroutines.runBlocking {
                    // ЖЕСТКОЕ УСЛОВИЕ: Сохраняем ответ в переменную, заставляя поток ждать реального ответа сети!
                    val response = mailClient.post("https://eu1.unione.io/en/transactional/api/v1/email/send.json") {
                        contentType(ContentType.Application.Json)
                        header("X-API-KEY", "6chqodw6egheh87kzryuoa5ca88a67387y6jdzdy")
                        setBody(jsonBody)
                    }

                    // Получаем точный текст ответа от самого сервера UniOne
                    val responseBody = response.bodyAsText()

                    Log.d(TAG, "=== ОТВЕТ ОТ СЕРВЕРА UniOne ПОЛУЧЕН ===")
                    Log.d(TAG, "Цифровой код ответа: ${response.status.value}")
                    Log.d(TAG, "Текст ответа от UniOne: $responseBody")
                }

            } catch (e: Exception) {
                Log.e(TAG, "КРИТИЧЕСКИЙ СБОЙ СЕТИ: Запрос вообще не смог покинуть телефон", e)
            }
        }.start()
    }
}

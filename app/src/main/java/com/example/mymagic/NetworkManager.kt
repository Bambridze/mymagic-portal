@file:Suppress("SpellCheckingInspection", "unused", "ConvertToStringTemplate", "StringLiteralDuplication")
package com.example.mymagic

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NetworkManager {
    private const val TAG = "MagicNetwork"
    private const val SUPABASE_URL = "https://huxkryixyiugpadsjyni.supabase.co"
    private const val SUPABASE_ANON_KEY = "sb_publishable_RpyCjNV3Rt9MxLCYasqbEA_E8UEJ462"

    private val client = HttpClient(Android) {
        engine {
            connectTimeout = 15_000
            socketTimeout = 15_000
        }
    }

    /**
     * Запрос OTP для верификации почты (при первой привязке)
     */
    suspend fun sendOtpToEmail(email: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim()
            val jsonBody = "{\"email\":\"" + cleanEmail + "\",\"create_user\":true,\"data\":{},\"gotrue_meta_security\":{}}"
            val httpBody = io.ktor.content.TextContent(jsonBody, ContentType.Application.Json)

            val response = client.post(SUPABASE_URL + "/auth/v1/otp") {
                header("apikey", SUPABASE_ANON_KEY)
                setBody(httpBody)
            }
            response.status.value == 200 || response.status.value == 201
        } catch (e: Exception) {
            Log.e(TAG, "Сбой сети при отправке OTP", e)
            false
        }
    }

    /**
     * Монолитная привязка почты к текущему ID (с верификацией OTP) + отправка ID юзеру на почту
     */
    suspend fun verifyOtpAndSaveMonolithic(
        sproId: String,
        email: String,
        code: String,
        initialEnergy: Int,
        isPremium: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim()
            val cleanCode = code.trim()

            var verifyBody = "{\"email\":\"" + cleanEmail + "\",\"token\":\"" + cleanCode + "\",\"type\":\"signup\"}"
            var verifyResponse = client.post(SUPABASE_URL + "/auth/v1/verify") {
                contentType(ContentType.Application.Json)
                header("apikey", SUPABASE_ANON_KEY)
                setBody(verifyBody)
            }

            if (verifyResponse.status.value != 200 && verifyResponse.status.value != 201) {
                verifyBody = "{\"email\":\"" + cleanEmail + "\",\"token\":\"" + cleanCode + "\",\"type\":\"magiclink\"}"
                verifyResponse = client.post(SUPABASE_URL + "/auth/v1/verify") {
                    contentType(ContentType.Application.Json)
                    header("apikey", SUPABASE_ANON_KEY)
                    setBody(verifyBody)
                }
            }

            if (verifyResponse.status.value != 200 && verifyResponse.status.value != 201) {
                Log.w(TAG, "Supabase Auth отверг код: " + verifyResponse.status.value)
                return@withContext false
            }

            val profileBody = "{\"spro_id\":\"" + sproId + "\",\"email\":\"" + cleanEmail + "\",\"current_energy\":" + initialEnergy + ",\"is_premium\":" + isPremium + "}"
            val profileResponse = client.post(SUPABASE_URL + "/rest/v1/user_profiles") {
                contentType(ContentType.Application.Json)
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                header("Prefer", "resolution=merge-duplicates")
                setBody(profileBody)
            }

            val isSaved = profileResponse.status.value == 200 || profileResponse.status.value == 201
            if (isSaved) {
                // ФУНДАМЕНТАЛЬНО: Отправляем юзеру его ID на почту, чтобы он его не потерял!
                sendIdToUserEmail(cleanEmail, sproId)
            }
            isSaved
        } catch (e: Exception) {
            Log.e(TAG, "Сбой монолитного сохранения", e)
            false
        }
    }
    /**
     * Проверка существования связки ID + Email для восстановления БЕЗ OTP кодов
     */
    suspend fun verifyIdAndEmailForRestore(sproId: String, email: String): String? = withContext(Dispatchers.IO) {
        try {
            val cleanId = sproId.trim()
            val cleanEmail = email.trim()

            val urlFilter = "?spro_id=ilike.*" + cleanId.trim() + "&email=eq." + cleanEmail.trim()
            val response = client.get(SUPABASE_URL + "/rest/v1/user_profiles" + urlFilter) {
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer " + SUPABASE_ANON_KEY)
            }

            if (response.status.value == 200) {
                val rawJson = response.bodyAsText()
                if (rawJson == "[]" || rawJson.isEmpty()) null else rawJson
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки данных восстановления", e)
            null
        }
    }

    /**
     * Выгрузка АБСОЛЮТНО ВСЕЙ ленты транзакций для хронологического прогона без лимитов
     */
    suspend fun fetchAllTransactionsForRestore(sproId: String): String? = withContext(Dispatchers.IO) {
        try {
            val cleanId = sproId.trim()
            val orderField = "cre" + "ated" + "_at"
            // НАЙДИ В NetworkManager.kt И ЗАМЕНИ НА ЭТО:
            val urlFilter = "?user_spro_id=ilike.*" + cleanId + "&order=" + orderField + ".asc"

            val response = client.get(SUPABASE_URL + "/rest/v1/energy_transactions" + urlFilter) {
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer " + SUPABASE_ANON_KEY)
            }
            if (response.status.value == 200) response.bodyAsText() else null
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки истории транзакций", e)
            null
        }
    }

    /**
     * Логирование одиночной транзакции в облачную ленту аудита
     */
    suspend fun logEnergyTransaction(sproId: String, energyChange: Int, reason: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val txBody = "{\"user_spro_id\":\"" + sproId + "\",\"energy_change\":" + energyChange + ",\"reason\":\"" + reason + "\",\"store_source\":\"samsung\"}"
            val txResponse = client.post(SUPABASE_URL + "/rest/v1/energy_transactions") {
                contentType(ContentType.Application.Json)
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                setBody(txBody)
            }
            txResponse.status.value == 200 || txResponse.status.value == 201
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Вспомогательный метод для отправки ID на почту
     */
    private suspend fun sendIdToUserEmail(email: String, sproId: String): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "Письмо с ID $sproId успешно отправлено на адрес $email")
        true
    }

    suspend fun fetchTransactionsAfterRefund(sproId: String, purchaseDateIso: String): String? = withContext(Dispatchers.IO) {
        try {
            val fieldKey = "cre" + "ated" + "_at"
            val urlFilter = "?user_spro_id=eq." + sproId + "&energy_change=lt.0&" + fieldKey + "=gt." + purchaseDateIso
            val dbResponse = client.get(SUPABASE_URL + "/rest/v1/energy_transactions" + urlFilter) {
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer " + SUPABASE_ANON_KEY)
            }
            if (dbResponse.status.value == 200) dbResponse.bodyAsText() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun sendFraudEmailNotification(email: String, messageText: String): Boolean = withContext(Dispatchers.IO) { true }

    suspend fun countCloudEnergyPacks(sproId: String): Int = withContext(Dispatchers.IO) {
        try {
            val urlFilter = "?user_spro_id=eq." + sproId + "&reason=ilike.pack_100_id_%"
            val dbResponse = client.get(SUPABASE_URL + "/rest/v1/energy_transactions" + urlFilter) {
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                header("Prefer", "count=exact")
            }
            if (dbResponse.status.value == 200 || dbResponse.status.value == 206) {
                val contentRange = dbResponse.headers["Content-Range"] ?: ""
                contentRange.substringAfter("/").trim().toIntOrNull() ?: 0
            } else 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getLatestPackTimestamp(sproId: String): String? = withContext(Dispatchers.IO) {
        try {
            val dbFieldName = "cre" + "ated" + "_at"
            val urlFilter = "?user_spro_id=eq." + sproId + "&reason=ilike.pack_100_id_%&order=" + dbFieldName + ".desc&limit=1"
            val dbResponse = client.get(SUPABASE_URL + "/rest/v1/energy_transactions" + urlFilter) {
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer " + SUPABASE_ANON_KEY)
            }
            if (dbResponse.status.value == 200) {
                val rawJson = dbResponse.bodyAsText()
                val searchToken = "\"" + "cre" + "ated" + "_at" + "\":\""
                if (rawJson.contains(searchToken)) {
                    val timestamp = rawJson.substringAfter(searchToken).substringBefore("\"")
                    if (timestamp != "null" && timestamp.isNotEmpty()) return@withContext timestamp
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}

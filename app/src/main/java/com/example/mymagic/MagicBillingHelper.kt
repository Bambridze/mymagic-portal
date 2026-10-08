@file:Suppress("SpellCheckingInspection", "unused")
package com.example.mymagic

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.content.edit
import com.samsung.android.sdk.iap.lib.helper.IapHelper
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo
import com.samsung.android.sdk.iap.lib.vo.OwnedProductVo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MagicBillingHelper(
    val onPurchaseSuccess: (String, String) -> Unit
) {
    private var iapHelper: IapHelper? = null

    companion object {
        private const val TAG = "MagicBilling"
        private const val AD_UNIT_ID = "ca-app-pub-6861925892299187/9647732178"

        @Volatile
        private var isSdkInitialized = false
    }

    fun registerLauncher(activity: ComponentActivity) {
        try {
            iapHelper = IapHelper.getInstance(activity.applicationContext)
            Log.d(TAG, "Samsung IAP SDK 6.5.2 успешно инициализирован.")
        } catch (e: Throwable) {
            Log.e(TAG, "Критический сбой при инициализации Samsung IAP", e)
        }
    }

    fun launchPurchase(productId: String) {
        val helper = iapHelper ?: run {
            Log.e(TAG, "Ошибка: IapHelper не готов")
            return
        }

        try {
            Log.d(TAG, "Отправляем запрос startPayment для товара: $productId")

            helper.startPayment(productId, "", "") { errorVo: ErrorVo, purchaseVo: PurchaseVo? ->
                if (errorVo.errorCode == 0 && purchaseVo != null) {
                    Log.d(TAG, "Платеж Самсунга успешен! ID товара: ${purchaseVo.itemId}")
                    val realPaymentId = purchaseVo.paymentId
                    onPurchaseSuccess(purchaseVo.itemId, realPaymentId)
                } else {
                    Log.w(TAG, "Сбой платежа Samsung. Код: ${errorVo.errorCode}, Текст: ${errorVo.errorString}")
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Сбой вызова платежного экрана Samsung Checkout", e)
        }
    }

    /**
     * Официальное потребление расходуемого товара (пака энергии)
     * ИСПРАВЛЕНО: Вызов переведен на Reflection, чтобы обойти баг видимости методов Java SDK в Котлине!
     */
    fun consumePurchasedItem(purchaseId: String) {
        val helper = iapHelper ?: run {
            Log.e(TAG, "Ошибка consumePurchasedItem: IapHelper не готов")
            return
        }

        try {
            Log.d(TAG, "Отправляем команду consumeItem через Reflection для транзакции: $purchaseId")

            // Находим метод consumeItem в классе IapHelper динамически
            val consumeMethod = helper.javaClass.methods.find {
                it.name == "consumeItem" || it.name == "consumePurchasedItem"
            }

            if (consumeMethod != null) {
                // Создаем нативный лямбда-слушатель для Java-колбэка Самсунга
                val listenerClass = consumeMethod.parameterTypes.getOrNull(1)

                if (listenerClass != null) {
                    // Динамический прокси для перехвата ответа стора
                    val proxyListener = java.lang.reflect.Proxy.newProxyInstance(
                        listenerClass.classLoader,
                        arrayOf(listenerClass)
                    ) { _, method, args ->
                        if (method.name.contains("Consume") || method.name.contains("Response")) {
                            val errorVo = args?.getOrNull(0) as? ErrorVo
                            if (errorVo != null && errorVo.errorCode == 0) {
                                Log.d(TAG, "Товар успешно потреблен в сторе Samsung. Слот свободен.")
                            } else {
                                Log.e(TAG, "Сбой потребления товара Самсунга. Код: ${errorVo?.errorCode}")
                            }
                        }
                        null
                    }

                    // Вызываем метод в обход жестких проверок компилятора Котлина!
                    consumeMethod.invoke(helper, purchaseId, proxyListener)
                } else {
                    Log.e(TAG, "Не удалось определить класс слушателя в методе Самсунга.")
                }
            } else {
                Log.e(TAG, "Критическая ошибка: Метод потребления товара не найден в структуре SDK Самсунга.")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Критический сбой Reflection при вызове метода потребления", e)
        }
    }

    /**
     * Проверка owned-листа для контура защиты от фрода
     */
    fun checkOwnedItems(context: Context, prefs: SharedPreferences) {
        val helper = iapHelper ?: return

        try {
            Log.d(TAG, "Запуск проверки возвратов средств через getOwnedList...")

            helper.getOwnedList("item") { errorVo: ErrorVo, ownedList: ArrayList<OwnedProductVo>? ->
                if (errorVo.errorCode == 0 && ownedList != null) {

                    val hasPremiumNow = ownedList.any { it.itemId == "premium_unlocked" || it.itemId == "premium_test" }
                    val wasPremiumBefore = prefs.getBoolean("is_premium_user", false)

                    if (wasPremiumBefore && !hasPremiumNow) {
                        Log.w(TAG, "Обнаружен возврат средств за Премиум! Отключаем.")
                        prefs.edit { putBoolean("is_premium_user", false) }
                    }

                    val sproId = prefs.getString("user_magic_spro_id", "") ?: ""
                    val backupStatus = prefs.getString("email_backup_status", "none") ?: "none"

                    if (backupStatus == "saved" && sproId.isNotEmpty()) {
                        val activePacksOnSamsung = ownedList.count { it.itemId == "energy_pack_100" }

                        CoroutineScope(Dispatchers.Main).launch {
                            val packsInCloudHistory = NetworkManager.countCloudEnergyPacks(sproId)

                            if (packsInCloudHistory > activePacksOnSamsung) {
                                val refundCount = packsInCloudHistory - activePacksOnSamsung
                                Log.w(TAG, "ВНИМАНИЕ: Обнаружен чарджбэк паков! Разница: $refundCount")

                                val latestPackDateIso = NetworkManager.getLatestPackTimestamp(sproId) ?: "1970-01-01T00:00:00Z"

                                if (context is MainActivity) {
                                    context.triggerAntiFraudRollback(latestPackDateIso)
                                }
                            }
                        }
                    } else {
                        val hasEnergyPackNow = ownedList.any { it.itemId == "energy_pack_100" }
                        if (!hasEnergyPackNow && prefs.getBoolean("energy_pack_purchased_flag", false)) {
                            Log.w(TAG, "Локальный откат пака энергии для автономного профиля.")
                            val currentEnergy = prefs.getInt("global_magic_energy", 0)
                            prefs.edit {
                                putInt("global_magic_energy", maxOf(0, currentEnergy - 100))
                                putBoolean("energy_pack_purchased_flag", false)
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Не удалось проверить возвраты или список пуст. Код: ${errorVo.errorCode}")
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Сбой при проверке getOwnedList", e)
        }
    }
}

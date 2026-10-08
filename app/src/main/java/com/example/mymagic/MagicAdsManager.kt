@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MagicAdsManager(private val context: Context) {

    companion object {
        private const val TAG = "MagicAds"
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

        @Volatile
        private var isSdkInitialized = false
    }

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    init {
        if (!isSdkInitialized && checkGooglePlayServicesAvailable()) {
            synchronized(MagicAdsManager::class.java) {
                if (!isSdkInitialized) {
                    try {
                        MobileAds.initialize(context.applicationContext) {
                            isSdkInitialized = true
                            Log.d(TAG, "Google Mobile Ads SDK успешно проинициализирован.")
                            loadRewardedVideo()
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Критический сбой при вызове MobileAds.initialize на этом устройстве", e)
                    }
                }
            }
        } else if (!checkGooglePlayServicesAvailable()) {
            Log.w(TAG, "Инициализация AdMob пропущена: Google Play Services отсутствуют в прошивке устройства.")
        }
    }

    private fun checkGooglePlayServicesAvailable(): Boolean {
        return try {
            Class.forName("com.google.android.gms.common.GoogleApiAvailability")
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun loadRewardedVideo() {
        if (!isSdkInitialized || rewardedAd != null || isLoading) return
        isLoading = true

        try {
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(context, AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Ошибка загрузки рекламы AdMob: ${adError.message}")
                    rewardedAd = null
                    isLoading = false
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Реклама AdMob успешно загружена in кэш.")
                    rewardedAd = ad
                    isLoading = false
                }
            })
        } catch (e: Throwable) {
            Log.e(TAG, "Запрос рекламы отклонен системой", e)
            isLoading = false
        }
    }

    fun showRewardedVideo(activity: Activity, onRewardEarned: () -> Unit) {
        val ad = rewardedAd
        if (ad != null) {
            try {
                ad.show(activity) { rewardItem ->
                    Log.d(TAG, "Пользователь досмотрел video. Получено вознаграждение: ${rewardItem.amount}")

                    onRewardEarned()

                    if (activity is MainActivity) {
                        val prefs = MagicPrefsFactory.create(activity)
                        val backupStatus = prefs.getString("email_backup_status", "none") ?: "none"

                        if (backupStatus == "saved" || backupStatus == "declined") {
                            val sproId = prefs.getString("user_magic_spro_id", "") ?: ""
                            if (sproId.isNotEmpty()) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    // ИСПРАВЛЕНО: Вызов приведен строго к новой чистой сигнатуре без именованных параметров
                                    NetworkManager.logEnergyTransaction(sproId, 1, "ad_reward")
                                }
                            }
                        } else if (backupStatus == "none") {
                            activity.checkEmailBackupOnAdRewarded(prefs) { generatedIdFromLiveMemory ->
                                Log.d(TAG, "Реклама успешно вызвала триггер бэкапа. Временный ID в памяти: $generatedIdFromLiveMemory")
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Ошибка при показе шторки AdMob", e)
            }
            rewardedAd = null
            loadRewardedVideo()
        } else {
            Log.w(TAG, "Реклама ещё не загружена или отсутствует подключение к сети.")
            loadRewardedVideo()
        }
    }
}

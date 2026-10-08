@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log

class MagicTorchController(context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    /**
     * Включает фонарик на строго заданную мощность.
     * level = 0 -> Выключен
     * level = 1 -> Экономный режим (уровень 2 из 5)
     * level = 2 -> Максимальный режим (максимальный уровень устройства)
     */
    fun setTorchStrength(level: Int) {
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return

            // Шаг 1: Если уровень 0 — полностью тушим фонарик
            if (level == 0) {
                cameraManager.setTorchMode(cameraId, false)
                return
            }

            // Шаг 2: Проверяем плавную регулировку яркости для Android 13+ (Tiramisu)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val chars = cameraManager.getCameraCharacteristics(cameraId)
                    val maxLevel = chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL)

                    if (maxLevel != null && maxLevel > 1) {
                        // Если передан уровень 1 — ставим экономный уровень 2.
                        // Если передан уровень 2 (турбо) — выставляем абсолютный максимум устройства.
                        val targetStrength = if (level == 1) 2 else maxLevel

                        cameraManager.turnOnTorchWithStrengthLevel(cameraId, targetStrength)
                        Log.d("MagicFlash", "Фонарик успешно включен через API 13+. Уровень мощности: $targetStrength (Макс: $maxLevel)")
                        return
                    }
                } catch (internalErr: Exception) {
                    Log.w("MagicFlash", "Плавная регулировка яркости не поддерживается чипом, откат на бинарный режим", internalErr)
                }
            }

            // Железный fallback для старых API или если регулировка яркости аппаратно заблокирована
            cameraManager.setTorchMode(cameraId, true)
            Log.d("MagicFlash", "Фонарик включен в стандартном бинарном режиме (ON/OFF)")
        } catch (e: Exception) {
            Log.e("MagicFlash", "Глухой сбой управления фонариком: Torch failed", e)
        }
    }
}

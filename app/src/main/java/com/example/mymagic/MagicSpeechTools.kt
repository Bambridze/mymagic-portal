@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File


class MagicSpeechTools(private val context: Context) {
    var isModelLoaded by mutableStateOf(false)
    private val locManager = MagicLocalizationManager(context)
    var modelStatusText by mutableStateOf(locManager.get("engine_init"))

    private var speechJob = SupervisorJob()
    private var speechScope = CoroutineScope(Dispatchers.Default + speechJob)

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var channelListeningJob: Job? = null

    @Volatile
    private var voskModel: Model? = null

    @Volatile
    private var voskRecognizer: Recognizer? = null
    private var audioChannel = Channel<ShortArray>(Channel.UNLIMITED)
    private var onResultCallback: ((String) -> Unit)? = null

    @Volatile
    private var isUnpackingActive = false

    @Volatile
    private var isChannelListeningActive = false

    private val downloader = MagicModelDownloader(context)
    private val torchController = MagicTorchController(context)

    fun setTorchStrength(level: Int) {
        torchController.setTorchStrength(level)
    }

    fun loadModel() {
        val prefs = MagicPrefsFactory.create(context)
        var lang = prefs.getString("vosk_language", "en") ?: "en"

        Log.d("VOSK_DEBUG", "1. Вызов loadModel(). Язык: $lang")

        synchronized(this) {
            if (isUnpackingActive) {
                Log.d("VOSK_DEBUG", "-> Отмена: Распаковка уже активна!")
                return
            }
            isUnpackingActive = true
        }

        if (!speechScope.isActive || speechJob.isCancelled) {
            Log.d("VOSK_DEBUG", "2. Пересоздание speechScope...")
            speechJob = SupervisorJob()
            speechScope = CoroutineScope(Dispatchers.Default + speechJob)
        }

        speechScope.launch {
            try {
                Log.d("VOSK_DEBUG", "3. Проверяем шторку пермишена микрофона...")
                if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.e("VOSK_DEBUG", "-> Нет разрешения на микрофон! Выход.")
                    return@launch
                }


                withContext(Dispatchers.IO) {
                    var targetFolder = downloader.getTargetDirForLang(lang)
                    var markerFile = File(targetFolder, "model.ready")

                    Log.d("VOSK_DEBUG", "4. Проверяем папку модели: ${targetFolder.name}")
                    if (!downloader.isModelReady(lang)) {
                        Log.d("VOSK_DEBUG", "-> Модель не готова. Запуск копирования...")
                        withContext(Dispatchers.Main) {
                            isModelLoaded = false
                            modelStatusText = locManager.get("engine_download")
                        }

                        synchronized(this@MagicSpeechTools) {
                            try {
                                voskRecognizer?.close()
                                voskModel?.close()
                            } catch (_: Exception) {}
                            voskRecognizer = null
                            voskModel = null
                        }

                        val success = if (lang == "en") {
                            copyAssetFolder("vosk-model-small", targetFolder)
                        } else {
                            downloader.downloadAndUnpackLang(lang)
                        }

                        if (!success) {
                            Log.e("VOSK_DEBUG", "❌ Ошибка копирования файлов модели!")
                            targetFolder.deleteRecursively()
                            withContext(Dispatchers.Main) { modelStatusText = locManager.get("engine_error") }
                            return@withContext
                        }
                        markerFile.createNewFile()
                        Log.d("VOSK_DEBUG", "-> Маркер успеха создан.")
                    }

                    try {
                        synchronized(this@MagicSpeechTools) {
                            try {
                                voskRecognizer?.close()
                                voskModel?.close()
                            } catch (_: Exception) {}
                            voskRecognizer = null
                            voskModel = null

                            Log.d("VOSK_DEBUG", "5. ВХОД В JNI: Создаем Model()...")
                            voskModel = Model(targetFolder.absolutePath)
                            Log.d("VOSK_DEBUG", "6. ВХОД В JNI: Создаем Recognizer()...")
                            voskRecognizer = Recognizer(voskModel, 16000.0f)
                            Log.d("VOSK_DEBUG", "🚀 JNI ИНИЦИАЛИЗАЦИЯ ЗАВЕРШЕНА УСПЕШНО!")
                        }
                    } catch (voskErr: Exception) {
                        Log.e("VOSK_DEBUG", "💥 КРИТИЧЕСКИЙ СБОЙ JNI СЛOЯ!", voskErr)
                        targetFolder.deleteRecursively()
                        synchronized(this@MagicSpeechTools) {
                            try { voskRecognizer?.close(); voskModel?.close() } catch (_: Exception) {}
                            voskRecognizer = null; voskModel = null
                        }
                        lang = "en"
                        prefs.edit { putString("vosk_language", "en") }
                        targetFolder = downloader.getTargetDirForLang("en")
                        markerFile = File(targetFolder, "model.ready")
                        if (!downloader.isModelReady("en")) {
                            copyAssetFolder("vosk-model-small", targetFolder)
                            markerFile.createNewFile()
                        }
                        synchronized(this@MagicSpeechTools) {
                            voskModel = Model(targetFolder.absolutePath)
                            voskRecognizer = Recognizer(voskModel, 16000.0f)
                        }
                    }
                }

                if (voskRecognizer != null) {
                    Log.d("VOSK_DEBUG", "7. Движок готов к работе!")
                    withContext(Dispatchers.Main) {
                        isModelLoaded = true
                        modelStatusText = locManager.get("engine_ready")
                    }
                }
            } catch (err: Exception) {
                Log.e("VOSK_DEBUG", "❌ Глобальный сбой loadModel: ${err.message}")
                withContext(Dispatchers.Main) { modelStatusText = locManager.get("engine_error") }
            } finally {
                synchronized(this@MagicSpeechTools) {
                    isUnpackingActive = false
                    Log.d("VOSK_DEBUG", "8. Флаг распаковки сброшен.")
                }
            }
        }
    }

    private fun startChannelListening() {
        synchronized(this) {
            channelListeningJob?.cancel()
            isChannelListeningActive = true
        }
        audioChannel = Channel(Channel.UNLIMITED)
        channelListeningJob = speechScope.launch(Dispatchers.Default) {
            audioChannel.consumeAsFlow().collect { chunk ->
                if (!isModelLoaded) return@collect
                synchronized(this@MagicSpeechTools) {
                    val recognizer = voskRecognizer ?: return@collect
                    try {
                        val isFinal = recognizer.acceptWaveForm(chunk, chunk.size)
                        val json = if (isFinal) recognizer.result else recognizer.partialResult
                        if (json.isNotEmpty()) onResultCallback?.invoke(json)
                    } catch (ex: Exception) {
                        Log.e("MagicVosk", "JNI inside channel error: ${ex.message}")
                    }
                }
            }
        }
    }

    private fun copyAssetFolder(assetFolder: String, destinationDir: File): Boolean {
        return try {
            val assetsList = context.assets.list(assetFolder) ?: return false
            if (assetsList.isEmpty()) return downloader.copyAssetFile(assetFolder, destinationDir)
            if (!destinationDir.exists()) destinationDir.mkdirs()
            var allCopied = true
            for (assetName in assetsList) {
                val childAssetPath = "$assetFolder/$assetName"
                val childDestinationFile = File(destinationDir, assetName)
                val success = if (context.assets.list(childAssetPath)?.isNotEmpty() == true) {
                    copyAssetFolder(childAssetPath, childDestinationFile)
                } else {
                    downloader.copyAssetFile(childAssetPath, childDestinationFile)
                }
                if (!success) allCopied = false
            }
            allCopied
        } catch (e: Exception) {
            Log.e("MagicVosk", "Asset folder copy error: ${e.message}")
            false
        }
    }
    fun updateGrammar(phrasesJson: String) {
        synchronized(this) {
            val model = voskModel ?: return
            try {
                voskRecognizer?.close()
                voskRecognizer = Recognizer(model, 16000.0f, phrasesJson)
            } catch (ex: Exception) {
                Log.e("MagicVosk", "Grammar update failed: ${ex.message}")
            }
        }
    }

    fun registerListener(listener: (String) -> Unit) {
        onResultCallback = listener
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        stopListening()
        if (!speechScope.isActive || speechJob.isCancelled) {
            speechJob = SupervisorJob()
            speechScope = CoroutineScope(Dispatchers.Default + speechJob)
        }

        startChannelListening()

        recordingJob = speechScope.launch(Dispatchers.IO) {
            val sampleRate = 16000
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)

            if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) return@launch
            val localRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
            if (localRecord.state != AudioRecord.STATE_INITIALIZED) {
                localRecord.release()
                return@launch
            }

            audioRecord = localRecord
            try {
                localRecord.startRecording()
                val readBuffer = ShortArray(bufferSize / 2)
                while (isActive && localRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val readShorts = localRecord.read(readBuffer, 0, readBuffer.size)
                    if (readShorts > 0 && isActive) {
                        val chunk = ShortArray(readShorts)
                        System.arraycopy(readBuffer, 0, chunk, 0, readShorts)
                        if (isChannelListeningActive) {
                            audioChannel.send(chunk)
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.e("MagicVosk", "Mic loop error: ${ex.message}")
            } finally {
                withContext(NonCancellable) {
                    try {
                        localRecord.stop()
                    } catch (_: Exception) {}
                    try {
                        localRecord.release()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun stopListening() {
        runBlocking {
            try {
                recordingJob?.cancelAndJoin()
            } catch (_: Exception) {}
        }
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    fun shutdownEngine() {
        stopListening()
        synchronized(this) {
            isChannelListeningActive = false
            channelListeningJob?.cancel()
            channelListeningJob = null
            try {
                voskRecognizer?.close()
                voskModel?.close()
            } catch (_: Exception) {}
            voskRecognizer = null
            voskModel = null
            isModelLoaded = false
        }
        audioChannel.close()
        speechJob.cancel()
        modelStatusText = locManager.get("engine_sleep")
    }
}

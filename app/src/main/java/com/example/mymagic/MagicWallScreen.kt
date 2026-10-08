@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MagicWallScreen(
    wallState: String,
    zoomLevel: Int,
    isSleepingExternal: Boolean,
    speechTools: MagicSpeechTools, // ПРИНЯЛИ: Провод от речевого движка для контроля кругляша
    onWakeUpRequested: () -> Unit,
    onBypassClicked: () -> Unit,
    onVideoFinished: () -> Unit,
    locManager: MagicLocalizationManager
) {
    val context = LocalContext.current
    // Используем наш оптимизированный скоростной синглтон SharedPreferences
    val prefs = remember { MagicPrefsFactory.create(context) }

    val customLoopVideo = prefs.getString("custom_video_file_wall_loop", "") ?: ""
    val customBreakVideo = prefs.getString("custom_video_file_wall_break", "") ?: ""

    val isLocked = wallState == "locked"
    val neonGreen = Color(0xFF00FF66)

    // Глухой чёрный бэкграунд на самом нижнем уровне спасает от тёмных прорех при прогреве ExoPlayer
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isSleepingExternal) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF05050A)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = locManager.get("w_s"), color = Color.DarkGray, fontSize = 13.sp)
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(0.05f)).clickable { onWakeUpRequested() }.padding(12.dp), contentAlignment = Alignment.Center) {
                        Text(text = "👁️", fontSize = 20.sp, color = Color.DarkGray)
                    }
                }
            }
        } else {
            // Весь видео-агрегат обёрнут в один контейнер с общим масштабом.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(if (zoomLevel == 2) 1.25f else 1.0f)
            ) {
                // СЛОЙ 1 (НИЖНИЙ): Петля шлюза
                VideoBackground(
                    videoRawId = R.raw.wall_loop,
                    localFileName = customLoopVideo,
                    zoomLevel = 1,
                    spellId = "wall_loop",
                    isWallLoop = true,
                    isMutedByDefault = true,
                    onVideoEnd = {}
                )

                // СЛОЙ 2 (ВЕРХНИЙ): Прорыв стены
                if (!isLocked) {
                    VideoBackground(
                        videoRawId = R.raw.wall_break,
                        localFileName = customBreakVideo,
                        zoomLevel = 1,
                        spellId = "wall_break",
                        isWallLoop = false,
                        isMutedByDefault = false,
                        onVideoEnd = onVideoFinished
                    )
                }
            }
        }

        // БЕЗОПАСНЫЙ И КРАСИВЫЙ UX: Аккуратный кругляш загрузки по центру экрана.
        // Горит ровно до тех пор, пока нативный C++ Vosk не проинициализирует микрофон.
        if (!speechTools.isModelLoaded && !isSleepingExternal) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.4f)), // Легкое каноничное затемнение
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = neonGreen,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        if (isLocked && !isSleepingExternal) {
            Box(modifier = Modifier.align(Alignment.TopStart).padding(16.dp).size(48.dp).clickable {
                (context as? MainActivity)?.showSystemUnlock(locManager) { onBypassClicked() }
            }) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 3.dp.toPx()
                    val tintColor = Color.Gray.copy(alpha = 0.5f)

                    val framePath = Path().apply {
                        moveTo(size.width * 0.2f, size.height * 0.85f)
                        lineTo(size.width * 0.2f, size.height * 0.15f)
                        lineTo(size.width * 0.65f, size.height * 0.15f)
                        lineTo(size.width * 0.65f, size.height * 0.85f)
                    }
                    drawPath(path = framePath, color = tintColor, style = Stroke(width = strokeWidth))

                    val doorPath = Path().apply {
                        moveTo(size.width * 0.2f, size.height * 0.15f)
                        lineTo(size.width * 0.5f, size.height * 0.05f)
                        lineTo(size.width * 0.5f, size.height * 0.75f)
                        lineTo(size.width * 0.2f, size.height * 0.85f)
                        close()
                    }
                    drawPath(path = doorPath, color = tintColor, style = Stroke(width = strokeWidth))
                }
            }
        }
    }
}

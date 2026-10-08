@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MagicHubScreen(
    isCombatModeExternal: Boolean,
    onCombatModeChanged: (Boolean) -> Unit,
    isDebugHudExternal: Boolean,
    onDebugHudChanged: (Boolean) -> Unit,
    activeSpellId: String = "",
    partialSpeechText: String = "",
    onVideoFinished: () -> Unit = {},
    speechTools: MagicSpeechTools,
    locManager: MagicLocalizationManager,
    onPurchaseRequested: (String) -> Unit
) {
    var showSettingsMenu by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val prefs = remember { MagicPrefsFactory.create(ctx) }

    val customHubVideo = prefs.getString("custom_video_file_hub_main", "") ?: ""
    val isTorchSpell = activeSpellId == "light_on" || activeSpellId == "light_max" || activeSpellId == "light_off"
    val hasActiveCombatCast = isCombatModeExternal && activeSpellId.isNotEmpty() && !isTorchSpell

    val playLocalFile = if (hasActiveCombatCast) {
        val spell = magicSpellRegistry.firstOrNull { it.id == activeSpellId }
        if (spell != null) {
            prefs.getString("custom_video_file_${spell.id}", spell.videoFileName) ?: spell.videoFileName
        } else ""
    } else {
        if (!isCombatModeExternal) customHubVideo else ""
    }

    val playZoom = if (hasActiveCombatCast) {
        val spell = magicSpellRegistry.firstOrNull { it.id == activeSpellId }
        if (spell != null) prefs.getInt("zoom_level_${spell.id}", 1) else 1
    } else {
        prefs.getInt("zoom_level_hub", 1)
    }

    val playSpellId = if (hasActiveCombatCast) activeSpellId else "hub_main"

    LaunchedEffect(playZoom) {
        Log.d("MAGIC_ZOOM", "Hub Scale Redrawn. Active Zoom: $playZoom")
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.fillMaxSize().scale(if (playZoom == 2) 1.25f else 1.0f)) {
            if (!isCombatModeExternal && customHubVideo.isEmpty()) {
                Image(painter = painterResource(id = R.drawable.hall), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                key(playSpellId, playLocalFile) {
                    VideoBackground(videoRawId = 0, localFileName = playLocalFile, zoomLevel = playZoom, spellId = playSpellId, isWallLoop = !hasActiveCombatCast, isMutedByDefault = false, onVideoEnd = onVideoFinished)
                }
            }
        }

        if (!isCombatModeExternal) {
            Text(text = "⚙️", fontSize = 24.sp, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).clickable { showSettingsMenu = true }.padding(8.dp))
        }

        Box(modifier = Modifier.align(Alignment.TopStart).padding(16.dp).clickable { onCombatModeChanged(!isCombatModeExternal) }.padding(8.dp).size(36.dp).drawWithContent {
            drawContent()
            val glowColor = if (isCombatModeExternal) Color(0xFFBF40BF) else Color.Gray.copy(0.2f)
            drawCircle(color = glowColor, radius = 10f, center = Offset(11f, 6f))
            drawRoundRect(brush = Brush.linearGradient(listOf(Color(0xFFE5E5E5), Color(0xFF999999))), topLeft = Offset(4f, 26f), size = Size(8f, 26f), cornerRadius = CornerRadius(4f, 4f))
            drawRoundRect(brush = Brush.linearGradient(listOf(Color(0xFFCCCCCC), Color(0xFF666666))), topLeft = Offset(8f, 6f), size = Size(6f, 22f), cornerRadius = CornerRadius(2f, 2f))
        })

        if (isCombatModeExternal && isDebugHudExternal) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 24.dp)) {
                AnimatedVisibility(visible = hasActiveCombatCast, enter = fadeIn(), exit = fadeOut()) {
                    val currentSpell = magicSpellRegistry.firstOrNull { it.id == activeSpellId }
                    val indicatorColor = currentSpell?.defaultColor ?: Color.Transparent
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(indicatorColor).drawWithContent {
                        drawContent()
                        drawCircle(color = indicatorColor.copy(alpha = 0.4f), radius = size.minDimension * 0.8f, center = center)
                    })
                }
            }
            if (partialSpeechText.isNotEmpty()) {
                Text(text = "${locManager.get("d_h_h")}\"$partialSpeechText\"", color = Color.Yellow, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).background(Color.Black.copy(0.7f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }

        MagicHubSettingsOverlay(
            visible = showSettingsMenu,
            isDebugHudExternal = isDebugHudExternal,
            onDebugHudChanged = onDebugHudChanged,
            onClose = { showSettingsMenu = false },
            speechTools = speechTools,
            locManager = locManager,
            onPurchaseRequested = onPurchaseRequested
        )
    }
}

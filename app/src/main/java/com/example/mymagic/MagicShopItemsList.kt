@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MagicShopItemsList(
    context: Context,
    category: String,
    baseUrl: String,
    prefs: SharedPreferences,
    cherryCherry: Color,
    customBlue: Color,
    darkGreen: Color,
    onActionClick: (String, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val downloader = remember(context) { MagicShopDownloader(context) }

    val downloadStates = remember { mutableStateMapOf<String, String>() }
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val magicFolder = File(downloadsDir, "MyMagicMedia")

    fun startDirectDownload(fileUrl: String, fileName: String, fileKey: String) {
        downloadStates[fileKey] = "loading"
        scope.launch {
            val success = downloader.downloadFileDirectly(fileUrl, fileName)
            if (success) {
                downloadStates[fileKey] = "success"
                delay(500.milliseconds)
                downloadStates.remove(fileKey)
            } else {
                downloadStates.remove(fileKey)
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (category == "entry") {
            (1..10).forEach { i ->
                val buyKey = "buy_entry_$i"
                val isPaid = prefs.getBoolean(buyKey, false)
                val loopName = "wall_loop_$i.mp4"
                val breakName = "wall_break_$i.mp4"
                val loopUrl = "$baseUrl/$loopName"
                val breakUrl = "$baseUrl/$breakName"

                val isLoopDownloaded = File(magicFolder, loopName).exists()
                val isBreakDownloaded = File(magicFolder, breakName).exists()

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$i.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.width(20.dp))
                    if (!isPaid) {
                        Button(
                            onClick = { onActionClick(buyKey, loopUrl) },
                            colors = ButtonDefaults.buttonColors(cherryCherry),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("10 ⚡", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        val stateLoop = downloadStates["${buyKey}_loop"] ?: "none"
                        Button(
                            onClick = { if (stateLoop == "none") startDirectDownload(loopUrl, loopName, "${buyKey}_loop") },
                            colors = ButtonDefaults.buttonColors(if (isLoopDownloaded) darkGreen else customBlue),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp),
                            enabled = stateLoop != "loading"
                        ) {
                            when (stateLoop) {
                                "loading" -> CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                "success" -> Text("✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Green)
                                else -> Text(if (isLoopDownloaded) "I 📁 ⬇️" else "I ⬇️", fontSize = 11.sp, color = Color.White)
                            }
                        }

                        val stateBreak = downloadStates["${buyKey}_break"] ?: "none"
                        Button(
                            onClick = { if (stateBreak == "none") startDirectDownload(breakUrl, breakName, "${buyKey}_break") },
                            colors = ButtonDefaults.buttonColors(if (isBreakDownloaded) darkGreen else customBlue),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp),
                            enabled = stateBreak != "loading"
                        ) {
                            when (stateBreak) {
                                "loading" -> CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                "success" -> Text("✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Green)
                                else -> Text(if (isBreakDownloaded) "II 📁 ⬇️" else "II ⬇️", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        if (category == "hall" || category == "spells") {
            (1..10).forEach { i ->
                val buyKey = "buy_${category}_$i"
                val isPaid = prefs.getBoolean(buyKey, false)
                val fName = if (category == "hall") "hall_$i.mp4" else "spell_$i.mp4"
                val fileUrl = "$baseUrl/$fName"
                val isDownloaded = File(magicFolder, fName).exists()

                val state = downloadStates[buyKey] ?: "none"

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$i.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.width(20.dp))
                    Button(
                        onClick = {
                            if (!isPaid) {
                                onActionClick(buyKey, fileUrl)
                            } else if (state == "none") {
                                startDirectDownload(fileUrl, fName, buyKey)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isPaid) cherryCherry else if (isDownloaded) darkGreen else customBlue
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp),
                        enabled = state != "loading"
                    ) {
                        when (state) {
                            "loading" -> CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            "success" -> Text("✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Green)
                            else -> {
                                Text(
                                    text = if (!isPaid) "10 ⚡" else if (isDownloaded) "📁 ⬇️" else "⬇️",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } } } } } }

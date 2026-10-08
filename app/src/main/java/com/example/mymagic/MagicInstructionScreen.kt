@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MagicInstructionScreen(
    currentPassword: String,
    locManager: MagicLocalizationManager,
    onRequestMicPermission: () -> Unit
) {
    val neonGreen = Color(0xFF00FF66)
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .border(2.dp, neonGreen, RoundedCornerShape(12.dp))
                    .background(neonGreen.copy(0.05f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = locManager.get("i_t"),
                    color = neonGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
            Column(
                modifier = Modifier
                    .border(2.dp, Color.Cyan.copy(0.4f), RoundedCornerShape(12.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = locManager.get("i_d"),
                    color = Color.White,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Text(
                    text = "\"$currentPassword\"",
                    color = Color.Yellow,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                Text(
                    text = locManager.get("i_f"),
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
            Button(
                onClick = onRequestMicPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan),
                modifier = Modifier.width(60.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = locManager.get("b_o"),
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }
    }
}



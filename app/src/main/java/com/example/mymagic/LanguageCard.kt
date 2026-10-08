@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LanguageItem(val code: String, val title: String, val subtitle: String, val flag: String)

@Composable
fun LanguageCard(
    language: LanguageItem,
    isErrorFlash: Boolean,
    isHintFlash: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scaleByProgress by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "ScaleAnimation"
    )

    val flashColor by animateColorAsState(
        targetValue = when {
            isErrorFlash -> Color(0xFF8B0000)
            isHintFlash -> Color(0xFF1B4D22)
            isPressed && enabled -> Color(0xFF00FF66).copy(alpha = 0.15f)
            else -> Color.Black.copy(0.8f)
        },
        animationSpec = tween(durationMillis = 400),
        label = "NeonFlashColor"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(65.dp)
            .scale(scaleByProgress)
            .clip(RoundedCornerShape(16.dp))
            .background(flashColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .border(
                width = if (isPressed && enabled) 3.dp else 2.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        if (isErrorFlash) Color.Red else Color.Cyan,
                        if (isHintFlash) Color.Green else Color(0xFF00FF66)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(text = language.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = language.subtitle, color = Color.Gray, fontSize = 11.sp)
            }
            Text(text = language.flag, fontSize = 26.sp)
        }
    }
}



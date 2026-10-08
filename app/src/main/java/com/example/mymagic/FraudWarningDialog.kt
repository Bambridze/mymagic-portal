@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun FraudWarningDialog(
    visible: Boolean,
    locManager: MagicLocalizationManager,
    onDismissRequested: () -> Unit
) {
    if (!visible) return

    val strictDarkRed = Color(0xFF8B0000) // Строгий темно-вишневый цвет рамки
    val darkBg = Color(0xFF12121A)

    Dialog(
        onDismissRequest = { onDismissRequested() },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .border(2.dp, strictDarkRed, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = darkBg),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Заголовок: "Attention"
                Text(
                    text = locManager.get("fraud_title").uppercase(),
                    color = Color(0xFFFF4D4D), // Красный маркер предупреждения
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                // Твой точный текст из ТЗ на английском языке из локализации
                Text(
                    text = locManager.get("fraud_message"),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Намертво захардкоженная кнопка OK, понятная без всяких переводов
                Button(
                    onClick = { onDismissRequested() },
                    colors = ButtonDefaults.buttonColors(containerColor = strictDarkRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(0.5f)
                ) {
                    Text(
                        text = "OK",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}



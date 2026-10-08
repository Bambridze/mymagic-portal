@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri

@Composable
fun MagicShopCategorySelector(
    context: Context,
    category: String,
    telegramChannelUrl: String,
    neonGreen: Color,
    darkGreen: Color,
    locManager: MagicLocalizationManager,
    onCategoryChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Карточка ссылки на Telegram-канал
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            telegramChannelUrl.toUri()
                        )
                    )
                }
                .border(2.dp, Color(0xFF24A1DE), RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF17212B))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("👁️", fontSize = 28.sp)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Ряд кнопок переключения категорий
        // Ряд кнопок переключения категорий
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "entry" to "m_e_t",
                "hall" to "m_h_t",
                "spells" to "sh_cat_spells"
            ).forEach { (tag, key) ->
                val isSelected = category == tag

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) neonGreen else darkGreen)
                        .clickable { onCategoryChange(if (isSelected) "" else tag) }
                        .padding(horizontal = 4.dp, vertical = 2.dp), // Защитные отступы
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = locManager.get(key),
                        fontSize = 10.sp, // Оптимальный размер для двух строчек
                        color = if (isSelected) Color.Black else Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 3,      // ТЕПЕРЬ МОЖНО В 2 СТРОЧКИ!
                        softWrap = true    // Разрешаем перенос слов
                    )
                }
            }
        }
    }
}

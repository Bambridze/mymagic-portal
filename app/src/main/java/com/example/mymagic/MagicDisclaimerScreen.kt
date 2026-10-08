@file:Suppress("SpellCheckingInspection", "DEPRECATION")
package com.example.mymagic

import android.content.Intent
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import java.util.Locale

@Composable
fun MagicDisclaimerScreen(
    locManager: MagicLocalizationManager,
    onBackToLanguage: () -> Unit,
    onAccepted: () -> Unit
) {
    var isInstructionRead by remember { mutableStateOf(false) }
    var isAgreed by remember { mutableStateOf(false) }
    var isRejected by remember { mutableStateOf(false) }

    val isActionEnabled = (isInstructionRead && isAgreed) || isRejected

    val textScrollState = rememberScrollState()
    val context = LocalContext.current
    val neonGreen = Color(0xFF00FF66)
    val darkGreenBtn = Color(0xFF1B4D22)
    val rejectRed = Color(0xFFFF4D4D)

    val legalBaseUrl = "https://sproduction.it.com/legal.html"
    val sysLang = Locale.getDefault().language
    val langQuery = "?lang=$sysLang"

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, neonGreen, RoundedCornerShape(12.dp))
                .background(neonGreen.copy(0.05f), RoundedCornerShape(12.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = locManager.get("d_w_t"),
                color = neonGreen,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Black,
                lineHeight = 20.sp
            )

            Button(
                onClick = {
                    val instrUrl = "https://sproduction.it.com/instruction.html"
                    context.startActivity(Intent(Intent.ACTION_VIEW, instrUrl.toUri()))
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(darkGreenBtn),
                modifier = Modifier.fillMaxWidth(0.95f).height(40.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Text(
                    text = locManager.get("d_instr_btn").uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isInstructionRead = !isInstructionRead },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isInstructionRead,
                    onCheckedChange = { isInstructionRead = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = neonGreen,
                        uncheckedColor = neonGreen.copy(0.6f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = locManager.get("d_c_instr"),
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(2.dp, neonGreen, RoundedCornerShape(12.dp))
                .background(neonGreen.copy(0.05f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Text(
                text = locManager.get("d_l_t"),
                color = neonGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(textScrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = locManager.get("d_a_l"), color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
                    Text(text = locManager.get("d_p"), color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
                    Text(text = locManager.get("d_s"), color = Color.Yellow, fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = neonGreen.copy(0.3f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val tosUrl = "$legalBaseUrl$langQuery#tos"
                        context.startActivity(Intent(Intent.ACTION_VIEW, tosUrl.toUri()))
                    },
                    colors = ButtonDefaults.buttonColors(darkGreenBtn),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(
                        text = locManager.get("d_tos_btn"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
                Button(
                    onClick = {
                        val privacyUrl = "$legalBaseUrl$langQuery#privacy"
                        context.startActivity(Intent(Intent.ACTION_VIEW, privacyUrl.toUri()))
                    },
                    colors = ButtonDefaults.buttonColors(darkGreenBtn),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(
                        text = locManager.get("d_priv_btn"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isAgreed = !isAgreed
                        if (isAgreed) isRejected = false
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isAgreed,
                    onCheckedChange = {
                        isAgreed = it
                        if (it) isRejected = false
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = neonGreen,
                        uncheckedColor = neonGreen.copy(0.6f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = locManager.get("d_c_legal"), color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.weight(1f), lineHeight = 14.sp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isRejected = !isRejected
                        if (isRejected) isAgreed = false
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isRejected,
                    onCheckedChange = {
                        isRejected = it
                        if (it) isAgreed = false
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = rejectRed,
                        uncheckedColor = rejectRed.copy(0.6f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = locManager.get("d_c_not_legal"), color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.weight(1f), lineHeight = 14.sp)
            }
        }

        val activity = (context as? android.app.Activity)

        val buttonText = when {
            isAgreed -> locManager.get("d_b_e")
            isRejected -> locManager.get("d_b_reject")
            else -> ""
        }

        val buttonContainerColor = when {
            isAgreed -> neonGreen
            isRejected -> Color(0xFFE53935).copy(alpha = 0.30f)
            else -> Color.DarkGray.copy(0.4f)
        }

        val buttonTextColor = when {
            isAgreed -> Color.Black
            isRejected -> rejectRed
            else -> Color.Gray
        }

        Button(
            onClick = {
                if (isActionEnabled) {
                    if (isAgreed) onAccepted()
                    if (isRejected) activity?.finish()
                }
            },
            enabled = isActionEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonContainerColor,
                disabledContainerColor = Color.DarkGray.copy(0.4f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(
                text = buttonText,
                fontWeight = FontWeight.Black,
                color = buttonTextColor,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        val interactionSourceArrow = remember { MutableInteractionSource() }
        val isPressedArrow by interactionSourceArrow.collectIsPressedAsState()

        val scaleByProgress by animateFloatAsState(
            targetValue = if (isPressedArrow) 0.90f else 1f,
            animationSpec = tween(durationMillis = 80),
            label = "ArrowScale"
        )

        val arrowShape = remember {
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density
                ): Outline {
                    val path = Path().apply {
                        moveTo(0f, size.height / 2f)
                        lineTo(size.width, 0f)
                        lineTo(size.width * 0.75f, size.height / 2f)
                        lineTo(size.width, size.height)
                        close()
                    }
                    return Outline.Generic(path)
                }
            }
        }

        Box(
            modifier = Modifier
                .width(70.dp)
                .height(44.dp)
                .scale(scaleByProgress)
                .clip(arrowShape)
                .background(neonGreen.copy(0.20f))
                .border(width = 3.dp, color = neonGreen, shape = arrowShape)
                .clickable(
                    interactionSource = interactionSourceArrow,
                    indication = null,
                    onClick = onBackToLanguage
                )
        )
    }
}



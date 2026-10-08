@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import android.app.Activity
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit

@Composable
fun MagicEnergyBillingCard(
    magicEnergy: Int,
    isPremium: Boolean,
    prefs: SharedPreferences,
    locManager: MagicLocalizationManager,
    customBlue: Color,
    onEnergyUpdate: (Int) -> Unit,
    onToastRequested: (String) -> Unit = {},
    onPurchaseRequested: (String) -> Unit
) {
    val context = LocalContext.current

    val activity = remember(context) {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) break
            ctx = ctx.baseContext
        }
        ctx as? Activity
    }

    val adsManager = remember(context) {
        MagicAdsManager(context = context)
    }

    LaunchedEffect(Unit) {
        adsManager.loadRewardedVideo()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, customBlue.copy(0.4f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24).copy(0.6f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isPremium) {
                Button(
                    onClick = { onPurchaseRequested("premium_unlocked") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = locManager.get("b_b_p"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Button(
                onClick = { onPurchaseRequested("energy_pack_100") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF900C3F)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = locManager.get("b_buy_energy"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Button(
                onClick = {
                    activity?.let { currentActivity ->
                        adsManager.showRewardedVideo(currentActivity) {
                            val currentEnergy = prefs.getInt("global_magic_energy", 0)
                            val newEnergy = currentEnergy + 1
                            onEnergyUpdate(newEnergy)
                            prefs.edit { putInt("global_magic_energy", newEnergy) }
                            onToastRequested("+1 ⚡")
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = customBlue),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = locManager.get("b_w_a"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "⚡ $magicEnergy",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

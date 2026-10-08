@file:Suppress("SpellCheckingInspection")
package com.example.mymagic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MagicLanguageSelectionScreen(
    state: MagicNavigationState,
    locManager: MagicLocalizationManager,
    onLanguageSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = locManager.get("m_select_lang_title"),
            color = Color(0xFF00FF66),
            modifier = Modifier.padding(bottom = 8.dp),
            fontSize = 16.sp
        )
        fullInterfaceLanguagesList.forEach { lang ->
            LanguageCard(
                language = lang,
                isErrorFlash = (state.failedLangCode == lang.code),
                isHintFlash = (state.showEnglishHint && lang.code == "en"),
                enabled = !locManager.isDownloading && state.failedLangCode.isEmpty(),
                onClick = {
                    state.scope.launch {
                        val res = locManager.fetchLanguagePack(lang.code)
                        if (res == MagicLocalResult.SUCCESS || res == MagicLocalResult.CACHE_USED) {
                            state.selectedLanguage = lang.code
                            onLanguageSelected()
                        } else if (res == MagicLocalResult.NETWORK_ERROR) {
                            state.failedLangCode = lang.code
                            state.showEnglishHint = true
                            delay(800.milliseconds)
                            state.failedLangCode = ""
                            state.showEnglishHint = false
                        }
                    }
                }
            )
        }
    }
}



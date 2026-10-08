@file:Suppress("SpellCheckingInspection", "unused")
package com.example.mymagic
import androidx.compose.ui.graphics.Color

enum class SpellEffectType { NONE, LIGHT_BURST, COMBAT_STRIKE, COMBAT_SHIELD, COMBAT_FREEZE, COMBAT_DISARM }

data class MagicSpell(val id: String, val voiceCommand: String, val defaultName: String, val defaultColor: Color, val effectType: SpellEffectType, val videoFileName: String, val soundFileName: String)

object MagicSecretBypass { const val WALL_PASSWORD = "i am a tired traveler" }

val magicSpellRegistry = listOf(
    MagicSpell("light_on", "illumination", "Turn On Light", Color.Yellow, SpellEffectType.LIGHT_BURST, "", ""),
    MagicSpell("light_max", "maximum light", "Maximum Light", Color.Yellow, SpellEffectType.LIGHT_BURST, "", ""),
    MagicSpell("light_off", "light off", "Turn Off Light", Color.Gray, SpellEffectType.NONE, "", ""),
    MagicSpell("ignite_burst", "fire burst", "Strike", Color.Red, SpellEffectType.COMBAT_STRIKE, "strike.mp4", "strike.mp3"),
    MagicSpell("defendo_shield", "shield active", "Shield", Color.Cyan, SpellEffectType.COMBAT_SHIELD, "shield.mp4", "shield.mp3"),
    MagicSpell("frizio_freeze", "ice freeze", "Freeze", Color(0xFF5D8AA8), SpellEffectType.COMBAT_FREEZE, "freeze.mp4", "freeze.mp3"),
    MagicSpell("repulso_disarm", "weapon disarm", "Disarm", Color.Magenta, SpellEffectType.COMBAT_DISARM, "disarm.mp4", "disarm.mp3"),
    MagicSpell("mortis_fatal", "mortis fatal", "Forbidden Curse", Color(0xFF00FF66), SpellEffectType.COMBAT_STRIKE, "fatal.mp4", "fatal.mp3")
)

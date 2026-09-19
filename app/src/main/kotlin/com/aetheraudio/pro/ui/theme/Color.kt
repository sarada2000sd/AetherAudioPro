package com.aetheraudio.pro.ui.theme

import androidx.compose.ui.graphics.Color

// AetherAudio Pro — restrained "liquid glass" palette. Panels use low-alpha dark fills with a
// hairline light border rather than heavy blur everywhere, per the UI/UX brief: controls must
// stay the visual priority, not the glass effect.

val ObsidianBg = Color(0xFF0B0F17)
val ObsidianBgTrueBlack = Color(0xFF000000)
val CrystalLightBg = Color(0xFFF4F5F9)

val GlassPanelDark = Color(0xA60F172A)     // rgba(15,23,42,0.65) per spec 2.1
val GlassBorderDark = Color(0x1FFFFFFF)    // rgba(255,255,255,0.12)
val GlassPanelLight = Color(0xCCFFFFFF)
val GlassBorderLight = Color(0x1A000000)

val AccentViolet = Color(0xFF8B5CF6)
val AccentCyan = Color(0xFF22D3EE)
val AccentAmber = Color(0xFFF59E0B)

val TextPrimaryDark = Color(0xFFF5F6FA)
val TextSecondaryDark = Color(0xFFA0A7B8)
val TextPrimaryLight = Color(0xFF13131A)
val TextSecondaryLight = Color(0xFF5B5F6B)

val WaveformInactive = Color(0xFF394152)
val WaveformActiveDefault = AccentViolet

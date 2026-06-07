package com.example.cofredesegredos.ui.theme

import androidx.compose.ui.graphics.Color

// ===== Brand / semantic tokens (light) =====
val Background       = Color(0xFFF8F7F4) // warm linen
val Surface          = Color(0xFFFFFFFF)
val Foreground       = Color(0xFF1A1C1E) // ink — primary text + dark card
val OnForeground     = Color(0xFFF8F7F4) // text on the ink card
val Muted            = Color(0xFFEEEAE3)
val MutedForeground  = Color(0xFF76757A)
val Border           = Color(0xFFE9E4DE)
val Gold             = Color(0xFFC5A059) // brand accent
val GoldSoft         = Color(0xFFF1E6CB)
val Destructive      = Color(0xFFC8392B)
val Success          = Color(0xFF3A7D5C)

// ===== Dark mode =====
val BackgroundDark        = Color(0xFF101013)
val SurfaceDark           = Color(0xFF1A1B1F)
val ForegroundDark        = Color(0xFFF4F2EE)
val MutedDark             = Color(0xFF2A2B2F)
val MutedForegroundDark   = Color(0xFF8C8B91)
val BorderDark            = Color(0x14FFFFFF)

// ===== Vault accent palette (bg + fg pairs) =====
data class VaultTone(val bg: Color, val fg: Color)
val VaultBlue    = VaultTone(Color(0xFFE8F1FE), Color(0xFF2563EB))
val VaultPurple  = VaultTone(Color(0xFFF3ECFE), Color(0xFF7C3AED))
val VaultAmber   = VaultTone(Color(0xFFFEF3C7), Color(0xFFD97706))
val VaultEmerald = VaultTone(Color(0xFFE6F8EE), Color(0xFF059669))
val VaultRose    = VaultTone(Color(0xFFFEE7EC), Color(0xFFE11D48))
val VaultTeal    = VaultTone(Color(0xFFDEF7F4), Color(0xFF0D9488))

fun vaultToneByKey(key: String): VaultTone = when (key) {
    "purple"  -> VaultPurple
    "amber"   -> VaultAmber
    "emerald" -> VaultEmerald
    "rose"    -> VaultRose
    "teal"    -> VaultTeal
    else      -> VaultBlue
}

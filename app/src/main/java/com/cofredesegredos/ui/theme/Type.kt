package com.cofredesegredos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cofredesegredos.R

val Outfit = FontFamily(
    Font(R.font.outfit_variable, FontWeight.Normal),
    Font(R.font.outfit_variable, FontWeight.Medium),
    Font(R.font.outfit_variable, FontWeight.SemiBold),
    Font(R.font.outfit_variable, FontWeight.Bold),
)

val Inter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Medium),
    Font(R.font.inter_variable, FontWeight.SemiBold),
)

val Mono = FontFamily(
    Font(R.font.jetbrains_mono_variable, FontWeight.Medium),
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 32.sp),
    displayMedium = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, lineHeight = 12.sp),
)

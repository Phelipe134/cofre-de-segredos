package com.example.cofredesegredos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.cofredesegredos.R

// Google Fonts provider — declare provider once.
// Add this XML at res/values/font_certs.xml from the Compose docs:
// https://developer.android.com/develop/ui/compose/text/fonts
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val OutfitFont = GoogleFont("Outfit")
private val InterFont  = GoogleFont("Inter")
private val MonoFont   = GoogleFont("JetBrains Mono")

val Outfit = FontFamily(
    Font(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.Bold),
)

val Inter = FontFamily(
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.SemiBold),
)

val Mono = FontFamily(
    Font(googleFont = MonoFont, fontProvider = provider, weight = FontWeight.Medium),
)

val AppTypography = Typography(
    // display = headlines, page titles
    displayLarge   = TextStyle(Outfit,  FontWeight.SemiBold, 30.sp, lineHeight = 32.sp),
    displayMedium  = TextStyle(Outfit,  FontWeight.SemiBold, 24.sp, lineHeight = 26.sp),
    titleLarge     = TextStyle(Outfit,  FontWeight.Medium,   20.sp, lineHeight = 24.sp),
    titleMedium    = TextStyle(Outfit,  FontWeight.Medium,   18.sp, lineHeight = 22.sp),

    // body / labels = Inter
    bodyLarge      = TextStyle(Inter, FontWeight.Medium, 16.sp, lineHeight = 22.sp),
    bodyMedium     = TextStyle(Inter, FontWeight.Normal, 14.sp, lineHeight = 20.sp),
    bodySmall      = TextStyle(Inter, FontWeight.Normal, 12.sp, lineHeight = 16.sp),

    labelLarge     = TextStyle(Inter, FontWeight.SemiBold, 14.sp, lineHeight = 18.sp),
    labelMedium    = TextStyle(Inter, FontWeight.SemiBold, 12.sp, lineHeight = 16.sp),
    // eyebrow uses labelSmall with letterSpacing applied at call site
    labelSmall     = TextStyle(Inter, FontWeight.SemiBold, 10.sp, lineHeight = 12.sp),
)

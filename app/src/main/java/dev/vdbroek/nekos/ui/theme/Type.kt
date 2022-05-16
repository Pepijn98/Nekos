package dev.vdbroek.nekos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.vdbroek.nekos.R

val fonts = FontFamily(
    Font(R.font.nunito_black),
    Font(R.font.nunito_bold),
    Font(R.font.nunito_extra_bold),
    Font(R.font.nunito_extra_light),
    Font(R.font.nunito_italic),
    Font(R.font.nunito_light),
    Font(R.font.nunito_medium),
    Font(R.font.nunito_regular),
    Font(R.font.nunito_semi_bold),
)

// TODO: Define all Typography options
// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

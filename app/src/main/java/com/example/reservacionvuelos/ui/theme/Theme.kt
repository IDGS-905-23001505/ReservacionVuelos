package com.example.reservacionvuelos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = HotelPrimary,
    secondary = HotelSecondary,
    background = HotelBackground,
    surface = HotelCardBackground,
    onPrimary = Color.White,
    onBackground = HotelTextMain,
    onSurface = HotelTextMain,
    outline = HotelInputBorder,
    error = HotelErrorRed
)

@Composable
fun HotelAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography(
            titleLarge = TextStyle(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                letterSpacing = 0.5.sp
            ),
            titleMedium = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            ),
            bodyMedium = TextStyle(
                color = HotelTextSub,
                fontSize = 14.sp
            )
        ),
        content = content
    )
}
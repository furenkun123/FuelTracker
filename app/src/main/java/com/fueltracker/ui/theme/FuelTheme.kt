package com.fueltracker.ui.theme


import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.fueltracker.ui.miui.MiuiColors


private val FuelColors =
    lightColorScheme(

        primary =
            MiuiColors.Primary,

        background =
            MiuiColors.Background,

        surface =
            MiuiColors.Card,

        onSurface =
            MiuiColors.Text

    )


private val FuelTypography =
    Typography()


@Composable
fun FuelTheme(
    content: @Composable () -> Unit
){

    MaterialTheme(

        colorScheme = FuelColors,

        typography = FuelTypography,

        content = content
    )

}
package com.fueltracker.ui.miui


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun MiuiTextIconButton(
    text:String,
    onClick:()->Unit
){

    Text(

        text = text,

        fontSize = 20.sp,

        color =
            MiuiColors.Text,

        modifier =
            Modifier
                .clickable {
                    onClick()
                }
                .padding(8.dp)

    )

}
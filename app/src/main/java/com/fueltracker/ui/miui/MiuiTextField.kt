package com.fueltracker.ui.miui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@Composable
fun MiuiTextField(
    value: String,
    label: String,
    onValueChange: (String)->Unit
){

    TextField(

        value = value,

        onValueChange = onValueChange,

        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(
                    MiuiColors.Card,
                    RoundedCornerShape(12.dp)
                ),

        label = {

            Text(label)

        },

        singleLine = true,

        colors =
            TextFieldDefaults.colors(

                focusedContainerColor =
                    MiuiColors.Card,

                unfocusedContainerColor =
                    MiuiColors.Card,

                focusedIndicatorColor =
                    Color.Transparent,

                unfocusedIndicatorColor =
                    Color.Transparent

            )

    )

}
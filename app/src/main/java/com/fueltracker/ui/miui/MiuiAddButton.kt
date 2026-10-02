package com.fueltracker.ui.miui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun MiuiAddButton(
    onClick: () -> Unit
){

    Row(

        modifier =
            Modifier
                .padding(16.dp)
                .clickable {
                    onClick()
                }
                .padding(
                    horizontal = 20.dp,
                    vertical = 12.dp
                ),

        ){

        Text(
            text = "＋ 记加油"
        )

    }

}
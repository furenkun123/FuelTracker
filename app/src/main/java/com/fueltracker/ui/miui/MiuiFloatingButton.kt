package com.fueltracker.ui.miui


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box



@Composable
fun MiuiFloatingButton(

    text: String,

    onClick: () -> Unit

){

    Box(

        modifier =
            Modifier
                .height(48.dp)
                .background(

                    color =
                        MiuiColors.Primary,

                    shape =
                        RoundedCornerShape(24.dp)

                )
                .clickable {

                    onClick()

                }
                .padding(
                    horizontal = 24.dp
                ),

        contentAlignment =
            Alignment.Center

    ){

        Text(

            text = text,

            color =
                MiuiColors.White

        )

    }

}
package com.fueltracker.ui.miui


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun MiuiSegmentButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
){

    Box(

        modifier =
            modifier
                .height(40.dp)
                .background(

                    if(selected)
                        MiuiColors.Primary
                    else
                        MiuiColors.Card,

                    RoundedCornerShape(10.dp)

                )
                .clickable {

                    onClick()

                }
                .padding(
                    horizontal = 18.dp
                ),

        contentAlignment =
            Alignment.Center

    ){

        Text(

            text = text,

            color =
                if(selected)
                    MiuiColors.White
                else
                    MiuiColors.Text

        )

    }

}
package com.fueltracker.ui.components


import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp



@Composable
fun FuelCard(
    modifier: Modifier = Modifier,
    content:@Composable ()->Unit
){

    Card(

        modifier = modifier,

        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(18.dp),


        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        androidx.compose.ui.graphics
                            .Color.White
                )

    ){

        androidx.compose.foundation.layout
            .Column(

                modifier =
                    Modifier
                        .padding(18.dp)

            ){

                content()

            }

    }

}
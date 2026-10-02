package com.fueltracker.ui.miui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

val MiuiCardBackground = Color(0xFFFFFFFF)

/**
 * 基础 MIUI 风格卡片
 */
@Composable
fun MiuiCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    colors: CardColors = CardDefaults.cardColors(containerColor = MiuiCardBackground),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit // 补充 ColumnScope
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        content = content
    )
}

/**
 * 可点击的 MIUI 风格卡片
 */
@Composable
fun MiuiCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(18.dp),
    colors: CardColors = CardDefaults.cardColors(containerColor = MiuiCardBackground),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit // 补充 ColumnScope
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        content = content
    )
}
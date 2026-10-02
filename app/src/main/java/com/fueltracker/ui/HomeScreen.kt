package com.fueltracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.data.FuelRecord
import com.fueltracker.data.Vehicle
import com.fueltracker.ui.miui.MiuiButton
import com.fueltracker.ui.miui.MiuiCard
import com.fueltracker.ui.miui.MiuiColors
import com.fueltracker.ui.miui.MiuiFloatingButton
import com.fueltracker.ui.miui.MiuiLinkButton
import com.fueltracker.ui.miui.MiuiTextIconButton
import com.fueltracker.ui.miui.MiuiTopBar
import com.fueltracker.ui.miui.VehicleCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    vehicle: Vehicle?,
    records: List<FuelRecord>,
    latestConsumption: Double? = null,
    averageConsumption: Double? = null, // ➕ 直接从 ViewModel 传入，不再在 UI 层实时计算
    onAddVehicle: () -> Unit,
    onAddFuel: () -> Unit,
    onOpenSettings: () -> Unit,
    onViewStatistics: () -> Unit
) {
    // 1. 明确的 Comparator 排序写法（解决 Cannot infer type 问题）
    val sortedRecords = remember(records, vehicle) {
        val valid = if (vehicle?.deliveryDate != null) {
            records.filter { it.timestamp >= vehicle.deliveryDate }
        } else {
            records
        }
        valid.sortedWith { a, b ->
            val odoA = a.odometer ?: 0.0
            val odoB = b.odometer ?: 0.0
            val odoCompare = odoB.compareTo(odoA) // 优先按里程降序
            if (odoCompare != 0) odoCompare else b.timestamp.compareTo(a.timestamp) // 次要按时间戳降序
        }
    }

    // 过滤掉初始记录
    val fuelRecords = remember(sortedRecords) {
        sortedRecords.filter { !it.isInitialRecord }
    }

    val recentRecords = remember(fuelRecords) {
        fuelRecords.take(5) // 取最新的 5 条记录
    }

    // 费用趋势数据：严格按加油时间（timestamp）升序排序
    val chronologicalRecords = remember(fuelRecords) {
        fuelRecords.sortedBy { it.timestamp }
    }
    val costData = remember(chronologicalRecords) {
        chronologicalRecords.map { it.actualPaidAmount }
    }

    val canShowCost = costData.size >= 2

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            MiuiTopBar {
                MiuiTextIconButton(
                    text = "",
                    icon = "⚙",
                    onClick = onOpenSettings
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    top = 12.dp,
                    bottom = 100.dp
                )
            ) {
                if (vehicle == null) {
                    item {
                        EmptyVehicleCard(onAddVehicle = onAddVehicle)
                    }
                } else {
                    item {
                        VehicleCard(vehicle = vehicle)
                    }

                    item {
                        ConsumptionCard(
                            recordCount = fuelRecords.size,
                            latestConsumption = latestConsumption ?: fuelRecords.firstOrNull { it.consumption != null }?.consumption,
                            averageConsumption = averageConsumption
                        )
                    }

                    // 费用趋势图表卡片
                    if (canShowCost) {
                        item {
                            ChartCard(
                                title = "费用趋势",
                                unit = "元",
                                values = costData
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "最近加油",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )

                            MiuiLinkButton(
                                text = "统计 ›",
                                onClick = onViewStatistics
                            )
                        }
                    }

                    if (recentRecords.isEmpty()) {
                        item {
                            EmptyRecordCard()
                        }
                    } else {
                        items(
                            items = recentRecords,
                            key = { record: FuelRecord -> record.id }
                        ) { record: FuelRecord ->
                            HomeFuelRecordItem(
                                record = record // 移除未使用的 vehicle 参数
                            )
                        }
                    }
                }
            }
        }

        if (vehicle != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = 24.dp
                    )
            ) {
                MiuiFloatingButton(
                    text = "＋ 记加油",
                    onClick = onAddFuel
                )
            }
        }
    }
}

@Composable
private fun EmptyVehicleCard(
    onAddVehicle: () -> Unit
) {
    MiuiCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "还没有车辆",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MiuiColors.Text
            )
            Text(
                text = "添加你的车辆后开始记录油耗",
                style = MaterialTheme.typography.bodyMedium,
                color = MiuiColors.SecondaryText
            )
            MiuiButton(
                text = "添加车辆",
                onClick = onAddVehicle
            )
        }
    }
}

@Composable
private fun ConsumptionCard(
    recordCount: Int,
    latestConsumption: Double?,
    averageConsumption: Double?
) {
    MiuiCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "当前油耗",
                style = MaterialTheme.typography.titleMedium,
                color = MiuiColors.SecondaryText
            )
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = latestConsumption?.let { "%.2f".format(it) } ?: "--",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MiuiColors.Text
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "L/100km",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiuiColors.SecondaryText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiuiStatisticItem(
                    title = "平均油耗",
                    value = averageConsumption?.let { "%.2f L/100km".format(it) } ?: "--"
                )
                MiuiStatisticItem(
                    title = "加油次数",
                    value = "$recordCount 次"
                )
            }
        }
    }
}

@Composable
private fun MiuiStatisticItem(
    title: String,
    value: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MiuiColors.SecondaryText
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MiuiColors.Text
        )
    }
}

@Composable
private fun ChartCard(
    title: String,
    unit: String,
    values: List<Double>
) {
    if (values.isEmpty()) return

    val maxValue = values.maxOrNull() ?: return
    val minValue = values.minOrNull() ?: return

    val yMax = if (maxValue > 0) maxValue * 1.15 else 1.0

    val textMeasurer = rememberTextMeasurer()
    val baseLabelStyle = TextStyle(
        color = MiuiColors.SecondaryText,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Normal
    )

    val scrollState = rememberScrollState()

    LaunchedEffect(values.size, scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    MiuiCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuiColors.Text
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "($unit)",
                    fontSize = 10.sp,
                    color = MiuiColors.SecondaryText
                )
            }

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val visibleCount = 12
                val itemWidth = maxWidth / visibleCount

                val chartWidth = if (values.size <= visibleCount) {
                    maxWidth
                } else {
                    itemWidth * values.size
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                ) {
                    Canvas(
                        modifier = Modifier
                            .width(chartWidth)
                            .height(110.dp)
                    ) {
                        val width = size.width
                        val topPadding = 16.dp.toPx()
                        val chartHeight = size.height - topPadding

                        val count = values.size
                        val slotWidth = width / count

                        val barWidth = (slotWidth * 0.45f).coerceIn(4.dp.toPx(), 14.dp.toPx())

                        values.forEachIndexed { index, value ->
                            val slotLeft = index * slotWidth
                            val barCenterX = slotLeft + slotWidth / 2f
                            val barLeft = barCenterX - barWidth / 2f

                            val heightFactor = (value / yMax).toFloat().coerceIn(0f, 1f)
                            val barHeight = chartHeight * heightFactor
                            val barTop = topPadding + (chartHeight - barHeight)

                            val isMax = value == maxValue
                            val isMin = value == minValue

                            val barColor = if (isMax) {
                                MiuiColors.Primary
                            } else {
                                MiuiColors.Primary.copy(alpha = 0.65f)
                            }

                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(barLeft, barTop),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(x = 3.dp.toPx(), y = 3.dp.toPx())
                            )

                            val textString = "%.1f".format(value)
                            val currentStyle = if (isMax || isMin) {
                                baseLabelStyle.copy(
                                    color = MiuiColors.Text,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                baseLabelStyle
                            }

                            val textLayoutResult = textMeasurer.measure(textString, currentStyle)
                            val tWidth = textLayoutResult.size.width.toFloat()
                            val tHeight = textLayoutResult.size.height.toFloat()

                            val textX = (barCenterX - tWidth / 2f).coerceIn(0f, width - tWidth)
                            val textY = barTop - tHeight - 2.dp.toPx()

                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(textX, textY)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeFuelRecordItem(
    record: FuelRecord // 已经去掉了未使用的 vehicle 形参
) {
    val timeText = remember<String>(record.timestamp) {
        SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(record.timestamp))
    }

    // 直接读取落盘的油耗字段
    val singleConsumption = record.consumption

    val paidAmount = record.actualPaidAmount
    val volume = record.volume
    val unitPrice = record.unitPrice

    val odometerText = record.odometer?.let {
        if (it % 1.0 == 0.0) "%.0f".format(it) else "%.1f".format(it)
    } ?: "--"

    MiuiCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiuiColors.SecondaryText
                )
                Text(
                    text = "¥%.2f".format(paidAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuiColors.Text
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "%.2f L".format(volume),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MiuiColors.Text
                    )
                    Text(
                        text = "$odometerText km · %.2f 元/L".format(unitPrice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MiuiColors.SecondaryText
                    )
                }
                singleConsumption?.let { consumption ->
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "%.2f".format(consumption),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MiuiColors.Text
                        )
                        Text(
                            text = "L/100km",
                            style = MaterialTheme.typography.labelSmall,
                            color = MiuiColors.SecondaryText
                        )
                    }
                }
            }
            record.remainingFuel?.let { remaining ->
                Text(
                    text = "加油后剩余 %.2f L".format(remaining),
                    style = MaterialTheme.typography.bodySmall,
                    color = MiuiColors.SecondaryText
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (record.isFull) {
                    Text(
                        text = "已加满",
                        style = MaterialTheme.typography.labelSmall,
                        color = MiuiColors.Primary
                    )
                }

                if (record.hasMissedRecord) {
                    val missedVol = record.missedVolume ?: 0.0
                    Text(
                        text = "含漏记 %.2f L".format(missedVol),
                        style = MaterialTheme.typography.labelSmall,
                        color = MiuiColors.SecondaryText
                    )
                }

                if (!record.isFull) {
                    Text(
                        text = "未加满",
                        style = MaterialTheme.typography.labelSmall,
                        color = MiuiColors.SecondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRecordCard() {
    MiuiCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "还没有加油记录",
            modifier = Modifier.padding(24.dp),
            color = MiuiColors.SecondaryText
        )
    }
}
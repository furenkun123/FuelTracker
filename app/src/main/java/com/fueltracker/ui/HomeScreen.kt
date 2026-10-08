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
import androidx.compose.runtime.snapshotFlow
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
import com.fueltracker.ui.miui.MiuiFloatingButton
import com.fueltracker.ui.miui.MiuiLinkButton
import com.fueltracker.ui.miui.MiuiTextIconButton
import com.fueltracker.ui.miui.MiuiTopBar
import com.fueltracker.ui.miui.VehicleCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.flow.first

private val homeRecordDateFormat by lazy {
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
}

@Composable
fun HomeScreen(
    vehicle: Vehicle?,
    records: List<FuelRecord>,
    latestConsumption: Double? = null,
    averageConsumption: Double? = null,
    onAddVehicle: () -> Unit,
    onAddFuel: () -> Unit,
    onOpenSettings: () -> Unit,
    onViewStatistics: () -> Unit
) {
    // 真正使用到的只有 deliveryDate，避免整个 vehicle 对象变化时重新计算
    val deliveryDate = vehicle?.deliveryDate

    // 统一按时间倒序，并过滤交付日期之前的记录
    val sortedRecords = remember(records, deliveryDate) {
        val validRecords = if (deliveryDate != null) {
            records.filter { it.timestamp >= deliveryDate }
        } else {
            records
        }
        validRecords.sortedByDescending { it.timestamp }
    }

    // 首页不展示初始记录（保持时间倒序）
    val fuelRecords = remember(sortedRecords) {
        sortedRecords.filter { !it.isInitialRecord }
    }

    // 最近 5 条记录
    val recentRecords = remember(fuelRecords) {
        fuelRecords.take(5)
    }

    // 图表需要时间升序，直接反转即可
    val chronologicalRecords = remember(fuelRecords) {
        fuelRecords.asReversed()
    }

    // 费用趋势数据
    val costData = remember(chronologicalRecords) {
        chronologicalRecords.map { it.actualPaidAmount }
    }

    val canShowCost = costData.size >= 2

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏
            MiuiTopBar {
                MiuiTextIconButton(
                    text = "",
                    icon = "⚙",
                    onClick = onOpenSettings
                )
            }

            // 首页主体
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
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
                        val resolvedLatestConsumption = latestConsumption
                            ?: fuelRecords.firstOrNull { it.consumption != null }?.consumption

                        ConsumptionCard(
                            recordCount = fuelRecords.size,
                            latestConsumption = resolvedLatestConsumption,
                            averageConsumption = averageConsumption
                        )
                    }

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
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
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
                            HomeFuelRecordItem(record = record)
                        }
                    }
                }
            }
        }

        // 记加油悬浮按钮
        if (vehicle != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 24.dp)
            ) {
                MiuiFloatingButton(
                    text = "＋ 记加油",
                    onClick = onAddFuel
                )
            }
        }
    }
}

// =================================================
// 无车辆
// =================================================

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
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "添加你的车辆后开始记录油耗",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            MiuiButton(
                text = "添加车辆",
                onClick = onAddVehicle
            )
        }
    }
}

// =================================================
// 油耗统计卡片
// =================================================

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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = latestConsumption?.let { "%.2f".format(it) } ?: "--",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "L/100km",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// =================================================
// 费用趋势柱状图
// =================================================

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

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val textMeasurer = rememberTextMeasurer()

    val baseLabelStyle = remember(onSurfaceVariantColor) {
        TextStyle(
            color = onSurfaceVariantColor,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Normal
        )
    }

    // 缓存文字测量结果，避免 Canvas 重绘时重复 measure
    val labelLayouts = remember(values, maxValue, minValue, onSurfaceColor, onSurfaceVariantColor) {
        values.map { value ->
            val isMax = value == maxValue
            val isMin = value == minValue
            val currentStyle = if (isMax || isMin) {
                baseLabelStyle.copy(color = onSurfaceColor, fontWeight = FontWeight.Bold)
            } else {
                baseLabelStyle
            }

            textMeasurer.measure(
                text = "%.1f".format(value),
                style = currentStyle
            )
        }
    }

    val scrollState = rememberScrollState()
    val visibleCount = 12

    // 仅在数据数量变化时自动滚动到最右侧
    LaunchedEffect(values.size) {
        if (values.size > visibleCount) {
            val maxScroll = snapshotFlow { scrollState.maxValue }.first { it > 0 }
            scrollState.scrollTo(maxScroll)
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
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "($unit)",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val itemWidth = maxWidth / visibleCount
                val chartWidth = if (values.size <= visibleCount) maxWidth else itemWidth * values.size

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

                            val barColor = when {
                                isMax -> primaryColor
                                isMin -> primaryColor.copy(alpha = 0.85f)
                                else -> primaryColor.copy(alpha = 0.65f)
                            }

                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(barLeft, barTop),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )

                            val textLayoutResult = labelLayouts[index]
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

// =================================================
// 首页最近加油记录
// =================================================

@Composable
private fun HomeFuelRecordItem(
    record: FuelRecord
) {
    val timeText = remember(record.timestamp) {
        homeRecordDateFormat.format(Date(record.timestamp))
    }

    val singleConsumption = record.consumption
    val paidAmount = record.actualPaidAmount
    val volume = record.volume
    val unitPrice = record.unitPrice

    val odometerText = record.odometer?.let { odometer ->
        val rounded = odometer.toInt()
        if (abs(odometer - rounded) < 0.001) {
            "%.0f".format(odometer)
        } else {
            "%.1f".format(odometer)
        }
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
            // 时间 + 金额
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "¥%.2f".format(paidAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 油量 + 油耗
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "%.2f L".format(volume),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "$odometerText km · %.2f 元/L".format(unitPrice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                singleConsumption?.let { consumption ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "%.2f".format(consumption),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "L/100km",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 加油后剩余油量
            record.remainingFuel?.let { remaining ->
                Text(
                    text = "加油后剩余 %.2f L".format(remaining),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 状态标签
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (record.isFull) {
                    Text(
                        text = "已加满",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (record.hasMissedRecord) {
                    val missedVolume = record.missedVolume ?: 0.0
                    Text(
                        text = "含漏记 %.2f L".format(missedVolume),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!record.isFull) {
                    Text(
                        text = "未加满",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// =================================================
// 空记录
// =================================================

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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
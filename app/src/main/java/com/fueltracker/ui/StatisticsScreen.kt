package com.fueltracker.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.data.FuelRecord
import com.fueltracker.data.Vehicle
import com.fueltracker.util.ConsumptionResult
import com.fueltracker.util.FuelCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val MiuiBackground = Color(0xFFF7F7F7)
private val MiuiCard = Color(0xFFFFFFFF)
private val MiuiTextPrimary = Color(0xFF222222)
private val MiuiTextGray = Color(0xFF888888)
private val MiuiPrimary = Color(0xFF007AFF)

private enum class StatisticsFilter {
    ALL,
    MONTH,
    YEAR,
    CUSTOM_MONTH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    vehicle: Vehicle?,
    records: List<FuelRecord>,
    onBack: () -> Unit
) {
    val now = remember { System.currentTimeMillis() }
    val currentCalendar = remember(now) { Calendar.getInstance().apply { timeInMillis = now } }
    val currentYear = currentCalendar.get(Calendar.YEAR)
    val currentMonth = currentCalendar.get(Calendar.MONTH) + 1

    var selectedFilter by remember { mutableStateOf(StatisticsFilter.ALL) }
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }
    var showMonthDialog by remember { mutableStateOf(false) }

    val availableYears = remember(records, currentYear) {
        val recordYears = records.map {
            Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.YEAR)
        }
        (recordYears + currentYear).distinct().sortedDescending()
    }

    val range = remember(selectedFilter, now, selectedYear, selectedMonth) {
        getStatisticsRange(
            filter = selectedFilter,
            now = now,
            customYear = selectedYear,
            customMonth = selectedMonth
        )
    }

    val summary = remember(vehicle, records, range) {
        FuelCalculator.calculateSummary(
            vehicle = vehicle,
            allRecords = records,
            startTime = range.first,
            endTime = range.second
        )
    }

    Scaffold(
        containerColor = MiuiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "统计",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MiuiBackground
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 筛选控制区
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "统计范围",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuiTextGray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val chipShape = RoundedCornerShape(16.dp)
                        val chipColors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            labelColor = MiuiTextPrimary,
                            selectedContainerColor = MiuiPrimary,
                            selectedLabelColor = Color.White
                        )

                        FilterChip(
                            selected = selectedFilter == StatisticsFilter.ALL,
                            onClick = { selectedFilter = StatisticsFilter.ALL },
                            label = { Text("全部") },
                            shape = chipShape,
                            colors = chipColors,
                            border = null
                        )

                        FilterChip(
                            selected = selectedFilter == StatisticsFilter.MONTH,
                            onClick = { selectedFilter = StatisticsFilter.MONTH },
                            label = { Text("本月") },
                            shape = chipShape,
                            colors = chipColors,
                            border = null
                        )

                        FilterChip(
                            selected = selectedFilter == StatisticsFilter.YEAR,
                            onClick = { selectedFilter = StatisticsFilter.YEAR },
                            label = { Text("今年") },
                            shape = chipShape,
                            colors = chipColors,
                            border = null
                        )

                        FilterChip(
                            selected = selectedFilter == StatisticsFilter.CUSTOM_MONTH,
                            onClick = {
                                showMonthDialog = true
                            },
                            label = {
                                Text(
                                    if (selectedFilter == StatisticsFilter.CUSTOM_MONTH) {
                                        "${selectedYear}年${selectedMonth}月"
                                    } else {
                                        "选择月份"
                                    }
                                )
                            },
                            shape = chipShape,
                            colors = chipColors,
                            border = null
                        )
                    }

                    Text(
                        text = getFilterDescription(
                            filter = selectedFilter,
                            now = now,
                            customYear = selectedYear,
                            customMonth = selectedMonth
                        ),
                        fontSize = 12.sp,
                        color = MiuiTextGray
                    )
                }
            }

            // 核心统计指标卡片
            item {
                MiuixCard {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "统计数据",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatisticValue(title = "加油次数", value = "${summary.totalCount} 次")
                            StatisticValue(title = "累计加油", value = "%.2f L".format(summary.totalVolume))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatisticValue(title = "累计花费", value = "¥%.2f".format(summary.totalCost))
                            StatisticValue(
                                title = "平均油价",
                                value = summary.averagePrice?.let { "%.2f 元/L".format(it) } ?: "--"
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatisticValue(title = "累计行驶", value = "%.0f km".format(summary.totalDistance))
                            StatisticValue(
                                title = "平均油耗",
                                value = summary.averageConsumption?.let { "%.2f L/100km".format(it) } ?: "--"
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatisticValue(title = "起止表显里程", value = summary.odometerSpanText)
                        }

                        // 未闭合记录提示
                        if (summary.unclosedCount > 0) {
                            Text(
                                text = "提示：含有 ${summary.unclosedCount} 次加油尚未填写后续里程，将在下次填写里程后自动推算平摊油耗。",
                                fontSize = 12.sp,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            }

            // 漏记加油量卡片
            if (summary.totalMissedVolume > 0) {
                item {
                    MiuixCard {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "漏记加油量",
                                fontSize = 14.sp,
                                color = MiuiTextGray
                            )
                            Text(
                                text = "%.2f L".format(summary.totalMissedVolume),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "该数据已计入消耗量，未计入实际支付金额",
                                fontSize = 12.sp,
                                color = MiuiTextGray
                            )
                        }
                    }
                }
            }

            // 油耗明细列表
            item {
                Text(
                    text = "油耗明细",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (summary.rangeResults.isEmpty()) {
                item {
                    MiuixCard {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "暂无可计算的油耗",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = when (selectedFilter) {
                                    StatisticsFilter.ALL -> "在后续加油记录中填写一次里程后，系统会自动以起点里程平摊推算油耗。"
                                    StatisticsFilter.MONTH -> "本月暂时没有已闭合的里程及油耗数据。"
                                    StatisticsFilter.YEAR -> "今年暂时没有已闭合的里程及油耗数据。"
                                    StatisticsFilter.CUSTOM_MONTH -> "${selectedYear}年${selectedMonth}月暂无可计算的油耗数据。"
                                },
                                fontSize = 13.sp,
                                color = MiuiTextGray
                            )
                        }
                    }
                }
            } else {
                items(
                    items = summary.rangeResults.asReversed(),
                    key = { it.recordId }
                ) { result ->
                    StatisticsConsumptionItem(
                        result = result,
                        records = records
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showMonthDialog) {
        HyperWheelMonthPickerDialog(
            years = availableYears,
            initialYear = selectedYear,
            initialMonth = selectedMonth,
            onDismiss = { showMonthDialog = false },
            onConfirm = { year, month ->
                selectedYear = year
                selectedMonth = month
                selectedFilter = StatisticsFilter.CUSTOM_MONTH
                showMonthDialog = false
            }
        )
    }
}

@Composable
private fun MiuixCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MiuiCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = { content() }
    )
}

@Composable
private fun StatisticValue(
    title: String,
    value: String
) {
    Column {
        Text(
            text = title,
            fontSize = 12.sp,
            color = MiuiTextGray
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatisticsConsumptionItem(
    result: ConsumptionResult,
    records: List<FuelRecord>
) {
    val dateFormat = remember { SimpleDateFormat("yy-MM-dd", Locale.getDefault()) }
    val record = remember(records, result.recordId) {
        records.firstOrNull { it.id == result.recordId }
    }
    val timeText = remember(record) {
        record?.let { dateFormat.format(Date(it.timestamp)) } ?: "--"
    }

    MiuixCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "%.2f L/100km".format(result.consumption),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6900)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "表显: %.0f km → %.0f km".format(result.startOdometer, result.endOdometer),
                    fontSize = 13.sp,
                    color = MiuiTextPrimary.copy(alpha = 0.8f)
                )
                Text(
                    text = "行驶 %.0f km · 加油 %.2f L".format(result.distance, result.fuelUsed),
                    fontSize = 13.sp,
                    color = MiuiTextGray
                )
            }

            Text(
                text = if (result.isEstimated) "油量平摊估算" else "精准计算",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (result.isEstimated) Color(0xFFFF9800) else Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
private fun HyperWheelMonthPickerDialog(
    years: List<Int>,
    initialYear: Int,
    initialMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var year by remember { mutableIntStateOf(initialYear) }
    var month by remember { mutableIntStateOf(initialMonth) }
    val months = remember { (1..12).toList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MiuiCard,
        title = {
            Text(
                text = "选择统计月份",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MiuiTextPrimary
            )
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HyperWheelList(
                    selectedValue = year,
                    items = years,
                    modifier = Modifier.weight(1f),
                    itemLabel = { "$it 年" },
                    onChange = { year = it }
                )
                HyperWheelList(
                    selectedValue = month,
                    items = months,
                    modifier = Modifier.weight(1f),
                    itemLabel = { "%02d 月".format(it) },
                    onChange = { month = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(year, month) }) {
                Text("确定", color = MiuiPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MiuiTextGray)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> HyperWheelList(
    selectedValue: T,
    items: List<T>,
    modifier: Modifier = Modifier,
    visibleCount: Int = 3,
    itemLabel: (T) -> String = { it.toString() },
    onChange: (T) -> Unit
) {
    val itemHeight = 38.dp
    val density = LocalDensity.current
    val itemHeightPx = remember(density, itemHeight) { with(density) { itemHeight.toPx() } }

    val initialIndex = remember(items) {
        items.indexOf(selectedValue).coerceAtLeast(0)
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // 计算当前处于中间位置的 Item 索引
    val selectedIndex by remember {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            if (offset > itemHeightPx / 2f) firstVisible + 1 else firstVisible
        }
    }

    // 只有当列表滑动停止（isScrollInProgress == false）且索引发生变化时，才同步通知外部
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (!isScrolling) {
                    val targetIndex = selectedIndex.coerceIn(items.indices)
                    val currentItem = items[targetIndex]
                    if (currentItem != selectedValue) {
                        onChange(currentItem)
                    }
                }
            }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleCount)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(MiuiTextPrimary.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
        )

        LazyColumn(
            state = listState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleCount / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = items.size,
                key = { index -> items[index].hashCode() }
            ) { index ->
                val isSelected = index == selectedIndex
                val item = items[index]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = itemLabel(item),
                        fontSize = if (isSelected) 15.sp else 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MiuiTextPrimary else MiuiTextPrimary.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

private fun getStatisticsRange(
    filter: StatisticsFilter,
    now: Long,
    customYear: Int,
    customMonth: Int
): Pair<Long, Long> {

    if (filter == StatisticsFilter.ALL) {
        return Long.MIN_VALUE to Long.MAX_VALUE
    }

    val calendar = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    when (filter) {
        StatisticsFilter.MONTH -> {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
        }

        StatisticsFilter.YEAR -> {
            calendar.set(Calendar.MONTH, Calendar.JANUARY)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
        }

        StatisticsFilter.CUSTOM_MONTH -> {
            calendar.set(Calendar.YEAR, customYear)
            calendar.set(Calendar.MONTH, customMonth - 1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val start = calendar.timeInMillis
    val endCalendar = calendar.clone() as Calendar

    when (filter) {
        StatisticsFilter.MONTH, StatisticsFilter.CUSTOM_MONTH -> endCalendar.add(Calendar.MONTH, 1)
        StatisticsFilter.YEAR -> endCalendar.add(Calendar.YEAR, 1)
    }

    val end = endCalendar.timeInMillis - 1

    return start to end
}

private fun getFilterDescription(
    filter: StatisticsFilter,
    now: Long,
    customYear: Int,
    customMonth: Int
): String {
    return when (filter) {
        StatisticsFilter.ALL -> "显示全部历史记录"
        StatisticsFilter.MONTH -> SimpleDateFormat("yyyy年MM月", Locale.getDefault()).format(Date(now)) + " · 显示本月记录"
        StatisticsFilter.YEAR -> SimpleDateFormat("yyyy年", Locale.getDefault()).format(Date(now)) + " · 显示今年记录"
        StatisticsFilter.CUSTOM_MONTH -> "${customYear}年${customMonth}月 · 显示指定月份记录"
    }
}
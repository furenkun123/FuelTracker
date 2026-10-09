package com.fueltracker.ui

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
import androidx.compose.material3.MaterialTheme
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

private enum class StatisticsFilter {
    ALL,
    MONTH,
    YEAR,
    CUSTOM_MONTH
}
/**
 * 统计明细列表的显示项。
 * 有油耗结果的和没结果的混在一起按时间倒序展示。
 */
private sealed class StatisticsDisplayItem {
    abstract val timestamp: Long

    data class WithResult(
        val result: ConsumptionResult,
        override val timestamp: Long
    ) : StatisticsDisplayItem()

    data class Pending(
        val record: FuelRecord
    ) : StatisticsDisplayItem() {
        override val timestamp: Long get() = record.timestamp
    }
}

// 语义色 (深浅色模式下均清晰)
private val ConsumptionOrange = Color(0xFFFF6900)
private val WarningOrange = Color(0xFFFF9800)
private val SuccessGreen = Color(0xFF4CAF50)

// 日期格式化工具复用，避免列表滑动时重复创建对象
private val ITEM_DATE_FORMAT by lazy {
    SimpleDateFormat("yy-MM-dd", Locale.getDefault())
}
private val MONTH_DESC_FORMAT by lazy {
    SimpleDateFormat("yyyy年MM月", Locale.getDefault())
}
private val YEAR_DESC_FORMAT by lazy {
    SimpleDateFormat("yyyy年", Locale.getDefault())
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
    val currentYear = remember(currentCalendar) { currentCalendar.get(Calendar.YEAR) }
    val currentMonth = remember(currentCalendar) { currentCalendar.get(Calendar.MONTH) + 1 }

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

    // recordId → FuelRecord 映射，供明细列表复用
    val recordMap = remember(records) { records.associateBy { it.id } }

    // 合并"有结果的记录"和"无结果的记录"，统一按时间倒序
    val displayItems = remember(summary, recordMap) {
        val list = mutableListOf<StatisticsDisplayItem>()

        summary.rangeResults.forEach { result ->
            val timestamp = recordMap[result.recordId]?.timestamp ?: 0L
            list += StatisticsDisplayItem.WithResult(
                result = result,
                timestamp = timestamp
            )
        }

        summary.pendingRecords.forEach { record ->
            list += StatisticsDisplayItem.Pending(record = record)
        }

        list.sortByDescending { it.timestamp }
        list
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "统计",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val chipShape = RoundedCornerShape(16.dp)
                        val chipColors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
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
                            onClick = { showMonthDialog = true },
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
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

                        if (summary.unclosedCount > 0 || summary.missingOdometerCount > 0) {
                            val parts = buildList {
                                if (summary.missingOdometerCount > 0) {
                                    add("${summary.missingOdometerCount} 次未填里程")
                                }
                                if (summary.unclosedCount > 0) {
                                    add("${summary.unclosedCount} 次已填里程但未闭合")
                                }
                            }
                            Text(
                                text = "提示：${parts.joinToString("，")}，未填里程的记录油量会在下次填写里程时平摊推算。",
                                fontSize = 12.sp,
                                color = WarningOrange
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "%.2f L".format(summary.totalMissedVolume),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "该数据已计入消耗量，未计入实际支付金额",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (displayItems.isEmpty()) {
                item {
                    MiuixCard {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "暂无记录",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (selectedFilter) {
                                    StatisticsFilter.ALL -> "还没有任何加油记录。"
                                    StatisticsFilter.MONTH -> "本月暂时没有加油记录。"
                                    StatisticsFilter.YEAR -> "今年暂时没有加油记录。"
                                    StatisticsFilter.CUSTOM_MONTH -> "${selectedYear}年${selectedMonth}月暂无加油记录。"
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = displayItems,
                    key = { item ->
                        when (item) {
                            is StatisticsDisplayItem.WithResult -> "r:${item.result.recordId}"
                            is StatisticsDisplayItem.Pending -> "p:${item.record.id}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is StatisticsDisplayItem.WithResult -> {
                            StatisticsConsumptionItem(
                                result = item.result,
                                timestamp = item.timestamp
                            )
                        }
                        is StatisticsDisplayItem.Pending -> {
                            StatisticsPendingItem(record = item.record)
                        }
                    }
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatisticsConsumptionItem(
    result: ConsumptionResult,
    timestamp: Long
) {
    val timeText = remember(timestamp) {
        ITEM_DATE_FORMAT.format(Date(timestamp))
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
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "%.2f L/100km".format(result.consumption),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConsumptionOrange
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "表显: %.0f km → %.0f km".format(result.startOdometer, result.endOdometer),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "行驶 %.0f km · 加油 %.2f L".format(result.distance, result.fuelUsed),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = if (result.isEstimated) "油量平摊估算" else "精准计算",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (result.isEstimated) WarningOrange else SuccessGreen
            )
        }
    }
}
@Composable
private fun StatisticsPendingItem(
    record: FuelRecord
) {
    val timeText = remember(record.timestamp) {
        ITEM_DATE_FORMAT.format(Date(record.timestamp))
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
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "未记录里程",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = WarningOrange
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "加油 %.2f L".format(record.volume),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "¥%.2f".format(record.actualPaidAmount),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = buildString {
                    if (record.hasMissedRecord) {
                        val missedVolume = record.missedVolume ?: 0.0
                        append("含漏记 %.2f L · ".format(missedVolume))
                    }
                    append(if (record.isFull) "已加满" else "未加满")
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "选择统计月份",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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
                Text(
                    "确定",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

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
    val itemHeightPx = remember(density, itemHeight) {
        with(density) { itemHeight.toPx() }
    }

    // 补全 key 依赖，确保 selectedValue 或 items 变化时重置索引
    val initialIndex = remember(items, selectedValue) {
        items.indexOf(selectedValue).coerceAtLeast(0)
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // 计算当前选中索引，增加边界约束防止末尾越界
    val selectedIndex by remember(items, itemHeightPx) {
        derivedStateOf {
            if (items.isEmpty()) 0
            else {
                val firstVisible = listState.firstVisibleItemIndex
                val offset = listState.firstVisibleItemScrollOffset
                val rawIndex = if (offset > itemHeightPx / 2f) firstVisible + 1 else firstVisible
                rawIndex.coerceIn(items.indices)
            }
        }
    }

    // 当外部 selectedValue 改变时，同步滚动滚轮
    LaunchedEffect(selectedValue, items) {
        val targetIndex = items.indexOf(selectedValue).coerceAtLeast(0)
        if (items.isNotEmpty() && targetIndex in items.indices && targetIndex != selectedIndex) {
            listState.scrollToItem(targetIndex)
        }
    }

    // 滚动停止后向外部回调选中值
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (!isScrolling && items.isNotEmpty()) {
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
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                )
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
                key = { index -> items[index] ?: index }
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
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
        StatisticsFilter.MONTH, StatisticsFilter.CUSTOM_MONTH ->
            endCalendar.add(Calendar.MONTH, 1)
        StatisticsFilter.YEAR ->
            endCalendar.add(Calendar.YEAR, 1)
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
        StatisticsFilter.MONTH ->
            MONTH_DESC_FORMAT.format(Date(now)) + " · 显示本月记录"
        StatisticsFilter.YEAR ->
            YEAR_DESC_FORMAT.format(Date(now)) + " · 显示今年记录"
        StatisticsFilter.CUSTOM_MONTH ->
            "${customYear}年${customMonth}月 · 显示指定月份记录"
    }
}
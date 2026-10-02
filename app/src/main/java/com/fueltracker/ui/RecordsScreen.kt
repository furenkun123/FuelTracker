package com.fueltracker.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fueltracker.data.FuelRecord
import com.fueltracker.data.Vehicle
import com.fueltracker.util.ConsumptionResult
import com.fueltracker.util.FuelCalculator
import com.fueltracker.util.IntervalConfidence
import com.fueltracker.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material3.MaterialTheme


private val recordDateFormat by lazy {
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onEdit: (FuelRecord) -> Unit
) {
    val records by viewModel.records.collectAsState()
    val vehicle by viewModel.currentVehicle.collectAsState()

    // 1. 修正：传入参数顺序为 (records, vehicle)
    val consumptionMap: Map<Long, ConsumptionResult> = remember(records, vehicle) {
        FuelCalculator.calculateMap(records, vehicle)
    }

    // 当前系统时间与可选年份
    val now = remember { Calendar.getInstance() }
    val currentYear = remember(now) { now.get(Calendar.YEAR) }

    val availableYears = remember(records, currentYear) {
        val recordYears = records.map {
            Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.YEAR)
        }
        (recordYears + currentYear).distinct().sortedDescending()
    }

    // 筛选状态 (selectedDay == 0 代表“全月”)
    var filterType by remember { mutableStateOf("全部") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var selectedMonth by remember { mutableIntStateOf(now.get(Calendar.MONTH) + 1) }
    var selectedDay by remember { mutableIntStateOf(0) }

    // 筛选后的记录（按时间倒序）
    val displayRecords = remember(records, filterType, selectedYear, selectedMonth, selectedDay) {
        val sorted = records.sortedByDescending { it.timestamp }
        when (filterType) {
            "本月" -> {
                val cNow = Calendar.getInstance()
                sorted.filter {
                    val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    c.get(Calendar.YEAR) == cNow.get(Calendar.YEAR) &&
                            c.get(Calendar.MONTH) == cNow.get(Calendar.MONTH)
                }
            }
            "按时间" -> {
                sorted.filter {
                    val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    val matchYear = c.get(Calendar.YEAR) == selectedYear
                    val matchMonth = (c.get(Calendar.MONTH) + 1) == selectedMonth
                    val matchDay = if (selectedDay == 0) true else c.get(Calendar.DAY_OF_MONTH) == selectedDay
                    matchYear && matchMonth && matchDay
                }
            }
            else -> sorted
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "全部加油记录",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. 顶部筛选胶囊条
            RecordFilterBar(
                filterType = filterType,
                year = selectedYear,
                month = selectedMonth,
                day = selectedDay,
                onFilterChange = { filterType = it },
                onMonthClick = { showDatePicker = true }
            )

            // 2. 统计概览卡片
            RecordStatistics(
                vehicle = vehicle,
                allRecords = records,
                displayRecords = displayRecords
            )

            Spacer(modifier = Modifier.height(2.dp))

            // 3. 记录列表或空状态
            if (displayRecords.isEmpty()) {
                EmptyRecordsState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(Modifier.height(2.dp)) }

                    items(
                        items = displayRecords,
                        key = { it.id }
                    ) { record ->
                        FuelRecordCard(
                            record = record,
                            calculationResult = consumptionMap[record.id],
                            onDelete = { viewModel.deleteRecord(record) },
                            onEdit = { onEdit(record) }
                        )
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    // 时间选择底部弹窗
    if (showDatePicker) {
        DatePicker3WheelSheet(
            initialYear = selectedYear,
            initialMonth = selectedMonth,
            initialDay = selectedDay,
            availableYears = availableYears,
            onSelect = { year, month, day ->
                selectedYear = year
                selectedMonth = month
                selectedDay = day
                filterType = "按时间"
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

// =================================================
// 基础卡片封装 (HyperOS / MIUI 风格)
// =================================================

@Composable
private fun MiuixCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .then(modifier),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

// =================================================
// 统计汇总卡片
// =================================================

@Composable
private fun RecordStatistics(
    vehicle: Vehicle?,
    allRecords: List<FuelRecord>,
    displayRecords: List<FuelRecord>
) {
    val totalMoney = remember(displayRecords) { displayRecords.sumOf { it.actualPaidAmount } }
    val totalVolume = remember(displayRecords) { displayRecords.sumOf { it.volume } }

    // 2. 修正：传入参数顺序为 (allRecords, vehicle)
    val averageConsumption = remember(vehicle, allRecords, displayRecords) {
        val allMap = FuelCalculator.calculateMap(allRecords, vehicle)
        val validResults = displayRecords.mapNotNull { allMap[it.id] }.filter { !it.isOutlier }
        val totalDistance = validResults.sumOf { it.distance }
        val totalFuel = validResults.sumOf { it.fuelUsed }

        if (totalDistance > 0) {
            totalFuel / totalDistance * 100.0
        } else {
            null
        }
    }

    MiuixCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "次数", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${displayRecords.size}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column {
                Text(text = "金额", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "¥%.2f".format(totalMoney),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column {
                Text(text = "油量", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "%.1f L".format(totalVolume),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column {
                Text(text = "油耗", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = averageConsumption?.let { "%.1f".format(it) } ?: "--",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// =================================================
// 筛选胶囊组件
// =================================================

@Composable
private fun RecordFilterBar(
    filterType: String,
    year: Int,
    month: Int,
    day: Int,
    onFilterChange: (String) -> Unit,
    onMonthClick: () -> Unit
) {
    val customText = remember(year, month, day) {
        if (day == 0) "${year}.${month}月 ▼" else "${year}.${month}.${day} ▼"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterCapsule(
            text = "全部",
            selected = filterType == "全部",
            onClick = { onFilterChange("全部") }
        )

        FilterCapsule(
            text = "本月",
            selected = filterType == "本月",
            onClick = { onFilterChange("本月") }
        )

        FilterCapsule(
            text = customText,
            selected = filterType == "按时间",
            onClick = onMonthClick
        )
    }
}

@Composable
private fun FilterCapsule(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

// =================================================
// 加油记录卡片
// =================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FuelRecordCard(
    record: FuelRecord,
    calculationResult: ConsumptionResult?,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val timeText = remember(record.timestamp) {
        recordDateFormat.format(Date(record.timestamp))
    }

    val paidAmount = record.actualPaidAmount
    val volume = record.volume
    val unitPrice = record.unitPrice
    val odometer = record.odometer

    MiuixCard {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. 顶栏：日期 + 状态标签 + 右侧编辑/删除按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = timeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (record.fuelGrade.isNotBlank()) {
                        TagChip(text = record.fuelGrade, color = MaterialTheme.colorScheme.primary, bgColor = MaterialTheme.colorScheme.primaryContainer)
                    }
                    if (record.isFull) {
                        TagChip(text = "加满", color = Color(0xFF34C759), bgColor = Color(0xFFE8F8EC))
                    }
                    if (record.hasMissedRecord) {
                        TagChip(text = "含漏记", color = Color(0xFFFF9500), bgColor = Color(0xFFFFF4E5))
                    }
                    // 新增：离群与低置信度状态标记
                    if (calculationResult?.isOutlier == true) {
                        TagChip(text = "偏离值", color = Color(0xFFFF3B30), bgColor = Color(0xFFFFE5E5))
                    } else if (calculationResult?.quality?.confidence == IntervalConfidence.LOW ||
                        calculationResult?.quality?.confidence == IntervalConfidence.UNRELIABLE) {
                        TagChip(text = "预估值", color = Color(0xFFFF9500), bgColor = Color(0xFFFFF4E5))
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "编辑",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onEdit() }
                    )
                    Text(
                        text = "删除",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF3B30).copy(alpha = 0.8f),
                        modifier = Modifier.clickable { showDeleteDialog = true }
                    )
                }
            }

            // 2. 核心数据行：左侧金额+单价油量，右侧油耗+行驶里程
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "¥",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = "%.2f".format(paidAmount),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "%.2f L · 单价 %.2f元/L".format(volume, unitPrice),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 右侧：油耗与里程
                Column(horizontalAlignment = Alignment.End) {
                    if (calculationResult != null && calculationResult.distance > 0) {
                        Text(
                            text = "%.2f L/100km".format(calculationResult.consumption),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (calculationResult.isOutlier) Color(0xFFFF3B30) else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (calculationResult.isEstimated) "估算行驶 %.0f km".format(calculationResult.distance)
                            else "行驶 %.0f km".format(calculationResult.distance),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // 排除 null 以及 <= 0 的情况，未登记时显示“未记录里程”
                        val odometerText = record.odometer?.takeIf { it > 0 }?.let { "%.0f km".format(it) } ?: "未记录里程"
                        Text(
                            text = odometerText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 3. 补充细节折叠区
            val hasExtraInfo = (calculationResult != null && odometer != null) ||
                    record.remainingFuel != null ||
                    record.hasMissedRecord ||
                    record.note.isNotBlank()

            if (hasExtraInfo) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (calculationResult != null && odometer != null && odometer > 0) {
                        Text("仪表盘里程: ${odometer.toInt()} km", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    record.remainingFuel?.let { remaining ->
                        Text("加油前剩余: %.2f L".format(remaining), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (record.hasMissedRecord) {
                        Text("漏记数据: 约 ${record.missedOdometer ?: 0.0} km / ${record.missedVolume ?: 0.0} L", fontSize = 11.sp, color = Color(0xFFFF9500))
                    }
                    if (record.note.isNotBlank()) {
                        Text("备注: ${record.note}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        MiuiConfirmDialog(
            title = "删除记录",
            message = "确定删除该条加油记录吗？删除后相关油耗计算将重新推导。",
            confirmText = "删除",
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun TagChip(text: String, color: Color, bgColor: Color) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

// =================================================
// 滚轮选择弹窗组件
// =================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePicker3WheelSheet(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    availableYears: List<Int>,
    onSelect: (year: Int, month: Int, day: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var tempYear by remember { mutableIntStateOf(initialYear) }
    var tempMonth by remember { mutableIntStateOf(initialMonth) }
    var tempDay by remember { mutableIntStateOf(initialDay) }

    val months = remember { (1..12).toList() }

    val maxDays = remember(tempYear, tempMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, tempYear)
            set(Calendar.MONTH, tempMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val daysList = remember(maxDays) {
        listOf(0) + (1..maxDays).toList()
    }

    LaunchedEffect(maxDays) {
        if (tempDay > maxDays) {
            tempDay = maxDays
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                }

                Text(
                    text = "选择时间",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(onClick = {
                    onSelect(tempYear, tempMonth, tempDay)
                }) {
                    Text("确定", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HyperWheelList(
                    selectedValue = tempYear,
                    items = availableYears,
                    modifier = Modifier.weight(1f),
                    itemLabel = { "$it 年" },
                    onChange = { tempYear = it }
                )

                HyperWheelList(
                    selectedValue = tempMonth,
                    items = months,
                    modifier = Modifier.weight(1f),
                    itemLabel = { "%02d 月".format(it) },
                    onChange = { tempMonth = it }
                )

                HyperWheelList(
                    selectedValue = tempDay,
                    items = daysList,
                    modifier = Modifier.weight(1f),
                    itemLabel = { if (it == 0) "全月" else "%02d 日".format(it) },
                    onChange = { tempDay = it }
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
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
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    val initialIndex = remember(items, selectedValue) {
        items.indexOf(selectedValue).coerceAtLeast(0)
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val selectedIndex by remember {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            if (offset > itemHeightPx / 2f) firstVisible + 1 else firstVisible
        }
    }

    LaunchedEffect(selectedValue, items) {
        val targetIndex = items.indexOf(selectedValue).coerceAtLeast(0)
        if (targetIndex in items.indices && targetIndex != selectedIndex) {
            listState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex in items.indices) {
            val item = items[selectedIndex]
            if (item != selectedValue) {
                onChange(item)
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
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
        )

        LazyColumn(
            state = listState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleCount / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            items(items.size) { index ->
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
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// =================================================
// 空状态组件
// =================================================

@Composable
private fun EmptyRecordsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(64.dp)
                .width(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LocalGasStation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .height(32.dp)
                    .width(32.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "暂无加油记录",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "点击首页“记加油”添加新记录",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
@Composable
private fun MiuiConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确定",
    dismissText: String = "取消",
    confirmColor: Color = Color(0xFFFF3B30),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))

                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(Modifier.height(24.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.6.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Text(
                            text = dismissText,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(0.6.dp)
                            .height(30.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )

                    TextButton(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Text(
                            text = confirmText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = confirmColor
                        )
                    }
                }
            }
        }
    }
}
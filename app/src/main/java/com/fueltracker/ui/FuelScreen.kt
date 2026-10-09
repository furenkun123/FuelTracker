package com.fueltracker.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fueltracker.data.FuelRecord
import com.fueltracker.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelScreen(
    viewModel: MainViewModel,
    record: FuelRecord? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val isEditMode = record != null
    val vehicle by viewModel.currentVehicle.collectAsState()
    val tankCapacity = vehicle?.tankCapacity ?: 0.0
    val scope = rememberCoroutineScope()

    // 格式化日期 Formatter 缓存，避免反复重组重复创建
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    val defaultFuelGrade = remember(vehicle, record) {
        val rawGrade = record?.fuelGrade?.takeIf { it.isNotBlank() }
            ?: vehicle?.fuelGrade?.takeIf { it.isNotBlank() }
            ?: "92#"
        val trimmed = rawGrade.trim()
        if (trimmed.endsWith("#") || trimmed.contains("柴油")) trimmed else "${trimmed}#"
    }

    val fuelGradeOptions = remember(defaultFuelGrade) {
        val baseList = mutableListOf("89#", "92#", "95#", "98#", "0# 柴油", "-10# 柴油")
        if (defaultFuelGrade !in baseList) {
            baseList.add(0, defaultFuelGrade)
        }
        baseList
    }

    var fuelGrade by remember(defaultFuelGrade) { mutableStateOf(defaultFuelGrade) }
    var showFuelGradePicker by remember { mutableStateOf(false) }

    // 修复：防止编辑模式传入非法时间戳
    var timestamp by remember(record) {
        mutableLongStateOf(record?.timestamp?.takeIf { it > 0 } ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    var odometer by remember(record) { mutableStateOf(record?.odometer?.let { formatNumber(it) } ?: "") }
    var volume by remember(record) { mutableStateOf(record?.volume?.let { formatNumber(it) } ?: "") }
    var unitPrice by remember(record) { mutableStateOf(record?.unitPrice?.let { formatNumber(it) } ?: "") }
    var totalAmount by remember(record) { mutableStateOf(record?.totalAmount?.let { formatNumber(it) } ?: "") }
    var discount by remember(record) { mutableStateOf(record?.discount?.let { formatNumber(it) } ?: "") }
    var amount by remember(record) { mutableStateOf(record?.actualPaidAmount?.let { formatNumber(it) } ?: "") }
    var remainingFuel by remember(record) { mutableStateOf(record?.remainingFuel?.let { formatNumber(it) } ?: "") }
    var isFull by remember(record) { mutableStateOf(record?.isFull ?: false) }
    var hasMissedRecord by remember(record) { mutableStateOf(record?.hasMissedRecord ?: false) }
    var missedOdometer by remember(record) { mutableStateOf(record?.missedOdometer?.let { formatNumber(it) } ?: "") }
    var missedVolume by remember(record) { mutableStateOf(record?.missedVolume?.let { formatNumber(it) } ?: "") }
    var note by remember(record) { mutableStateOf(record?.note ?: "") }

    // 修复：保存按钮防抖状态
    var isSaving by remember { mutableStateOf(false) }

    // 使用 derivedStateOf 避免每次界面小改动都反复 parse 文本
    val volumeValue by remember(volume) { derivedStateOf { volume.toDoubleOrNull()?.takeIf { it > 0 } } }
    val amountValue by remember(amount) { derivedStateOf { amount.toDoubleOrNull()?.takeIf { it >= 0 } } }
    val unitPriceValue by remember(unitPrice) { derivedStateOf { unitPrice.toDoubleOrNull()?.takeIf { it > 0 } } }
    val remainingFuelValue by remember(remainingFuel) { derivedStateOf { remainingFuel.toDoubleOrNull()?.takeIf { it >= 0 } } }

    // 修复：漏记油量增加 > 0 校验
    val missedOdometerValue by remember(missedOdometer) { derivedStateOf { missedOdometer.toDoubleOrNull()?.takeIf { it > 0 } } }
    val missedVolumeValue by remember(missedVolume) { derivedStateOf { missedVolume.toDoubleOrNull()?.takeIf { it > 0 } } }

    // 判断加油量是否超出油箱容量
    val isVolumeExceedsCapacity by remember(volumeValue, tankCapacity) {
        derivedStateOf { volumeValue != null && tankCapacity > 0.0 && volumeValue!! > tankCapacity }
    }

    val isMissedValid by remember(hasMissedRecord, missedOdometerValue, missedVolumeValue) {
        derivedStateOf { !hasMissedRecord || (missedOdometerValue != null && missedVolumeValue != null) }
    }

    val isVolumeValid by remember(volumeValue, isVolumeExceedsCapacity) {
        derivedStateOf { volumeValue != null && volumeValue!! > 0 && !isVolumeExceedsCapacity }
    }

    val isFormValid by remember(isVolumeValid, amountValue, unitPriceValue, fuelGrade, isMissedValid) {
        derivedStateOf {
            isVolumeValid && amountValue != null &&
                    unitPriceValue != null &&
                    fuelGrade.isNotBlank() && isMissedValid
        }
    }

    // 联动计算逻辑
    fun onVolumeChange(newVol: String) {
        volume = newVol
        val v = newVol.toDoubleOrNull()
        val p = unitPriceValue
        if (v != null && v > 0 && p != null) {
            val total = v * p
            val disc = discount.toDoubleOrNull() ?: 0.0
            totalAmount = formatNumber(total)
            amount = formatNumber((total - disc).coerceAtLeast(0.0))
        }
    }

    fun onUnitPriceChange(newPrice: String) {
        unitPrice = newPrice
        val p = newPrice.toDoubleOrNull()
        val vol = volumeValue
        if (p != null && p > 0 && vol != null) {
            val total = vol * p
            val disc = discount.toDoubleOrNull() ?: 0.0
            totalAmount = formatNumber(total)
            amount = formatNumber((total - disc).coerceAtLeast(0.0))
        }
    }

    fun onAmountChange(newAmt: String) {
        amount = newAmt
        val amt = newAmt.toDoubleOrNull()
        val p = unitPriceValue
        val disc = discount.toDoubleOrNull() ?: 0.0
        if (amt != null && amt >= 0) {
            val total = amt + disc
            totalAmount = formatNumber(total)
            if (p != null && p > 0) {
                volume = formatNumber(total / p)
            }
        }
    }

    fun onTotalAmountChange(newTotal: String) {
        totalAmount = newTotal
        val total = newTotal.toDoubleOrNull()
        val p = unitPriceValue
        val disc = discount.toDoubleOrNull() ?: 0.0
        if (total != null && total >= 0) {
            amount = formatNumber((total - disc).coerceAtLeast(0.0))
            if (p != null && p > 0) {
                volume = formatNumber(total / p)
            }
        }
    }

    fun onDiscountChange(newDisc: String) {
        discount = newDisc
        val disc = newDisc.toDoubleOrNull() ?: 0.0
        val total = totalAmount.toDoubleOrNull()
        val amt = amount.toDoubleOrNull()
        val p = unitPriceValue

        if (total != null) {
            amount = formatNumber((total - disc).coerceAtLeast(0.0))
        } else if (amt != null) {
            val newTotal = amt + disc
            totalAmount = formatNumber(newTotal)
            if (p != null && p > 0) {
                volume = formatNumber(newTotal / p)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "编辑加油记录" else "记加油",
                        fontWeight = FontWeight.Bold,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        val currentVehicle = vehicle
        if (currentVehicle == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                // 修复：添加 IME 和导航栏 Padding，防止键盘遮挡底部按钮
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 车辆信息卡片
            MiuixCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${currentVehicle.brand} ${currentVehicle.series}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = currentVehicle.model,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "油箱容量 ${currentVehicle.tankCapacity} L",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 油品标号
            MiuixCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFuelGradePicker = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "油品标号",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " *",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = fuelGrade,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 输入区域
            MiuixCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "加油时间",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = " *",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { showDatePicker = true }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = remember(timestamp) { dateFormatter.format(Date(timestamp)) },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }

                        MiuixInput(
                            value = odometer,
                            onValueChange = { odometer = it },
                            label = "当前公里数",
                            suffix = "km",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MiuixInput(
                            value = volume,
                            onValueChange = { onVolumeChange(it) },
                            label = "本次加油量",
                            isRequired = true,
                            suffix = "L",
                            isError = isVolumeExceedsCapacity,
                            errorMessage = if (isVolumeExceedsCapacity) "超出容量(${tankCapacity}L)" else null,
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        MiuixInput(
                            value = unitPrice,
                            onValueChange = { onUnitPriceChange(it) },
                            label = "单价",
                            isRequired = true,
                            suffix = "元/L",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MiuixInput(
                            value = totalAmount,
                            onValueChange = { onTotalAmountChange(it) },
                            label = "原价",
                            isRequired = true,
                            suffix = "元",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        MiuixInput(
                            value = discount,
                            onValueChange = { onDiscountChange(it) },
                            label = "优惠金额",
                            suffix = "元",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MiuixInput(
                            value = amount,
                            onValueChange = { onAmountChange(it) },
                            label = "实际支付",
                            isRequired = true,
                            suffix = "元",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        MiuixInput(
                            value = remainingFuel,
                            onValueChange = { remainingFuel = it },
                            label = "加油后剩余",
                            suffix = "L",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            val vVal = volumeValue
            val pVal = unitPriceValue
            if (vVal != null && pVal != null) {
                val total = vVal * pVal
                val disc = discount.toDoubleOrNull() ?: 0.0
                val paid = (total - disc).coerceAtLeast(0.0)
                Text(
                    text = "计算结果：$fuelGrade · %.2f L × %.2f 元/L = %.2f 元，实付 %.2f 元"
                        .format(LocalLocale.current.platformLocale, vVal, pVal, total, paid),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            // 勾选项
            MiuixCard {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    MiuixCheckRow(
                        checked = isFull,
                        text = "本次已加满",
                        onChecked = { checked ->
                            isFull = checked
                            if (checked && tankCapacity > 0) {
                                remainingFuel = formatNumber(tankCapacity)
                            } else if (!checked) {
                                // 修复：取消勾选时清空剩余油量，防止产生矛盾数据
                                remainingFuel = ""
                            }
                        }
                    )
                    MiuixCheckRow(
                        checked = hasMissedRecord,
                        text = "上次加油忘记记录 (漏记)",
                        onChecked = { checked ->
                            hasMissedRecord = checked
                            if (!checked) {
                                missedOdometer = ""
                                missedVolume = ""
                            }
                        }
                    )
                    if (hasMissedRecord) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiuixInput(
                                value = missedOdometer,
                                onValueChange = { missedOdometer = it },
                                label = "漏记期间大概行驶里程",
                                isRequired = true,
                                suffix = "km",
                                keyboardType = KeyboardType.Decimal
                            )
                            MiuixInput(
                                value = missedVolume,
                                onValueChange = { missedVolume = it },
                                label = "漏记期间大概加油量",
                                isRequired = true,
                                suffix = "L",
                                keyboardType = KeyboardType.Decimal
                            )
                        }
                    }
                }
            }

            // 备注
            MiuixCard {
                MiuixInput(
                    value = note,
                    onValueChange = { note = it },
                    label = "备注 (如: 某某加油站/优惠活动)",
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            // 保存按钮
            Button(
                onClick = {
                    if (isSaving) return@Button
                    isSaving = true

                    scope.launch {
                        try {
                            val odo = odometer.toDoubleOrNull()?.takeIf { it > 0 }
                            val vol = volumeValue ?: return@launch
                            val amt = amountValue ?: return@launch
                            val price = unitPriceValue ?: return@launch

                            val recordToSave = if (isEditMode) {
                                record.copy(
                                    timestamp = timestamp,
                                    fuelGrade = fuelGrade,
                                    odometer = odo,
                                    volume = vol,
                                    totalAmount = totalAmount.toDoubleOrNull(),
                                    discount = discount.toDoubleOrNull(),
                                    actualPaidAmount = amt,
                                    unitPrice = price,
                                    remainingFuel = remainingFuelValue,
                                    isFull = isFull,
                                    hasMissedRecord = hasMissedRecord,
                                    missedOdometer = missedOdometerValue,
                                    missedVolume = missedVolumeValue,
                                    note = note.trim()
                                )
                            } else {
                                FuelRecord(
                                    vehicleId = currentVehicle.id,
                                    timestamp = timestamp,
                                    fuelGrade = fuelGrade,
                                    odometer = odo,
                                    volume = vol,
                                    totalAmount = totalAmount.toDoubleOrNull(),
                                    discount = discount.toDoubleOrNull(),
                                    actualPaidAmount = amt,
                                    unitPrice = price,
                                    remainingFuel = remainingFuelValue,
                                    isFull = isFull,
                                    hasMissedRecord = hasMissedRecord,
                                    missedOdometer = missedOdometerValue,
                                    missedVolume = missedVolumeValue,
                                    note = note.trim()
                                )
                            }

                            if (isEditMode) {
                                viewModel.updateFuelRecord(recordToSave)
                            } else {
                                viewModel.addFuelRecord(recordToSave)
                            }
                            onSaved()
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = isFormValid && !isSaving,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    text = if (isEditMode) "保存修改" else "保存加油记录",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        // 浮层：油品选择器
        if (showFuelGradePicker) {
            HyperFuelGradePickerDialog(
                currentValue = fuelGrade,
                options = fuelGradeOptions,
                onSelected = {
                    fuelGrade = it
                    showFuelGradePicker = false
                },
                onDismiss = { showFuelGradePicker = false }
            )
        }

        // 浮层：日期选择器
        if (showDatePicker) {
            HyperDatePickerDialog(
                initialTimestamp = timestamp,
                onSelected = { selectedTimestamp ->
                    timestamp = selectedTimestamp
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }
    }
}

// ============================================================
// 通用输入组件
// ============================================================

@Composable
private fun MiuixCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
private fun MiuixInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = modifier.padding(horizontal = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isRequired) {
                Text(
                    text = " *",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = isError,
            shape = RoundedCornerShape(14.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            suffix = {
                suffix?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor = MaterialTheme.colorScheme.error,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

@Composable
private fun MiuixCheckRow(checked: Boolean, text: String, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ============================================================
// 工具函数
// ============================================================

private fun formatNumber(value: Double): String {
    return if (value <= 0.0) "" else String.format(Locale.US, "%.2f", value)
}

// ============================================================
// 油品标号选择弹窗
// ============================================================

@Composable
private fun HyperFuelGradePickerDialog(
    currentValue: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedGrade by remember { mutableStateOf(currentValue) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "取消",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(8.dp)
                    )
                    Text(
                        text = "选择油品标号",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "确定",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onSelected(selectedGrade) }
                            .padding(8.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                HyperWheelList(
                    selectedValue = selectedGrade,
                    items = options,
                    visibleCount = 5,
                    modifier = Modifier.fillMaxWidth(),
                    onChange = { selectedGrade = it }
                )
            }
        }
    }
}

// ============================================================
// MIUI 风格月历
// ============================================================

private data class DayCell(
    val year: Int,
    val month: Int,
    val day: Int,
    val inCurrentMonth: Boolean
)

private fun buildMonthGrid(year: Int, month: Int): List<DayCell> {
    val cal = Calendar.getInstance().apply { set(year, month - 1, 1) }
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1  // 0 = 周日
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val cells = mutableListOf<DayCell>()

    val prevCal = cal.clone() as Calendar
    prevCal.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek)
    repeat(firstDayOfWeek) {
        cells.add(
            DayCell(
                prevCal.get(Calendar.YEAR),
                prevCal.get(Calendar.MONTH) + 1,
                prevCal.get(Calendar.DAY_OF_MONTH),
                false
            )
        )
        prevCal.add(Calendar.DAY_OF_MONTH, 1)
    }

    for (d in 1..daysInMonth) {
        cells.add(DayCell(year, month, d, true))
    }

    return cells
}

@Composable
private fun MiuiCalendar(
    displayYear: Int,
    displayMonth: Int,
    selectedYear: Int,
    selectedMonth: Int,
    selectedDay: Int,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (year: Int, month: Int, day: Int) -> Unit,
    onTitleClick: () -> Unit
) {
    val cells = remember(displayYear, displayMonth) {
        buildMonthGrid(displayYear, displayMonth)
    }

    val today = remember {
        Calendar.getInstance().let {
            Triple(
                it.get(Calendar.YEAR),
                it.get(Calendar.MONTH) + 1,
                it.get(Calendar.DAY_OF_MONTH)
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevMonth, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "上个月",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${displayYear}年${displayMonth}月",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTitleClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            IconButton(onClick = onNextMonth, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "下个月",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { w ->
                Box(
                    modifier = Modifier.weight(1f).height(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = w,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val rowCount = (cells.size + 6) / 7
        for (row in 0 until rowCount) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    if (index >= cells.size) {
                        Box(modifier = Modifier.weight(1f).height(40.dp))
                        continue
                    }

                    val cell = cells[index]
                    val isSelected = cell.year == selectedYear &&
                            cell.month == selectedMonth &&
                            cell.day == selectedDay
                    val isToday = cell.year == today.first &&
                            cell.month == today.second &&
                            cell.day == today.third

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelectDay(cell.year, cell.month, cell.day) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .then(
                                    if (isToday && !isSelected)
                                        Modifier.border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${cell.day}",
                                fontSize = 14.sp,
                                fontWeight = if (isSelected || isToday)
                                    FontWeight.Bold
                                else
                                    FontWeight.Normal,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    !cell.inCurrentMonth ->
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    isToday -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// 年月选择弹窗
// ============================================================

@Composable
private fun HyperYearMonthPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    minYear: Int,
    maxYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var year by remember { mutableIntStateOf(initialYear) }
    var month by remember { mutableIntStateOf(initialMonth) }

    val years = remember(minYear, maxYear) { (minYear..maxYear).toList() }
    val months = remember { (1..12).toList() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "选择年月",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HyperWheelList(
                        selectedValue = year,
                        items = years,
                        visibleCount = 5,
                        modifier = Modifier.weight(1.4f),
                        itemLabel = { "${it}年" },
                        onChange = { year = it }
                    )
                    HyperWheelList(
                        selectedValue = month,
                        items = months,
                        visibleCount = 5,
                        modifier = Modifier.weight(1f),
                        itemLabel = { "%02d 月".format(it) },
                        onChange = { month = it }
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "取消",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(
                        onClick = { onConfirm(year, month) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "确定",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// 日期 + 时间选择弹窗
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HyperDatePickerDialog(
    initialTimestamp: Long,
    onSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialCalendar = remember(initialTimestamp) {
        Calendar.getInstance().apply { timeInMillis = initialTimestamp }
    }

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val minYear = currentYear - 2
    val maxYear = currentYear + 10

    var selectedYear by remember { mutableIntStateOf(initialCalendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(initialCalendar.get(Calendar.MONTH) + 1) }
    var selectedDay by remember { mutableIntStateOf(initialCalendar.get(Calendar.DAY_OF_MONTH)) }

    var displayYear by remember { mutableIntStateOf(initialCalendar.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableIntStateOf(initialCalendar.get(Calendar.MONTH) + 1) }

    var hour by remember { mutableIntStateOf(initialCalendar.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(initialCalendar.get(Calendar.MINUTE)) }

    val hours = remember { (0..23).toList() }
    val minutes = remember { (0..59).toList() }

    var showYearMonthDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun prevMonth() {
        if (displayMonth == 1) {
            if (displayYear - 1 < minYear) return
            displayYear -= 1
            displayMonth = 12
        } else {
            displayMonth -= 1
        }
    }

    fun nextMonth() {
        if (displayMonth == 12) {
            if (displayYear + 1 > maxYear) return
            displayYear += 1
            displayMonth = 1
        } else {
            displayMonth += 1
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "取消",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "选择加油时间",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        clear()
                        set(selectedYear, selectedMonth - 1, selectedDay, hour, minute, 0)
                    }
                    onSelected(cal.timeInMillis)
                }) {
                    Text(
                        text = "确定",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            MiuiCalendar(
                displayYear = displayYear,
                displayMonth = displayMonth,
                selectedYear = selectedYear,
                selectedMonth = selectedMonth,
                selectedDay = selectedDay,
                onPrevMonth = { prevMonth() },
                onNextMonth = { nextMonth() },
                onSelectDay = { y, m, d ->
                    selectedYear = y
                    selectedMonth = m
                    selectedDay = d
                    displayYear = y
                    displayMonth = m
                },
                onTitleClick = { showYearMonthDialog = true }
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(modifier = Modifier.weight(0.6f))
                HyperWheelList(
                    selectedValue = hour,
                    items = hours,
                    visibleCount = 3,
                    modifier = Modifier.weight(1f),
                    itemLabel = { "%02d 时".format(it) },
                    onChange = { hour = it }
                )
                HyperWheelList(
                    selectedValue = minute,
                    items = minutes,
                    visibleCount = 3,
                    modifier = Modifier.weight(1f),
                    itemLabel = { "%02d 分".format(it) },
                    onChange = { minute = it }
                )
                Spacer(modifier = Modifier.weight(0.6f))
            }
        }

        if (showYearMonthDialog) {
            HyperYearMonthPickerDialog(
                initialYear = displayYear,
                initialMonth = displayMonth,
                minYear = minYear,
                maxYear = maxYear,
                onDismiss = { showYearMonthDialog = false },
                onConfirm = { y, m ->
                    displayYear = y
                    displayMonth = m
                    showYearMonthDialog = false
                }
            )
        }
    }
}

// ============================================================
// 滚轮选择
// ============================================================

@SuppressLint("FrequentlyChangingValue")
@Composable
private fun <T> HyperWheelList(
    selectedValue: T,
    items: List<T>,
    modifier: Modifier = Modifier,
    visibleCount: Int = 3,
    itemLabel: (T) -> String = { it.toString() },
    onChange: (T) -> Unit
) {
    if (items.isEmpty()) return

    val itemHeight = 38.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    val initialIndex = remember(items, selectedValue) {
        items.indexOf(selectedValue).coerceAtLeast(0)
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (!isScrolling) {
                    val firstVisible = listState.firstVisibleItemIndex
                    val offset = listState.firstVisibleItemScrollOffset
                    val centerIndex = if (offset > itemHeightPx / 2f) firstVisible + 1 else firstVisible
                    if (centerIndex in items.indices && items[centerIndex] != selectedValue) {
                        onChange(items[centerIndex])
                    }
                }
            }
    }

    // 修复：当外部 selectedValue 改变时同步滚轮位置，使用 animateScrollToItem 并仅在非滚动时触发
    LaunchedEffect(selectedValue, items) {
        val targetIndex = items.indexOf(selectedValue).coerceAtLeast(0)
        if (targetIndex in items.indices && !listState.isScrollInProgress) {
            scope.launch {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleCount).fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleCount / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            items(items.size) { index ->
                val isSelected = index == (listState.firstVisibleItemIndex +
                        if (listState.firstVisibleItemScrollOffset > itemHeightPx / 2f) 1 else 0)
                val item = items[index]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = itemLabel(item),
                        fontSize = if (isSelected) 16.sp else 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}
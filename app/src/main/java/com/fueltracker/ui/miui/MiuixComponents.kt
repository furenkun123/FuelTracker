package com.fueltracker.ui.miui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs

// =========================================================
// MIUI 主题颜色定义（全项目公开可用）
// =========================================================
val MiuiPrimary = Color(0xFF007AFF)
val MiuiBackground = Color(0xFFF2F2F7)
val MiuiTextPrimary = Color(0xFF000000)

val MuiXBgColor = Color(0xFFF4F4F6)
val MuiXCardColor = Color(0xFFFFFFFF)
val MuiXPrimaryColor = Color(0xFF3482FF)
val MuiXTextPrimary = Color(0xFF191919)
val MuiXTextSecondary = Color(0xFF8C8C8C)
val MuiXDividerColor = Color(0xFFF0F0F3)
val MuiXSelectionBg = Color(0xFFF2F3F7)

// =========================================================
// 基础输入框与复选框组件
// =========================================================
@Composable
fun MiuixInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    singleLine: Boolean = true,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label?.let { { Text(it) } },
        trailingIcon = suffix?.let { { Text(it, fontSize = 14.sp, color = Color.Gray) } },
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        isError = isError,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MiuiPrimary,
            focusedLabelColor = MiuiPrimary
        )
    )
}

@Composable
fun MiuixCheckRow(
    checked: Boolean,
    text: String,
    onChecked: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChecked,
            colors = CheckboxDefaults.colors(
                checkedColor = MiuiPrimary
            )
        )
        Text(
            text = text,
            color = MiuiTextPrimary,
            fontSize = 15.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

// =========================================================
// 卡片布局与表单列表组件
// =========================================================

@Composable
fun MuiXSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MuiXTextSecondary,
        modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
    )
}

@Composable
fun MuiXGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MuiXCardColor
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            content()
        }
    }
}

@Composable
fun MuiXInputItem(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = MuiXTextPrimary,
            modifier = Modifier.width(110.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = MuiXTextPrimary,
                fontWeight = FontWeight.Medium
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 15.sp,
                            color = MuiXTextSecondary.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun MuiXClickableItem(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = MuiXTextPrimary,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value.ifBlank { "请选择" },
            fontSize = 15.sp,
            color = if (value.isBlank()) MuiXTextSecondary.copy(alpha = 0.6f) else MuiXPrimaryColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

// =========================================================
// 通用 BottomSheet 弹窗与滚轮选择器组件
// =========================================================

/**
 * 通用底部弹窗容器组件（带 取消 / 确认 按钮）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuiXBottomSheetDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MuiXCardColor,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = MuiXTextSecondary, fontSize = 15.sp)
                }
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MuiXTextPrimary)
                TextButton(onClick = onConfirm) {
                    Text("确定", color = MuiXPrimaryColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            content()
        }
    }
}

/**
 * 独立滚轮单列选择组件（修复对齐与点击居中 Bug）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MuiXWheelPickerColumn(
    options: List<String>,
    currentValue: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 44.dp,
    visibleCount: Int = 5
) {
    if (options.isEmpty()) return

    val initialIndex = remember(options, currentValue) {
        options.indexOf(currentValue).coerceAtLeast(0)
    }

    val paddingItems = visibleCount / 2
    val coroutineScope = rememberCoroutineScope()
    // 当 padding 被按 itemHeight 拆分后，Option initialIndex 正好对应 listState 的 initialFirstVisibleItemIndex
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val totalHeight = itemHeight * visibleCount

    // 计算当前处于居中位置的 Option 索引
    val selectedIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) initialIndex
            else {
                val centerOffset = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val centeredItem = visibleItems.minByOrNull {
                    abs((it.offset + it.size / 2) - centerOffset)
                }
                // 扣除顶部的 paddingItems 个 Spacer 项
                (centeredItem?.index?.minus(paddingItems))?.coerceIn(0, options.lastIndex) ?: initialIndex
            }
        }
    }

    Box(
        modifier = modifier.height(totalHeight),
        contentAlignment = Alignment.Center
    ) {
        // 1. 中间圆角高亮背景
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(itemHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(MuiXSelectionBg)
        )

        // 2. 单层滚动列表
        LazyColumn(
            state = listState,
            flingBehavior = snapFlingBehavior,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 顶部填充项（拆分为 paddingItems 个独立 Item）
            items(paddingItems) {
                Spacer(modifier = Modifier.height(itemHeight))
            }

            // 选项列表
            itemsIndexed(options) { index, item ->
                val isSelected = index == selectedIndex

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable {
                            coroutineScope.launch {
                                // 点击时平滑滚动到该项居中
                                listState.animateScrollToItem(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        fontSize = if (isSelected) 17.sp else 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MuiXTextPrimary else MuiXTextSecondary
                    )
                }
            }

            // 底部填充项
            items(paddingItems) {
                Spacer(modifier = Modifier.height(itemHeight))
            }
        }

        // 3. 滚动停止后自动同步选中值
        LaunchedEffect(listState.isScrollInProgress) {
            if (!listState.isScrollInProgress) {
                val safeIndex = selectedIndex.coerceIn(0, options.lastIndex)
                if (options[safeIndex] != currentValue) {
                    onValueChange(options[safeIndex])
                }
            }
        }
    }
}

/**
 * 单列滚轮选择器 BottomSheet 弹窗
 */
@Composable
fun MuiXWheelPickerBottomSheet(
    title: String,
    options: List<String>,
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedValue by remember { mutableStateOf(currentValue.ifBlank { options.firstOrNull() ?: "" }) }

    MuiXBottomSheetDialog(
        title = title,
        onDismiss = onDismiss,
        onConfirm = { onConfirm(selectedValue) }
    ) {
        MuiXWheelPickerColumn(
            options = options,
            currentValue = selectedValue,
            onValueChange = { selectedValue = it },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 日期选择器 BottomSheet 弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuiXDatePickerBottomSheet(
    initialMillis: Long?,
    onDismiss: () -> Unit,
    onDateSelected: (Long?) -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    onDismiss()
                }
            ) {
                Text("确定", color = MuiXPrimaryColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MuiXTextSecondary)
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
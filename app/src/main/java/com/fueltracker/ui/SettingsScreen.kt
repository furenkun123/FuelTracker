package com.fueltracker.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.fueltracker.BuildConfig
import com.fueltracker.data.Vehicle
import com.fueltracker.ui.theme.ThemeMode
import com.fueltracker.ui.theme.ThemePreferences
import com.fueltracker.util.BackupManager
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ============================================================
// 动画规格
// ============================================================

private const val ExpandDurationMs = 380
private const val ScrollDelayMs: Long = ExpandDurationMs.toLong() + 40L

private val ExpandSpec = expandVertically(
    animationSpec = tween(durationMillis = ExpandDurationMs, easing = FastOutSlowInEasing)
)

private val ShrinkSpec = shrinkVertically(
    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
)

private val FadeInSpec = fadeIn(
    animationSpec = tween(durationMillis = 260, delayMillis = 80)
)

private val FadeOutSpec = fadeOut(
    animationSpec = tween(durationMillis = 180)
)

// ============================================================
// LazyColumn 稳定 Key
// ============================================================

private const val KeyDataSection = "section_data"
private const val KeyThemeSection = "section_theme"
private const val KeyAboutSection = "section_about"

// ============================================================
// SettingsScreen
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vehicles: List<Vehicle>,
    onSetCurrentVehicle: (Vehicle) -> Unit,
    onDeleteVehicle: (Vehicle) -> Unit,
    onAddVehicle: () -> Unit,
    onEditVehicle: (Vehicle) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var vehicleToDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedRestoreUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    var isDataExpanded by rememberSaveable { mutableStateOf(false) }
    var isThemeExpanded by rememberSaveable { mutableStateOf(false) }
    var isAboutExpanded by rememberSaveable { mutableStateOf(false) }
    var isInstructionsExpanded by rememberSaveable { mutableStateOf(false) }

    var currentThemeMode by remember { mutableStateOf(ThemePreferences.mode) }
    var isBackupOperationRunning by rememberSaveable { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // 导出备份
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        isBackupOperationRunning = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                BackupManager.writeBackupToUri(context, uri)
                withContext(Dispatchers.Main) {
                    isBackupOperationRunning = false
                    Toast.makeText(context, "备份导出成功", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isBackupOperationRunning = false
                    Toast.makeText(context, "导出失败: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 导入备份
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        if (isJsonFile(context, uri)) {
            selectedRestoreUri = uri
        } else {
            Toast.makeText(context, "请选择 JSON 格式的备份文件", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶栏
        TopAppBar(
            title = {
                Text(
                    text = "设置与数据",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
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

        // 内容区
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 车辆管理标题栏
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "车辆管理",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        TextButton(
                            onClick = onAddVehicle,
                            enabled = !isBackupOperationRunning
                        ) {
                            Text(
                                text = "＋ 添加车辆",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }

                    if (vehicles.isEmpty()) {
                        MiuiCardLocal {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无车辆信息，请添加车辆",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // 车辆列表
            items(
                items = vehicles,
                key = { it.id },
                contentType = { "vehicle" }
            ) { vehicle ->
                VehicleItemCard(
                    vehicle = vehicle,
                    onSelect = {
                        if (!isBackupOperationRunning) {
                            onSetCurrentVehicle(vehicle)
                        }
                    },
                    onEdit = {
                        if (!isBackupOperationRunning) {
                            onEditVehicle(vehicle)
                        }
                    },
                    onDelete = {
                        if (!isBackupOperationRunning) {
                            vehicleToDeleteId = vehicle.id
                        }
                    }
                )
            }

            // 数据管理
            item(key = KeyDataSection) {
                ExpandableSettingCard(
                    icon = Icons.Default.Folder,
                    title = "备份与恢复",
                    subtitle = "导出或导入本地 JSON 数据备份文件",
                    expanded = isDataExpanded,
                    enabled = !isBackupOperationRunning,
                    onExpandedChange = { isDataExpanded = !isDataExpanded }
                ) {
                    SettingActionRow(
                        icon = Icons.Default.FileUpload,
                        title = "导出本地备份",
                        subtitle = "将车辆及所有加油记录导出为 JSON 文件",
                        enabled = !isBackupOperationRunning,
                        onClick = {
                            exportLauncher.launch(BackupManager.generateFileName())
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        thickness = 0.6.dp
                    )

                    SettingActionRow(
                        icon = Icons.Default.FileDownload,
                        title = "恢复本地备份",
                        subtitle = "从本地备份文件中恢复数据并覆盖当前记录",
                        enabled = !isBackupOperationRunning,
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "*/*"))
                        }
                    )

                    if (isBackupOperationRunning) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            thickness = 0.6.dp
                        )
                        ProcessingRow()
                    }
                }
            }

            // 主题设置
            item(key = KeyThemeSection) {
                ExpandableSettingCard(
                    icon = Icons.Default.Brightness6,
                    title = "主题",
                    subtitle = currentThemeMode.displayName,
                    expanded = isThemeExpanded,
                    enabled = !isBackupOperationRunning,
                    onExpandedChange = { isThemeExpanded = !isThemeExpanded }
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = currentThemeMode == mode

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isBackupOperationRunning) {
                                    ThemePreferences.setMode(context, mode)
                                    currentThemeMode = mode
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = mode.displayName,
                                fontSize = 14.sp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                            )

                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "已选中",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.size(18.dp))
                            }
                        }

                        if (mode != ThemeMode.entries.last()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                thickness = 0.6.dp,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }

            // 关于应用
            item(key = KeyAboutSection) {
                ExpandableSettingCard(
                    icon = Icons.Default.Info,
                    title = "关于应用",
                    subtitle = "版本信息、使用说明、更新",
                    expanded = isAboutExpanded,
                    enabled = !isBackupOperationRunning,
                    onExpandedChange = {
                        isAboutExpanded = !isAboutExpanded
                    }
                ) {

                    // 关键：恢复原来的左右内边距
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        // ==============================
                        // 应用说明
                        // ==============================

                        Column {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        enabled = !isBackupOperationRunning
                                    ) {
                                        isInstructionsExpanded =
                                            !isInstructionsExpanded
                                    }
                                    .padding(vertical = 2.dp),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween,
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "应用说明",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color =
                                            MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(
                                        modifier = Modifier.height(2.dp)
                                    )

                                    Text(
                                        text = "初次使用建议先阅读使用说明与功能指南",
                                        fontSize = 12.sp,
                                        color =
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Icon(
                                    imageVector =
                                        if (isInstructionsExpanded) {
                                            Icons.Default.KeyboardArrowUp
                                        } else {
                                            Icons.Default.KeyboardArrowDown
                                        },
                                    contentDescription =
                                        if (isInstructionsExpanded) {
                                            "收起"
                                        } else {
                                            "展开"
                                        },
                                    tint =
                                        MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = isInstructionsExpanded,
                                enter = ExpandSpec + FadeInSpec,
                                exit = ShrinkSpec + FadeOutSpec
                            ) {

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            top = 10.dp,
                                            bottom = 4.dp
                                        ),
                                    verticalArrangement =
                                        Arrangement.spacedBy(10.dp)
                                ) {
                                    MiuiInstructionItem(1, "应用完全离线，一定注意备份")
                                    MiuiInstructionItem(2, "完全依赖免费AI提供代码，所以开源。有兴趣的自己改，没发现明显Bug")
                                    MiuiInstructionItem(3, "平均油耗使用多个逻辑相互计算减小误差，以“满箱到满箱”为基本计算单元")
                                    MiuiInstructionItem(4, "车型列表从2008款起，应该比较全")
                                    MiuiInstructionItem(5, "支持多车辆切换，暗色主题")
                                    MiuiInstructionItem(6, "代码在不断找ai优化，不定时会更新")
                                }
                            }
                        }

                        HorizontalDivider(
                            color =
                                MaterialTheme.colorScheme.surfaceVariant,
                            thickness = 0.6.dp
                        )

                        // ==============================
                        // 查看更新
                        // ==============================

                        AboutRow(
                            title = "查看更新",
                            subtitle = "跳转网盘查看与下载最新安装包",
                            icon = Icons.Default.CloudDownload,
                            enabled = !isBackupOperationRunning,
                            onClick = {
                                val url =
                                    "https://yun.139.com/shareweb/#/w/i/2xTrJEeQhKJ01"

                                try {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            url.toUri()
                                        )
                                    )
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        context,
                                        "无法打开网盘链接",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )

                        HorizontalDivider(
                            color =
                                MaterialTheme.colorScheme.surfaceVariant,
                            thickness = 0.6.dp
                        )

                        // ==============================
                        // GitHub
                        // ==============================

                        AboutRow(
                            title = "GitHub",
                            subtitle =
                                "https://github.com/furenkun123/FuelTracker",
                            icon = Icons.Default.Code,
                            enabled = !isBackupOperationRunning,
                            onClick = {
                                val url =
                                    "https://github.com/furenkun123/FuelTracker"

                                try {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            url.toUri()
                                        )
                                    )
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        context,
                                        "无法打开链接",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )

                        HorizontalDivider(
                            color =
                                MaterialTheme.colorScheme.surfaceVariant,
                            thickness = 0.6.dp
                        )

                        // ==============================
                        // 版本号
                        // ==============================

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text = "版本号",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color =
                                    MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text =
                                    "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color =
                                    MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // 自动滚动处理
    AutoScrollOnExpand(isDataExpanded, KeyDataSection, listState)
    AutoScrollOnExpand(isThemeExpanded, KeyThemeSection, listState)
    AutoScrollOnExpand(isAboutExpanded, KeyAboutSection, listState)
    AutoScrollOnExpand(isAboutExpanded && isInstructionsExpanded, KeyAboutSection, listState)

    // 删除车辆确认弹窗
    vehicles.firstOrNull { it.id == vehicleToDeleteId }?.let { vehicle ->
        MiuiConfirmDialog(
            title = "确认删除车辆？",
            message = buildString {
                append("删除“${vehicle.brand} ${vehicle.model}”将同步删除该车辆下的所有加油记录，且不可恢复。")
                if (vehicle.isCurrent) {
                    append("\n注意：这是当前正在使用的车辆。")
                }
            },
            confirmText = "删除",
            onConfirm = {
                vehicleToDeleteId = null
                onDeleteVehicle(vehicle)
            },
            onDismiss = { vehicleToDeleteId = null }
        )
    }

    // 恢复备份确认弹窗
    selectedRestoreUri?.let { uri ->
        MiuiConfirmDialog(
            title = "确认恢复备份？",
            message = "恢复备份将覆盖当前的所有车辆和加油记录，此操作无法撤销。是否继续？",
            confirmText = "覆盖并恢复",
            onConfirm = {
                selectedRestoreUri = null
                isBackupOperationRunning = true
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        BackupManager.restoreFromUri(context, uri)
                        withContext(Dispatchers.Main) {
                            isBackupOperationRunning = false
                            Toast.makeText(context, "数据恢复成功！", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            isBackupOperationRunning = false
                            Toast.makeText(context, "数据恢复失败: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onDismiss = { selectedRestoreUri = null }
        )
    }
}

// ============================================================
// 可展开设置卡片
// ============================================================

@Composable
private fun ExpandableSettingCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    expanded: Boolean,
    enabled: Boolean = true,
    onExpandedChange: () -> Unit,
    content: @Composable () -> Unit
) {
    MiuiCardLocal {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled, onClick = onExpandedChange)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = ExpandSpec + FadeInSpec,
                exit = ShrinkSpec + FadeOutSpec
            ) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        thickness = 0.6.dp
                    )
                    content()
                }
            }
        }
    }
}

// ============================================================
// 自动滚动
// ============================================================

@Composable
private fun AutoScrollOnExpand(
    expanded: Boolean,
    sectionKey: String,
    listState: LazyListState
) {
    val density = androidx.compose.ui.platform.LocalDensity.current

    LaunchedEffect(expanded, sectionKey) {
        if (!expanded) return@LaunchedEffect

        delay(ScrollDelayMs.milliseconds)

        val layoutInfo = listState.layoutInfo
        val item = layoutInfo.visibleItemsInfo.firstOrNull { it.key == sectionKey } ?: return@LaunchedEffect

        val overshoot = item.offset + item.size - layoutInfo.viewportEndOffset
        if (overshoot <= 0) return@LaunchedEffect

        val keepHeaderPx = with(density) { 48.dp.toPx() }
        val maxScroll = (item.offset - layoutInfo.viewportStartOffset + keepHeaderPx).coerceAtLeast(0f)
        val scrollBy = overshoot.toFloat().coerceIn(0f, maxScroll)

        if (scrollBy > 0f) {
            listState.animateScrollBy(
                value = scrollBy,
                animationSpec = tween(
                    durationMillis = 350,
                    easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
                )
            )
        }
    }
}

// ============================================================
// JSON 文件判断
// ============================================================

private fun isJsonFile(context: Context, uri: Uri): Boolean {
    val displayName = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) {
            cursor.getString(index)
        } else {
            null
        }
    }

    return displayName == null || displayName.endsWith(".json", ignoreCase = true)
}

// ============================================================
// MIUI 卡片
// ============================================================

@Composable
private fun MiuiCardLocal(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        content()
    }
}

// ============================================================
// 车辆卡片
// ============================================================

@Composable
private fun VehicleItemCard(
    vehicle: Vehicle,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    MiuiCardLocal(modifier = Modifier.clickable(onClick = onSelect)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (vehicle.isCurrent) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = if (vehicle.isCurrent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listOf(vehicle.brand, vehicle.series, vehicle.model)
                        .filter { it.isNotBlank() }
                        .joinToString(" "),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${vehicle.year}款 | ${vehicle.fuelType} (${vehicle.fuelGrade}) | 油箱 ${vehicle.tankCapacity}L",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (vehicle.isCurrent) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "当前使用",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑车辆",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "删除车辆",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// 设置操作行
// ============================================================

@Composable
private fun SettingActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "›",
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

// ============================================================
// 处理中的提示
// ============================================================

@Composable
private fun ProcessingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "正在处理数据，请稍候…",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================
// About 行
// ============================================================

@Composable
private fun AboutRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ============================================================
// 使用说明
// ============================================================

@Composable
private fun MiuiInstructionItem(
    index: Int,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$index",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 21.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

// ============================================================
// MIUI 确认弹窗
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MiuiConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确定",
    dismissText: String = "取消",
    confirmColor: Color? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val effectiveConfirmColor = confirmColor ?: MaterialTheme.colorScheme.error
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var actionTriggered by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = dismissText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(
                    onClick = {
                        if (actionTriggered) return@TextButton
                        actionTriggered = true
                        onConfirm()
                    },
                    enabled = !actionTriggered,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = effectiveConfirmColor.copy(alpha = 0.12f)
                    )
                ) {
                    Text(
                        text = confirmText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = effectiveConfirmColor
                    )
                }
            }
        }
    }
}
package com.fueltracker.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.fueltracker.data.Vehicle
import com.fueltracker.ui.miui.MiuiCard
import com.fueltracker.ui.miui.MiuiColors
import com.fueltracker.ui.miui.MiuiLinkButton
import com.fueltracker.ui.miui.MiuiTopBar
import com.fueltracker.util.BackupManager
import kotlinx.coroutines.launch
import com.fueltracker.BuildConfig
import androidx.compose.foundation.layout.heightIn

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

    var vehicleToDelete by remember { mutableStateOf<Vehicle?>(null) }
    var selectedRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isDataExpanded by remember { mutableStateOf(false) }       // 数据管理折叠状态
    var isAboutExpanded by remember { mutableStateOf(false) }      // 关于折叠状态
    var showInstructionsDialog by remember { mutableStateOf(false) } // 应用说明弹窗状态

    // 导出备份文件选择器
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    BackupManager.writeBackupToUri(context, uri)
                    Toast.makeText(context, "备份导出成功", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "导出失败: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 导入备份文件选择器
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedRestoreUri = uri
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // MIUI 顶栏
        MiuiTopBar(
            title = "设置与数据",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MiuiColors.Text
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ================= 车辆管理区域 =================
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "车辆管理",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MiuiColors.Text
                        )
                        MiuiLinkButton(
                            text = "＋ 添加车辆",
                            onClick = onAddVehicle
                        )
                    }

                    if (vehicles.isEmpty()) {
                        MiuiCard(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无车辆信息，请添加车辆",
                                    color = MiuiColors.SecondaryText,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            items(vehicles, key = { it.id }) { vehicle ->
                VehicleItemCard(
                    vehicle = vehicle,
                    onSelect = { onSetCurrentVehicle(vehicle) },
                    onEdit = { onEditVehicle(vehicle) },
                    onDelete = { vehicleToDelete = vehicle }
                )
            }

            // ================= 数据备份与恢复区域 (可折叠) =================
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "数据管理",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MiuiColors.Text,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    MiuiCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            // 折叠/展开 头部栏
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isDataExpanded = !isDataExpanded }
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
                                            .background(MiuiColors.Primary.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = MiuiColors.Primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = "备份与恢复",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MiuiColors.Text
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "导出或导入本地 JSON 数据备份文件",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MiuiColors.SecondaryText
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = if (isDataExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isDataExpanded) "收起" else "展开",
                                    tint = MiuiColors.SecondaryText
                                )
                            }

                            // 展开内容区域：导出备份 / 恢复备份
                            AnimatedVisibility(
                                visible = isDataExpanded,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MiuiColors.SecondaryText.copy(alpha = 0.12f)
                                    )

                                    SettingActionRow(
                                        icon = Icons.Default.FileUpload,
                                        title = "导出本地备份",
                                        subtitle = "将车辆及所有加油记录导出为 JSON 文件",
                                        onClick = {
                                            exportLauncher.launch(BackupManager.generateFileName())
                                        }
                                    )

                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MiuiColors.SecondaryText.copy(alpha = 0.12f)
                                    )

                                    SettingActionRow(
                                        icon = Icons.Default.FileDownload,
                                        title = "恢复本地备份",
                                        subtitle = "从本地备份文件中恢复数据并覆盖当前记录",
                                        onClick = {
                                            importLauncher.launch(arrayOf("application/json", "*/*"))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ================= 关于应用区域 (可折叠) =================
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "关于",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MiuiColors.Text,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    MiuiCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            // 折叠/展开 头部栏
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isAboutExpanded = !isAboutExpanded }
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
                                            .background(MiuiColors.Primary.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MiuiColors.Primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = "关于应用",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MiuiColors.Text
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "版本信息、使用说明、网盘更新与开发者联系",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MiuiColors.SecondaryText
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = if (isAboutExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isAboutExpanded) "收起" else "展开",
                                    tint = MiuiColors.SecondaryText
                                )
                            }

                            // 展开内容区域
                            AnimatedVisibility(
                                visible = isAboutExpanded,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MiuiColors.SecondaryText.copy(alpha = 0.12f)
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {

                                        // 2. 应用说明(点击弹出说明对话框)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showInstructionsDialog = true }
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "应用说明",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MiuiColors.Text
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "初次使用建议先阅读使用说明与功能指南",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MiuiColors.SecondaryText
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Description,
                                                contentDescription = "应用说明",
                                                tint = MiuiColors.Primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        HorizontalDivider(color = MiuiColors.SecondaryText.copy(alpha = 0.08f))

                                        // 3. 查看更新（网盘链接）
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val cloudDriveUrl = "https://yun.139.com/shareweb/#/w/i/2xTrJEeQhKJ01"
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, cloudDriveUrl.toUri())
                                                        context.startActivity(intent)
                                                    } catch (_: Exception) {
                                                        Toast.makeText(context, "无法打开网盘链接", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "查看更新",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MiuiColors.Text
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "跳转网盘查看与下载最新安装包",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MiuiColors.SecondaryText
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.CloudDownload,
                                                contentDescription = "查看更新",
                                                tint = MiuiColors.Primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        HorizontalDivider(color = MiuiColors.SecondaryText.copy(alpha = 0.08f))



                                        // 5. GitHub
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val githubUrl = "https://github.com/furenkun123/FuelTracker"
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, githubUrl.toUri())
                                                        context.startActivity(intent)
                                                    } catch (_: Exception) {
                                                        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "GitHub",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MiuiColors.Text
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "https://github.com/furenkun123/FuelTracker",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MiuiColors.SecondaryText
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Code,
                                                contentDescription = "GitHub",
                                                tint = MiuiColors.Primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        // 4. 联系我（跳转指定网址）
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val contactUrl = "https://qm.qq.com/q/MEBXIAFnSm"
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, contactUrl.toUri())
                                                        context.startActivity(intent)
                                                    } catch (_: Exception) {
                                                        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "联系我",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MiuiColors.Text
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "QQ与我联系",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MiuiColors.SecondaryText
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "联系我",
                                                tint = MiuiColors.Primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        HorizontalDivider(color = MiuiColors.SecondaryText.copy(alpha = 0.08f))
                                        // 1. 版本号
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "版本号",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MiuiColors.Text
                                            )
                                            Text(
                                                text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MiuiColors.Primary
                                            )
                                        }

                                        HorizontalDivider(color = MiuiColors.SecondaryText.copy(alpha = 0.08f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ================= 应用说明弹窗 =================
    if (showInstructionsDialog) {
        AlertDialog(
            onDismissRequest = { showInstructionsDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "应用说明",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    InstructionItem(
                        index = "1",
                        text = "应用全离线使用，一定注意备份"
                    )
                    InstructionItem(
                        index = "2",
                        text = "完全依赖免费 AI 提供代码，所以直接 GitHub 开源。有兴趣的自己改，使用应该没明显 Bug"
                    )
                    InstructionItem(
                        index = "3",
                        text = "平均油耗使用多个逻辑算法，加满和漏记也参与计算"
                    )
                    InstructionItem(
                        index = "4",
                        text = "车型信息爬取筛选后还有 3000 多车系，可能有遗漏"
                    )
                    InstructionItem(
                        index = "5",
                        text = "支持多车辆切换"

                    )

                }
            },
            confirmButton = {
                TextButton(onClick = { showInstructionsDialog = false }) {
                    Text("我知道了", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 删除确认对话框
    vehicleToDelete?.let { vehicle ->
        AlertDialog(
            onDismissRequest = { vehicleToDelete = null },
            title = {
                Text(
                    text = "确认删除车辆？",
                    fontWeight = FontWeight.Bold,
                    color = MiuiColors.Text
                )
            },
            text = {
                Text(
                    text = "删除“${vehicle.brand} ${vehicle.model}”将同步删除该车辆下的所有加油记录，且不可恢复。",
                    color = MiuiColors.SecondaryText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteVehicle(vehicle)
                        vehicleToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToDelete = null }) {
                    Text("取消", color = MiuiColors.SecondaryText)
                }
            }
        )
    }

    // 恢复确认对话框
    selectedRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { selectedRestoreUri = null },
            title = {
                Text(
                    text = "确认恢复备份？",
                    fontWeight = FontWeight.Bold,
                    color = MiuiColors.Text
                )
            },
            text = {
                Text(
                    text = "恢复备份将覆盖当前的所有车辆和加油记录，此操作无法撤销。是否继续？",
                    color = MiuiColors.SecondaryText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedRestoreUri = null
                        coroutineScope.launch {
                            try {
                                BackupManager.restoreFromUri(context, uri)
                                Toast.makeText(context, "数据恢复成功！", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "数据恢复失败: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("覆盖并恢复", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRestoreUri = null }) {
                    Text("取消", color = MiuiColors.SecondaryText)
                }
            }
        )
    }
}

// 车辆列表卡片组件
@Composable
private fun VehicleItemCard(
    vehicle: Vehicle,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    MiuiCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
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
                        if (vehicle.isCurrent) MiuiColors.Primary.copy(alpha = 0.12f)
                        else MiuiColors.SecondaryText.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = if (vehicle.isCurrent) MiuiColors.Primary else MiuiColors.SecondaryText,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${vehicle.brand} ${vehicle.series} ${vehicle.model}".trim(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MiuiColors.Text
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${vehicle.year}款 | ${vehicle.fuelType} (${vehicle.fuelGrade}) | 油箱 ${vehicle.tankCapacity}L",
                    style = MaterialTheme.typography.bodySmall,
                    color = MiuiColors.SecondaryText
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
                        color = MiuiColors.Primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "当前使用",
                            style = MaterialTheme.typography.labelSmall,
                            color = MiuiColors.Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑车辆",
                            tint = MiuiColors.SecondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "删除车辆",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// 设置数据行组件
@Composable
private fun SettingActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                    .background(MiuiColors.Primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MiuiColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuiColors.Text
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MiuiColors.SecondaryText
                )
            }
        }

        Text(
            text = "›",
            style = MaterialTheme.typography.titleLarge,
            color = MiuiColors.SecondaryText,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
@Composable
private fun InstructionItem(
    index: String,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // MD3 风格的序号徽章
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
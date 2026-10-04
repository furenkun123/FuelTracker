package com.fueltracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.data.CarDatabase
import com.fueltracker.data.Trim

// ============================================================
// 车系
// ============================================================
@Composable
fun SeriesSelectorScreen(
    brand: String,
    onBack: () -> Unit,
    onSelected: (String) -> Unit,
    onManualInput: (String) -> Unit = onSelected
) {
    val db = CarDatabase.get(LocalContext.current)
    var loading by remember { mutableStateOf(!db.isCatalogReady()) }
    var all by remember { mutableStateOf<List<String>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var manualDraft by remember { mutableStateOf("") }

    LaunchedEffect(brand) {
        db.ensureCatalogLoaded()
        all = db.series(brand)
        loading = false
    }

    val filtered = remember(search, all) {
        if (search.isBlank()) all
        else all.filter { it.contains(search, ignoreCase = true) }
    }

    PickerScaffold(
        title = "选择车系",
        subtitle = brand,
        onBack = onBack,
        search = search,
        onSearchChange = { search = it },
        placeholder = "搜索车系...",
        loading = loading,
        empty = false,
        emptyHint = ""
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            item(key = "manual_series_header") {
                ManualInputCard(
                    title = "手动输入车系",
                    placeholder = "未找到车系？在此直接输入",
                    value = manualDraft,
                    onValueChange = { manualDraft = it },
                    onConfirm = { onManualInput(it) }
                )
            }

            if (filtered.isNotEmpty()) {
                item(key = "preset_header") {
                    PresetHeader("预置车系")
                }
                items(filtered, key = { it }) { s ->
                    PickerRow(text = s, onClick = { onSelected(s) })
                }
            } else if (search.isNotBlank()) {
                item(key = "no_match") {
                    NoMatchText("未找到匹配的车系")
                }
            }
        }
    }
}

// ============================================================
// 年款
// ============================================================
@Composable
fun YearSelectorScreen(
    brand: String,
    series: String,
    onBack: () -> Unit,
    onSelected: (String) -> Unit,
    onManualInput: (String) -> Unit = onSelected
) {
    val db = CarDatabase.get(LocalContext.current)
    var loading by remember { mutableStateOf(!db.isCatalogReady()) }
    var years by remember { mutableStateOf<List<String>>(emptyList()) }
    var manualDraft by remember { mutableStateOf("") }

    LaunchedEffect(brand, series) {
        db.ensureCatalogLoaded()
        years = db.years(brand, series)
        loading = false
    }

    PickerScaffold(
        title = "选择年款",
        subtitle = "$brand · $series",
        onBack = onBack,
        search = "",
        onSearchChange = {},
        placeholder = "",
        showSearch = false,
        loading = loading,
        empty = false,
        emptyHint = ""
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            item(key = "manual_year_header") {
                ManualInputCard(
                    title = "手动输入年款",
                    placeholder = "未找到年款？如：2024款",
                    value = manualDraft,
                    onValueChange = { manualDraft = it },
                    onConfirm = { onManualInput(it) }
                )
            }

            if (years.isNotEmpty()) {
                item(key = "preset_header") {
                    PresetHeader("预置年款")
                }
                items(years, key = { it }) { y ->
                    PickerRow(text = y, onClick = { onSelected(y) })
                }
            }
        }
    }
}

// ============================================================
// 车型
// ============================================================
@Composable
fun TrimSelectorScreen(
    brand: String,
    series: String,
    year: String,
    onBack: () -> Unit,
    onSelected: (Trim) -> Unit,
    onManualInput: (String) -> Unit = { name -> onSelected(Trim(name = name)) }
) {
    val db = CarDatabase.get(LocalContext.current)
    var loading by remember { mutableStateOf(!db.isCatalogReady()) }
    var all by remember { mutableStateOf<List<Trim>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var manualDraft by remember { mutableStateOf("") }

    LaunchedEffect(brand, series, year) {
        db.ensureCatalogLoaded()
        all = db.trims(brand, series, year)
        loading = false
    }

    val filtered = remember(search, all) {
        if (search.isBlank()) all
        else all.filter { it.name.contains(search, ignoreCase = true) }
    }

    PickerScaffold(
        title = "选择车型",
        subtitle = "$brand · $series · $year",
        onBack = onBack,
        search = search,
        onSearchChange = { search = it },
        placeholder = "搜索车型...",
        loading = loading,
        empty = false,
        emptyHint = ""
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            item(key = "manual_trim_header") {
                ManualInputCard(
                    title = "手动输入车型",
                    placeholder = "未找到车型？在此直接输入",
                    value = manualDraft,
                    onValueChange = { manualDraft = it },
                    onConfirm = { onManualInput(it) }
                )
            }

            if (filtered.isNotEmpty()) {
                item(key = "preset_header") {
                    PresetHeader("预置车型")
                }
                items(filtered, key = { it.name }) { t ->
                    TrimRow(trim = t, onClick = { onSelected(t) })
                }
            } else if (search.isNotBlank()) {
                item(key = "no_match") {
                    NoMatchText("未找到匹配车型")
                }
            }
        }
    }
}

// ============================================================
// 通用组件
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    search: String,
    onSearchChange: (String) -> Unit,
    placeholder: String,
    loading: Boolean,
    empty: Boolean,
    emptyHint: String,
    showSearch: Boolean = true,
    content: @Composable () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (showSearch) {
                PickerSearchBox(search, onSearchChange, placeholder)
            }
            when {
                loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                empty -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyHint,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
                else -> content()
            }
        }
    }
}

@Composable
private fun ManualInputCard(
    title: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 10.dp),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    }
                }
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { if (value.isNotBlank()) onConfirm(value.trim()) },
                enabled = value.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(38.dp)
            ) {
                Text(text = "确定", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PresetHeader(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun NoMatchText(text: String) {
    Box(
        Modifier.fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PickerRow(text: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceVariant,
            thickness = 0.8.dp,
            modifier = Modifier.padding(start = 20.dp)
        )
    }
}

@Composable
private fun TrimRow(trim: Trim, onClick: () -> Unit) {
    val info = buildString {
        if (trim.energyType.isNotBlank()) append(trim.energyType)
        if (trim.fuelGrade.isNotBlank()) {
            if (isNotEmpty()) append(" · ")
            append(trim.fuelGrade)
        }
        if (trim.fuelTank.isNotBlank()) {
            if (isNotEmpty()) append(" · ")
            append("${trim.fuelTank}L")
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    text = trim.name,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (info.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = info,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceVariant,
            thickness = 0.8.dp,
            modifier = Modifier.padding(start = 20.dp)
        )
    }
}

@Composable
private fun PickerSearchBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    }
                }
            )
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
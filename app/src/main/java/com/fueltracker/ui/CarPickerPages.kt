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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.data.CarDatabase
import com.fueltracker.data.Trim
import com.fueltracker.ui.miui.MuiXCardColor

// ============================================================
// 车系
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
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
        empty = false,   // 空也照常渲染, 让手输卡片一直可见
        emptyHint = ""
    ) {
        LazyColumn(Modifier.fillMaxSize()) {

            // ==========================================
            // ★ 顶部: 手动输入车系卡片 (常驻可见)
            // ==========================================
            item(key = "manual_series_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MuiXCardColor)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "手动输入车系",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3482FF)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BasicTextField(
                            value = manualDraft,
                            onValueChange = { manualDraft = it },
                            textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF191919)),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEBECEF))
                                .padding(horizontal = 10.dp),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (manualDraft.isEmpty()) {
                                        Text(
                                            text = "未找到车系？在此直接输入",
                                            fontSize = 13.sp,
                                            color = Color(0xFF8C8C8C)
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (manualDraft.isNotBlank()) {
                                    onManualInput(manualDraft.trim())
                                }
                            },
                            enabled = manualDraft.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3482FF)
                            ),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text(text = "确定", fontSize = 13.sp)
                        }
                    }
                }
            }

            // ==========================================
            // 预置车系列表 (有数据才显示)
            // ==========================================
            if (filtered.isNotEmpty()) {
                item(key = "preset_header") {
                    Text(
                        text = "预置车系",
                        fontSize = 12.sp,
                        color = Color(0xFF8C8C8C),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
                items(filtered, key = { it }) { s ->
                    PickerRow(text = s, onClick = { onSelected(s) })
                }
            } else if (search.isNotBlank()) {
                item(key = "no_match") {
                    Box(
                        Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "未找到匹配的车系",
                            fontSize = 14.sp,
                            color = Color(0xFF8C8C8C)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// 年款
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
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
        empty = false,           // ★ 保持 false, 空列表也让卡片渲染
        emptyHint = ""
    ) {
        LazyColumn(Modifier.fillMaxSize()) {

            // ============================================
            // 手动输入年款卡片 (常驻顶部)
            // ============================================
            item(key = "manual_year_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MuiXCardColor)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "手动输入年款",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3482FF)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BasicTextField(
                            value = manualDraft,
                            onValueChange = { manualDraft = it },
                            textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF191919)),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEBECEF))
                                .padding(horizontal = 10.dp),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (manualDraft.isEmpty()) {
                                        Text(
                                            text = "未找到年款？如：2024款",
                                            fontSize = 13.sp,
                                            color = Color(0xFF8C8C8C)
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (manualDraft.isNotBlank()) {
                                    onManualInput(manualDraft.trim())
                                }
                            },
                            enabled = manualDraft.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3482FF)
                            ),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text(text = "确定", fontSize = 13.sp)
                        }
                    }
                }
            }

            // ============================================
            // 预置年款列表
            // ============================================
            if (years.isNotEmpty()) {
                item(key = "preset_header") {
                    Text(
                        text = "预置年款",
                        fontSize = 12.sp,
                        color = Color(0xFF8C8C8C),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
                items(years, key = { it }) { y ->
                    PickerRow(text = y, onClick = { onSelected(y) })
                }
            }
        }
    }
}

// ============================================================
// 具体车型（带油箱/能源/燃油标号）
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
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
        empty = false,               // ★ 一定要是 false，否则空列表时会盖掉整个 content
        emptyHint = ""
    ) {
        LazyColumn(Modifier.fillMaxSize()) {

            // ============================================
            // 手动输入车型卡片 (常驻顶部)
            // ============================================
            item(key = "manual_trim_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MuiXCardColor)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "手动输入车型",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3482FF)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BasicTextField(
                            value = manualDraft,
                            onValueChange = { manualDraft = it },
                            textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF191919)),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEBECEF))
                                .padding(horizontal = 10.dp),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (manualDraft.isEmpty()) {
                                        Text(
                                            text = "未找到车型？在此直接输入",
                                            fontSize = 13.sp,
                                            color = Color(0xFF8C8C8C)
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (manualDraft.isNotBlank()) {
                                    onManualInput(manualDraft.trim())
                                }
                            },
                            enabled = manualDraft.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3482FF)
                            ),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text(text = "确定", fontSize = 13.sp)
                        }
                    }
                }
            }

            // ============================================
            // 预置车型列表
            // ============================================
            if (filtered.isNotEmpty()) {
                item(key = "preset_header") {
                    Text(
                        text = "预置车型",
                        fontSize = 12.sp,
                        color = Color(0xFF8C8C8C),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
                items(filtered, key = { it.name }) { t ->
                    TrimRow(trim = t, onClick = { onSelected(t) })
                }
            } else if (search.isNotBlank()) {
                item(key = "no_match") {
                    Box(
                        Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "未找到匹配车型",
                            fontSize = 14.sp,
                            color = Color(0xFF8C8C8C)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// 通用骨架
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
        containerColor = Color(0xFFF4F4F6),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, fontSize = 18.sp,
                            fontWeight = FontWeight.Bold, color = Color(0xFF191919))
                        if (subtitle.isNotBlank()) {
                            Text(subtitle, fontSize = 12.sp,
                                color = Color(0xFF8C8C8C),
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color(0xFF191919))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF4F4F6))
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (showSearch) {
                PickerSearchBox(search, onSearchChange, placeholder)
            }
            when {
                loading -> Box(Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF3482FF))
                }
                empty -> Box(Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center) {
                    Text(emptyHint, color = Color(0xFF8C8C8C), fontSize = 14.sp)
                }
                else -> content()
            }
        }
    }
}

@Composable
private fun PickerRow(text: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(MuiXCardColor)) {
        Box(Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(text, fontSize = 16.sp, color = Color(0xFF191919))
        }
        HorizontalDivider(color = Color(0xFFF0F0F3), thickness = 0.8.dp,
            modifier = Modifier.padding(start = 20.dp))
    }
}

@Composable
private fun TrimRow(trim: Trim, onClick: () -> Unit) {
    val info = buildString {
        if (trim.energyType.isNotBlank()) append(trim.energyType)
        if (trim.fuelGrade.isNotBlank()) {
            if (isNotEmpty()) append(" · "); append(trim.fuelGrade)
        }
        if (trim.fuelTank.isNotBlank()) {
            if (isNotEmpty()) append(" · "); append("${trim.fuelTank}L")
        }
    }
    Column(Modifier.fillMaxWidth().background(MuiXCardColor)) {
        Box(Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)) {
            Column {
                Text(trim.name, fontSize = 15.sp, color = Color(0xFF191919),
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (info.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(info, fontSize = 12.sp, color = Color(0xFF8C8C8C))
                }
            }
        }
        HorizontalDivider(color = Color(0xFFF0F0F3), thickness = 0.8.dp,
            modifier = Modifier.padding(start = 20.dp))
    }
}

@Composable
private fun PickerSearchBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Box(
        Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(Color(0xFFEBECEF))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Search, null,
                tint = Color(0xFF8C8C8C), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF191919)),
                singleLine = true,
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) Text(placeholder, fontSize = 14.sp,
                            color = Color(0xFF8C8C8C))
                        inner()
                    }
                }
            )
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") },
                    modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Clear, "清除",
                        tint = Color(0xFF8C8C8C), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
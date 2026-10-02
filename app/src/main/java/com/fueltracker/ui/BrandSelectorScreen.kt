package com.fueltracker.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.data.CarBrand
import com.fueltracker.data.CarDatabase
import com.fueltracker.ui.miui.MuiXCardColor
import kotlinx.coroutines.launch

// MIUI 配色定义
private val MuiXBgColor = Color(0xFFF4F4F6)
private val MuiXPrimaryColor = Color(0xFF3482FF)
private val MuiXTextPrimary = Color(0xFF191919)
val MuiXTextSecondary = Color(0xFF8C8C8C)
private val MuiXDividerColor = Color(0xFFF0F0F3)
private val MuiXSearchBg = Color(0xFFEBECEF)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BrandSelectorScreen(
    initialBrand: String,
    onBack: () -> Unit,
    onSelected: (String) -> Unit,
    onManualInput: (String) -> Unit
) {
    val context = LocalContext.current
    val carDatabase = remember { CarDatabase.get(context) }

    var allBrands by remember { mutableStateOf<List<CarBrand>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchText by remember { mutableStateOf("") }
    var customBrandInput by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        carDatabase.ensureCatalogLoaded()
        // 从 catalog 展开成你现有的 CarBrand 列表格式
        val list = carDatabase.brandsByLetter().flatMap { (L, brands) ->
            brands.map { CarBrand(it, L) }
        }
        allBrands = list
        isLoading = false
    }

    // 2. 搜索逻辑过滤
    val filteredBrands = remember(searchText, allBrands) {
        val list = if (searchText.isBlank()) {
            allBrands
        } else {
            allBrands.filter {
                it.brand.contains(searchText, ignoreCase = true) ||
                        it.letter.contains(searchText, ignoreCase = true)
            }
        }
        list.groupBy { it.letter.uppercase() }.toSortedMap()
    }

    val letters = remember(filteredBrands) { filteredBrands.keys.toList() }

    // 3. 计算每个字母 Header 在 LazyColumn 中的真实位置
    // ★ 关键修改：因为顶部增加了一个手动输入的 item，所以初始索引为 1
    val letterIndexMap = remember(filteredBrands) {
        val map = mutableMapOf<String, Int>()
        var currentIndex = 1
        filteredBrands.forEach { (letter, brands) ->
            map[letter] = currentIndex
            currentIndex += 1 + brands.size // 1 个字母 Header + N 个品牌 Item
        }
        map
    }

    Scaffold(
        containerColor = MuiXBgColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "选择品牌",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MuiXTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MuiXTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MuiXBgColor)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MuiXPrimaryColor)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // 左侧主内容区
                Column(modifier = Modifier.weight(1f)) {
                    // MIUI 风格搜索框
                    MuiXSearchBox(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = "搜索汽车品牌..."
                    )

                    if (filteredBrands.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = "未找到相关品牌",
                                    fontSize = 15.sp,
                                    color = MuiXTextSecondary
                                )

                                if (searchText.isNotBlank()) {
                                    Button(
                                        onClick = { onManualInput(searchText.trim()) },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MuiXPrimaryColor)
                                    ) {
                                        Text(text = "使用手动输入 \"$searchText\"")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // ==========================================
                            // ★ 1. 放在字母 A 上方的手动输入卡片
                            // ==========================================
                            item(key = "manual_input_header") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MuiXCardColor)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "手动输入品牌",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MuiXPrimaryColor
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        BasicTextField(
                                            value = customBrandInput,
                                            onValueChange = { customBrandInput = it },
                                            textStyle = TextStyle(
                                                fontSize = 14.sp,
                                                color = MuiXTextPrimary
                                            ),
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MuiXSearchBg)
                                                .padding(horizontal = 10.dp),
                                            decorationBox = { innerTextField ->
                                                Box(contentAlignment = Alignment.CenterStart) {
                                                    if (customBrandInput.isEmpty()) {
                                                        Text(
                                                            text = "未找到品牌？在此直接输入",
                                                            fontSize = 13.sp,
                                                            color = MuiXTextSecondary
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                if (customBrandInput.isNotBlank()) {
                                                    onManualInput(customBrandInput.trim())
                                                }
                                            },
                                            enabled = customBrandInput.isNotBlank(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MuiXPrimaryColor),
                                            modifier = Modifier.height(38.dp)
                                        ) {
                                            Text(text = "确定", fontSize = 13.sp)
                                        }
                                    }
                                }
                            }

                            // ==========================================
                            // 2. 按字母 A-Z 分组的品牌列表
                            // ==========================================
                            filteredBrands.forEach { (letter, brands) ->
                                // 字母分组标题（支持吸顶）
                                stickyHeader(key = letter) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MuiXBgColor)
                                            .padding(horizontal = 20.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = letter,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MuiXPrimaryColor
                                        )
                                    }
                                }

                                items(
                                    items = brands,
                                    key = { it.brand }
                                ) { brand ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MuiXCardColor)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onSelected(brand.brand) }
                                                .padding(horizontal = 20.dp, vertical = 14.dp)
                                        ) {
                                            Text(
                                                text = brand.brand,
                                                fontSize = 16.sp,
                                                color = if (brand.brand == initialBrand) MuiXPrimaryColor else MuiXTextPrimary,
                                                fontWeight = if (brand.brand == initialBrand) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                        HorizontalDivider(
                                            color = MuiXDividerColor,
                                            thickness = 0.8.dp,
                                            modifier = Modifier.padding(start = 20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 右侧 A-Z 快速索引栏
                if (letters.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .width(28.dp)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        letters.forEach { letter ->
                            Text(
                                text = letter,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MuiXTextSecondary,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        letterIndexMap[letter]?.let { targetIndex ->
                                            scope.launch {
                                                listState.scrollToItem(targetIndex)
                                            }
                                        }
                                    }
                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * MIUI 风格圆角搜索输入框
 */
@Composable
private fun MuiXSearchBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(MuiXSearchBg)
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
                tint = MuiXTextSecondary,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = MuiXTextPrimary
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 14.sp,
                                color = MuiXTextSecondary
                            )
                        }
                        innerTextField()
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
                        tint = MuiXTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
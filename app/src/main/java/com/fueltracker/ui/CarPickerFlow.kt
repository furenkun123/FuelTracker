package com.fueltracker.ui

import androidx.compose.runtime.*
import com.fueltracker.data.Trim

/** picker 起点: 从哪一级开始重新选 */
enum class PickerLevel { BRAND, SERIES, YEAR, TRIM }

sealed interface CarSelection {
    /** 只选到品牌 */
    data class Manual(val brand: String) : CarSelection

    /** 品牌+车系 (手输车系时) */
    data class BrandSeries(
        val brand: String,
        val series: String
    ) : CarSelection

    /** 品牌+车系+年款 (手输年款时) */
    data class BrandSeriesYear(
        val brand: String,
        val series: String,
        val year: String
    ) : CarSelection

    /** 完整选到具体车型 */
    data class Full(
        val brand: String,
        val series: String,
        val year: String,
        val trim: Trim
    ) : CarSelection
}

@Composable
fun CarPickerFlow(
    startLevel: PickerLevel = PickerLevel.BRAND,
    initialBrand: String = "",
    initialSeries: String = "",
    initialYear: String = "",
    onBack: () -> Unit,
    onPicked: (CarSelection) -> Unit
) {
    var brand by remember {
        mutableStateOf(if (startLevel != PickerLevel.BRAND) initialBrand else "")
    }
    var series by remember {
        mutableStateOf(
            if (startLevel == PickerLevel.YEAR || startLevel == PickerLevel.TRIM) initialSeries else ""
        )
    }
    var year by remember {
        mutableStateOf(if (startLevel == PickerLevel.TRIM) initialYear else "")
    }
    var level by remember { mutableStateOf(startLevel) }

    fun goBackTo(prev: PickerLevel) {
        level = prev
        when (prev) {
            PickerLevel.BRAND -> { brand = ""; series = ""; year = "" }
            PickerLevel.SERIES -> { series = ""; year = "" }
            PickerLevel.YEAR -> { year = "" }
            PickerLevel.TRIM -> { }
        }
    }

    when (level) {
        PickerLevel.BRAND -> BrandSelectorScreen(
            initialBrand = initialBrand,
            onBack = onBack,
            onSelected = {
                brand = it; series = ""; year = ""
                level = PickerLevel.SERIES
            },
            onManualInput = { onPicked(CarSelection.Manual(it)) }
        )

        PickerLevel.SERIES -> SeriesSelectorScreen(
            brand = brand,
            onBack = {
                if (startLevel == PickerLevel.BRAND) goBackTo(PickerLevel.BRAND)
                else onBack()
            },
            onSelected = {
                series = it; year = ""
                level = PickerLevel.YEAR
            },
            onManualInput = { onPicked(CarSelection.BrandSeries(brand, it)) }
        )

        PickerLevel.YEAR -> YearSelectorScreen(
            brand = brand, series = series,
            onBack = {
                if (startLevel == PickerLevel.BRAND || startLevel == PickerLevel.SERIES)
                    goBackTo(PickerLevel.SERIES)
                else onBack()
            },
            onSelected = {
                year = it
                level = PickerLevel.TRIM
            },
            // ★ 手输年款: 直接结束, 只带品牌+车系+年款
            onManualInput = { onPicked(CarSelection.BrandSeriesYear(brand, series, it)) }
        )

        PickerLevel.TRIM -> TrimSelectorScreen(
            brand = brand, series = series, year = year,
            onBack = {
                if (startLevel == PickerLevel.TRIM) onBack()
                else goBackTo(PickerLevel.YEAR)
            },
            onSelected = { onPicked(CarSelection.Full(brand, series, year, it)) },
            onManualInput = { name ->
                onPicked(CarSelection.Full(brand, series, year, Trim(name = name)))
            }
        )
    }
}
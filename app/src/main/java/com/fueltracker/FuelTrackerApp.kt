package com.fueltracker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fueltracker.data.FuelRecord
import com.fueltracker.data.Vehicle
import com.fueltracker.ui.FuelScreen
import com.fueltracker.ui.HomeScreen
import com.fueltracker.ui.InitialRecordScreen
import com.fueltracker.ui.RecordsScreen
import com.fueltracker.ui.SettingsScreen
import com.fueltracker.ui.StatisticsScreen
import com.fueltracker.ui.VehicleScreen
import com.fueltracker.ui.miui.MiuiNavigationBar
import com.fueltracker.ui.miui.MiuiNavigationItem
import com.fueltracker.viewmodel.MainViewModel

@Composable
fun FuelTrackerApp(
    viewModel: MainViewModel
) {
    var screen by remember {
        mutableStateOf(AppScreen.HOME)
    }

    var editingRecord by remember {
        mutableStateOf<FuelRecord?>(null)
    }

    var editingVehicle by remember {
        mutableStateOf<Vehicle?>(null)
    }

    // ADD_VEHICLE 有两个入口（首页 / 设置页），
    // 按返回时回到进入前的位置，而不是一律回首页。
    var addVehicleOrigin by remember {
        mutableStateOf(AppScreen.HOME)
    }

    // 按返回键时的"上一级"映射。
    // 只有当前不在首页时才拦截。
    BackHandler(enabled = screen != AppScreen.HOME) {
        when (screen) {
            AppScreen.RECORDS,
            AppScreen.STATISTICS -> {
                screen = AppScreen.HOME
            }

            AppScreen.ADD_VEHICLE -> {
                screen = addVehicleOrigin
            }

            AppScreen.INITIAL_RECORD -> {
                // 车辆已保存，返回不再回 ADD_VEHICLE（避免重复保存）
                screen = AppScreen.HOME
            }

            AppScreen.ADD_FUEL -> {
                screen = AppScreen.HOME
            }

            AppScreen.EDIT_FUEL -> {
                editingRecord = null
                screen = AppScreen.RECORDS
            }

            AppScreen.EDIT_VEHICLE -> {
                editingVehicle = null
                screen = AppScreen.SETTINGS
            }

            AppScreen.SETTINGS -> {
                screen = AppScreen.HOME
            }

            AppScreen.HOME -> {
                // 不会到这里（enabled 条件已排除），写出来是为了穷举
            }
        }
    }

    val showBottomBar = screen == AppScreen.HOME
            || screen == AppScreen.RECORDS
            || screen == AppScreen.STATISTICS

    if (showBottomBar) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                MiuiNavigationBar {
                    MiuiNavigationItem(
                        selected = screen == AppScreen.HOME,
                        onClick = { screen = AppScreen.HOME },
                        icon = "⌂",
                        label = "首页"
                    )

                    MiuiNavigationItem(
                        selected = screen == AppScreen.RECORDS,
                        onClick = { screen = AppScreen.RECORDS },
                        icon = "▤",
                        label = "记录"
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (screen) {
                    AppScreen.HOME -> {
                        val vehicle by viewModel.currentVehicle.collectAsState()
                        val records by viewModel.records.collectAsState()
                        val latest by viewModel.latestConsumption.collectAsState()
                        val average by viewModel.averageConsumption.collectAsState()

                        HomeScreen(
                            vehicle = vehicle,
                            records = records,
                            latestConsumption = latest,
                            averageConsumption = average,
                            onAddVehicle = {
                                addVehicleOrigin = AppScreen.HOME
                                screen = AppScreen.ADD_VEHICLE
                            },
                            onAddFuel = { screen = AppScreen.ADD_FUEL },
                            onOpenSettings = { screen = AppScreen.SETTINGS },
                            onViewStatistics = { screen = AppScreen.STATISTICS }
                        )
                    }

                    AppScreen.RECORDS -> {
                        RecordsScreen(
                            viewModel = viewModel,
                            onBack = { screen = AppScreen.HOME },
                            onEdit = { record ->
                                editingRecord = record
                                screen = AppScreen.EDIT_FUEL
                            }
                        )
                    }

                    AppScreen.STATISTICS -> {
                        val vehicle by viewModel.currentVehicle.collectAsState()
                        val records by viewModel.records.collectAsState()

                        StatisticsScreen(
                            vehicle = vehicle,
                            records = records,
                            onBack = { screen = AppScreen.HOME }
                        )
                    }

                    else -> {
                        screen = AppScreen.HOME
                    }
                }
            }
        }
    } else {
        when (screen) {
            AppScreen.ADD_VEHICLE -> {
                VehicleScreen(
                    viewModel = viewModel,
                    vehicle = null,
                    onBack = { screen = addVehicleOrigin },
                    onSaved = { screen = AppScreen.INITIAL_RECORD }
                )
            }

            AppScreen.INITIAL_RECORD -> {
                InitialRecordScreen(
                    viewModel = viewModel,
                    onCompleted = { screen = AppScreen.HOME },
                    onBack = { screen = AppScreen.HOME }
                )
            }

            AppScreen.ADD_FUEL -> {
                FuelScreen(
                    viewModel = viewModel,
                    record = null,
                    onBack = { screen = AppScreen.HOME },
                    onSaved = { screen = AppScreen.HOME }
                )
            }

            AppScreen.EDIT_FUEL -> {
                val record = editingRecord

                if (record == null) {
                    screen = AppScreen.RECORDS
                } else {
                    FuelScreen(
                        viewModel = viewModel,
                        record = record,
                        onBack = {
                            editingRecord = null
                            screen = AppScreen.RECORDS
                        },
                        onSaved = {
                            editingRecord = null
                            screen = AppScreen.RECORDS
                        }
                    )
                }
            }

            AppScreen.EDIT_VEHICLE -> {
                val vehicle = editingVehicle

                if (vehicle == null) {
                    screen = AppScreen.SETTINGS
                } else {
                    VehicleScreen(
                        viewModel = viewModel,
                        vehicle = vehicle,
                        onBack = {
                            editingVehicle = null
                            screen = AppScreen.SETTINGS
                        },
                        onSaved = {
                            editingVehicle = null
                            screen = AppScreen.SETTINGS
                        }
                    )
                }
            }

            AppScreen.SETTINGS -> {
                val vehicles by viewModel.vehicles.collectAsState(initial = emptyList())

                SettingsScreen(
                    vehicles = vehicles,
                    onSetCurrentVehicle = { vehicle ->
                        viewModel.setCurrentVehicle(vehicle)
                    },
                    onDeleteVehicle = { vehicle ->
                        viewModel.deleteVehicle(vehicle)
                    },
                    onAddVehicle = {
                        addVehicleOrigin = AppScreen.SETTINGS
                        screen = AppScreen.ADD_VEHICLE
                    },
                    onEditVehicle = { vehicle ->
                        editingVehicle = vehicle
                        screen = AppScreen.EDIT_VEHICLE
                    },
                    onBack = { screen = AppScreen.HOME }
                )
            }

            else -> {
                screen = AppScreen.HOME
            }
        }
    }
}
enum class AppScreen {
    HOME,
    ADD_VEHICLE,
    INITIAL_RECORD,
    ADD_FUEL,
    RECORDS,
    STATISTICS,
    EDIT_FUEL,
    EDIT_VEHICLE,
    SETTINGS
}
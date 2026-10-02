package com.fueltracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fueltracker.data.AppDatabase
import com.fueltracker.data.FuelRecord
import com.fueltracker.data.Vehicle
import com.fueltracker.util.FuelCalculator
import com.fueltracker.util.StatisticsSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val vehicleDao = database.vehicleDao()
    private val fuelDao = database.fuelDao()

    // 内存即时临时状态（用于消除切车/加车时的 UI 延迟）
    private val _currentVehicleOverride = MutableStateFlow<Vehicle?>(null)

    // StateFlow 统一订阅超时策略（切后台 5 秒后切断 DB 监听，节省系统资源）
    private val stopTimeout = 5_000L

    // 所有已添加的车辆列表
    val vehicles: StateFlow<List<Vehicle>> = vehicleDao
        .observeAllVehicles()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(stopTimeout),
            emptyList()
        )

    // 当前选中的车辆
    val currentVehicle: StateFlow<Vehicle?> = combine(
        vehicleDao.observeCurrentVehicle(),
        _currentVehicleOverride,
        vehicles
    ) { dbVehicle: Vehicle?, overrideVehicle: Vehicle?, allVehicles: List<Vehicle> ->
        val resolved = overrideVehicle ?: dbVehicle ?: allVehicles.find { it.isCurrent } ?: allVehicles.firstOrNull()

        // 当数据库返回的新状态与内存 Override 的 ID 相符时，清除 Override，恢复单源真理 (SSOT)
        if (overrideVehicle != null && dbVehicle?.id == overrideVehicle.id) {
            _currentVehicleOverride.value = null
        }

        resolved
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeout),
        null
    )

    // =========================
    // 加油记录（依据当前选中的车辆动态响应）
    // =========================

    @OptIn(ExperimentalCoroutinesApi::class)
    val records: StateFlow<List<FuelRecord>> = currentVehicle
        .flatMapLatest { vehicle ->
            if (vehicle == null) {
                flowOf(emptyList())
            } else {
                fuelDao.observeRecords(vehicle.id)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(stopTimeout),
            emptyList()
        )

    // =========================
    // 油耗计算 StateFlow (适配 FuelCalculator)
    // =========================

    /**
     * 完整统计指标汇总对象（提供给 UI 统计大卡片使用）
     */
    val statisticsSummary: StateFlow<StatisticsSummary?> = combine(
        records,
        currentVehicle
    ) { recordList, vehicle ->
        if (recordList.isEmpty()) null
        else FuelCalculator.calculateSummary(allRecords = recordList, vehicle = vehicle)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(stopTimeout),
            null
        )

    /**
     * 最新油耗（优先返回非离群、置信度不低于 LOW 的真实油耗）
     */
    val latestConsumption: StateFlow<Double?> = combine(
        records,
        currentVehicle
    ) { recordList, vehicle ->
        FuelCalculator.latestReliableConsumption(records = recordList, vehicle = vehicle)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(stopTimeout),
            null
        )

    /**
     * 平均油耗
     */
    val averageConsumption: StateFlow<Double?> = statisticsSummary
        .map { it?.averageConsumption }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(stopTimeout),
            null
        )

    // =========================
    // 车辆操作
    // =========================

    /**
     * 添加新车辆并将新车辆设为当前车辆
     */
    fun addVehicle(vehicle: Vehicle, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val newId = vehicleDao.insertAsCurrent(vehicle)
            val newVehicle = vehicle.copy(id = newId, isCurrent = true)
            _currentVehicleOverride.value = newVehicle
            onComplete?.invoke()
        }
    }

    /**
     * 切换当前激活的车辆
     */
    fun setCurrentVehicle(targetVehicle: Vehicle) {
        if (targetVehicle.isCurrent || currentVehicle.value?.id == targetVehicle.id) return
        viewModelScope.launch {
            _currentVehicleOverride.value = targetVehicle.copy(isCurrent = true)
            vehicleDao.setCurrentVehicle(targetVehicle.id)
        }
    }

    /**
     * 修改/更新车辆基本信息
     */
    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            if (currentVehicle.value?.id == vehicle.id || _currentVehicleOverride.value?.id == vehicle.id) {
                _currentVehicleOverride.value = vehicle
            }
            vehicleDao.update(vehicle)
        }
    }

    /**
     * 删除车辆及其关联的所有加油记录
     */
    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            val isDeletingCurrent = (currentVehicle.value?.id == vehicle.id) ||
                    (_currentVehicleOverride.value?.id == vehicle.id) ||
                    vehicle.isCurrent

            fuelDao.deleteByVehicleId(vehicle.id)
            vehicleDao.delete(vehicle)

            if (isDeletingCurrent) {
                val nextVehicle = vehicleDao.getAnotherVehicle(vehicle.id)
                if (nextVehicle != null) {
                    _currentVehicleOverride.value = nextVehicle.copy(isCurrent = true)
                    vehicleDao.setCurrentVehicle(nextVehicle.id)
                } else {
                    _currentVehicleOverride.value = null
                }
            }
        }
    }

    // =========================
    // 加油记录操作
    // =========================

    fun addFuelRecord(record: FuelRecord) {
        viewModelScope.launch {
            val vehicle = currentVehicle.value ?: return@launch
            val defaultFuelGrade = vehicle.fuelGrade
            fuelDao.insert(
                record.copy(
                    vehicleId = vehicle.id,
                    fuelGrade = record.fuelGrade.ifBlank { defaultFuelGrade }
                )
            )
        }
    }

    fun updateFuelRecord(record: FuelRecord) {
        viewModelScope.launch {
            fuelDao.update(record)
        }
    }

    fun deleteRecord(record: FuelRecord) {
        viewModelScope.launch {
            fuelDao.delete(record)
        }
    }

    fun addInitialRecord(
        odometer: Double?,
        currentFuel: Double?
    ) {
        viewModelScope.launch {
            val vehicle = currentVehicle.value ?: return@launch
            fuelDao.insert(
                FuelRecord(
                    vehicleId = vehicle.id,
                    fuelGrade = vehicle.fuelGrade,
                    timestamp = System.currentTimeMillis(),
                    odometer = odometer,
                    volume = 0.0,
                    actualPaidAmount = 0.0,
                    unitPrice = 0.0,
                    remainingFuel = currentFuel,
                    isInitialRecord = true,
                    initialFuel = currentFuel
                )
            )
        }
    }
}
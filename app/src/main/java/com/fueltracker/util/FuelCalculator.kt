package com.fueltracker.util

import com.fueltracker.data.FuelRecord
import com.fueltracker.data.Vehicle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class ConsumptionResult(
    val recordId: Long,
    val startOdometer: Double,
    val endOdometer: Double,
    val distance: Double,
    val fuelUsed: Double,
    val consumption: Double,
    val isEstimated: Boolean,
    // ===== 新增：算法与质量信息，默认值保证旧调用兼容 =====
    val algorithm: String = "FULL_TO_FULL",
    val quality: IntervalQuality? = null,
    val isOutlier: Boolean = false
)

/** 区间置信等级 */
enum class IntervalConfidence {
    UNRELIABLE, LOW, MEDIUM, HIGH
}

/** 一个闭合区间的数据质量画像 */
data class IntervalQuality(
    val startOdometerEstimated: Boolean = false,
    val missingOdometerCount: Int = 0,
    val missedFuelUnrecordedCount: Int = 0,
    val missedFuelRecordedVolume: Double = 0.0,
    val middlePartialFuelingCount: Int = 0,
    val intervalRecordCount: Int = 0,
    val confidenceScore: Double = 1.0,
    val confidence: IntervalConfidence = IntervalConfidence.HIGH,
    val exclusionReason: String? = null
)

/**
 * 统计页面的指标汇总结果
 */
data class StatisticsSummary(
    val totalCount: Int,
    val totalVolume: Double,
    val totalMissedVolume: Double,
    val totalCost: Double,
    val averagePrice: Double?,
    val totalDistance: Double,
    val totalFuel: Double,
    val averageConsumption: Double?,
    val odometerSpanText: String,
    val unclosedCount: Int,
    val rangeResults: List<ConsumptionResult>,

    // ===== 新增：抗误差汇总，默认参数兼容旧构造 =====
    val robustAverageConsumption: Double? = null,
    val weightedAverageConsumption: Double? = null,
    val reliableResultCount: Int = 0,
    val outlierCount: Int = 0
)

object FuelCalculator {

    private const val DEFAULT_INITIAL_ODOMETER = 20.0
    private const val MIN_VALID_CONSUMPTION = 0.1
    private const val MAX_VALID_CONSUMPTION = 150.0

    // ============================================================
    // 对外接口
    // ============================================================

    /**
     * 更稳的“最近油耗”：优先取最后一个非离群、置信度不低于 LOW 的闭合区间；
     * 没有则用最后一个闭合区间；再没有才返回 null。
     */
    @JvmOverloads
    fun latestReliableConsumption(
        records: List<FuelRecord>,
        vehicle: Vehicle? = null
    ): Double? {
        val results = calculateAll(records, vehicle)
        return results.lastOrNull { !it.isOutlier && (it.quality?.confidence ?: IntervalConfidence.HIGH) >= IntervalConfidence.LOW }?.consumption
            ?: results.lastOrNull()?.consumption
    }

    // ============================================================
    // 核心算法：O(n) 一次扫描，满到满 + 质量画像
    // ============================================================

    @JvmOverloads
    fun calculateAll(
        records: List<FuelRecord>,
        vehicle: Vehicle? = null
    ): List<ConsumptionResult> {

        val deliveryDate = vehicle?.deliveryDate?.takeIf { it > 0L }

        val fuelRecords = records
            .asSequence()
            .filter { isFuelVisible(it, deliveryDate) }
            .sortedWith(RECORD_COMPARATOR)
            .toList()

        if (fuelRecords.isEmpty()) return emptyList()

        val firstFuelRecord = fuelRecords.first()
        val firstRealOdometer = firstFuelRecord.odometer?.takeIf { it > 0 }

        val initialOdometer = firstRealOdometer
            ?: vehicle?.initialOdometer?.takeIf { it > 0 }
            ?: DEFAULT_INITIAL_ODOMETER

        var startOdometerEstimated = firstRealOdometer == null || !firstFuelRecord.isFull
        var segmentStartOdometer = initialOdometer

        val state = SegmentState().apply { resetForStartRecord(firstFuelRecord) }
        val results = ArrayList<ConsumptionResult>(fuelRecords.size)

        for (anchorIndex in 1 until fuelRecords.size) {
            val anchorRecord = fuelRecords[anchorIndex]

            state.addRecordFuel(anchorRecord)
            state.addRecordQualityFlags(anchorRecord)

            val currentOdometer = anchorRecord.odometer?.takeIf { it > 0 }
            if (currentOdometer == null) {
                // 多次未录公里数：不把假里程当终点；若它未加满，未来闭合时只记为“中间未加满”
                state.markAsMiddleRecordIfNeeded(anchorRecord)
                continue
            }

            if (!anchorRecord.isFull) {
                state.markAsMiddleRecordIfNeeded(anchorRecord)
                continue
            }

            val distance = currentOdometer - segmentStartOdometer
            if (distance <= 0) continue

            val totalFuel = state.pendingFuel
            if (totalFuel <= 0) continue

            val quality = buildQuality(
                state = state,
                startOdometerEstimated = startOdometerEstimated,
                distance = distance
            )
            val isEstimated = quality.confidence != IntervalConfidence.HIGH
            val consumption = totalFuel / distance * 100.0

            if (!consumption.isFinite() || consumption !in MIN_VALID_CONSUMPTION..MAX_VALID_CONSUMPTION) {
                continue
            }

            results.add(
                ConsumptionResult(
                    recordId = anchorRecord.id,
                    startOdometer = segmentStartOdometer,
                    endOdometer = currentOdometer,
                    distance = distance,
                    fuelUsed = totalFuel,
                    consumption = consumption,
                    isEstimated = isEstimated,
                    algorithm = "FULL_TO_FULL",
                    quality = quality
                )
            )

            segmentStartOdometer = currentOdometer
            startOdometerEstimated = false
            state.resetForStartRecord(anchorRecord)
        }

        return markOutliers(results)
    }

    // ============================================================
    // 缺失里程：可选的时间插值/外推，默认不参与油耗
    // ============================================================

    // ============================================================
    // Map / Summary
    // ============================================================

    @JvmOverloads
    fun calculateMap(
        records: List<FuelRecord>,
        vehicle: Vehicle? = null
    ): Map<Long, ConsumptionResult> =
        calculateAll(records, vehicle).associateBy { it.recordId }

    @JvmOverloads
    fun calculateSummary(
        allRecords: List<FuelRecord>,
        vehicle: Vehicle? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): StatisticsSummary {

        val resultsById = calculateMap(allRecords, vehicle)

        val deliveryDate = vehicle?.deliveryDate?.takeIf { it > 0L }
        val periodRecords = allRecords
            .asSequence()
            .filter { record ->
                if (!isFuelVisible(record, deliveryDate)) return@filter false
                if (startTime != null && record.timestamp < startTime) return@filter false
                if (endTime != null && record.timestamp >= endTime) return@filter false
                true
            }
            .sortedBy { it.timestamp }
            .toList()

        val rangeResults = periodRecords.mapNotNull { resultsById[it.id] }

        val totalCount = periodRecords.size
        val totalVolume = periodRecords.sumOf { it.volume }
        val totalMissedVolume = periodRecords.sumOf { it.missedVolume ?: 0.0 }
        val totalCost = periodRecords.sumOf { it.actualPaidAmount }

        val averagePrice = if (totalVolume > 0) totalCost / totalVolume else null

        val totalDistance = rangeResults.sumOf { it.distance }
        val totalFuel = rangeResults.sumOf { it.fuelUsed }

        val averageConsumption = if (totalDistance > 0) totalFuel / totalDistance * 100.0 else null
        val robust = robustWeightedConsumption(rangeResults)

        val odometerSpanText = if (rangeResults.isNotEmpty()) {
            val minOdo = rangeResults.minOf { it.startOdometer }
            val maxOdo = rangeResults.maxOf { it.endOdometer }
            "%.0f - %.0f km".format(Locale.US, minOdo, maxOdo)
        } else {
            "--"
        }

        val unclosedCount = periodRecords.count { !it.isFull || resultsById[it.id] == null }

        return StatisticsSummary(
            totalCount = totalCount,
            totalVolume = totalVolume,
            totalMissedVolume = totalMissedVolume,
            totalCost = totalCost,
            averagePrice = averagePrice,
            totalDistance = totalDistance,
            totalFuel = totalFuel,
            averageConsumption = averageConsumption,
            odometerSpanText = odometerSpanText,
            unclosedCount = unclosedCount,
            rangeResults = rangeResults,
            robustAverageConsumption = robust,
            reliableResultCount = rangeResults.count {
                !it.isOutlier && (it.quality?.confidence ?: IntervalConfidence.HIGH) >= IntervalConfidence.MEDIUM
            },
            outlierCount = rangeResults.count { it.isOutlier }
        )
    }

    // ============================================================
    // 私有：滚动状态
    // ============================================================

    private class SegmentState {
        var pendingFuel: Double = 0.0
            private set

        var processedRecordCount: Int = 0
            private set
        var missingOdometerCount: Int = 0
            private set
        var missedFuelUnrecordedCount: Int = 0
            private set
        var missedFuelRecordedVolume: Double = 0.0
            private set
        var middlePartialFuelingCount: Int = 0
            private set

        fun resetForStartRecord(record: FuelRecord) {
            pendingFuel = 0.0
            processedRecordCount = 1
            missingOdometerCount = if (hasInvalidOdometer(record)) 1 else 0
            missedFuelUnrecordedCount = if (isMissedFuelUnrecorded(record)) 1 else 0
            missedFuelRecordedVolume = (record.missedVolume ?: 0.0).coerceAtLeast(0.0)
            middlePartialFuelingCount = 0
        }

        fun addRecordFuel(record: FuelRecord) {
            // 只累计“起点之后”的油；起点油量是上一段已计入的满箱基准
            pendingFuel += record.volume + (record.missedVolume ?: 0.0)
        }

        fun addRecordQualityFlags(record: FuelRecord) {
            processedRecordCount++
            if (hasInvalidOdometer(record)) missingOdometerCount++
            if (isMissedFuelUnrecorded(record)) missedFuelUnrecordedCount++
            missedFuelRecordedVolume += (record.missedVolume ?: 0.0).coerceAtLeast(0.0)
        }

        fun markAsMiddleRecordIfNeeded(record: FuelRecord) {
            if (!record.isFull) middlePartialFuelingCount++
        }
    }

    private data class RobustBounds(val median: Double, val mad: Double, val lower: Double, val upper: Double)

    private val RECORD_COMPARATOR = compareBy<FuelRecord>(
        { it.timestamp },
        { it.odometer ?: Double.POSITIVE_INFINITY }
    )

    private fun isFuelVisible(record: FuelRecord, deliveryDate: Long?): Boolean {
        return !record.isInitialRecord && (deliveryDate == null || record.timestamp >= deliveryDate)
    }

    private fun hasInvalidOdometer(record: FuelRecord): Boolean {
        val odometer = record.odometer
        return odometer == null || odometer <= 0
    }

    /** hasMissedRecord=true 但没填 missedVolume：这是最大的误差来源之一，重罚但不编造油量 */
    private fun isMissedFuelUnrecorded(record: FuelRecord): Boolean {
        return record.hasMissedRecord && (record.missedVolume ?: 0.0) <= 0
    }

    private fun buildQuality(
        state: SegmentState,
        startOdometerEstimated: Boolean,
        distance: Double
    ): IntervalQuality {
        var score = 1.0

        if (startOdometerEstimated) score -= 0.35

        // 多次未录公里数：每个缺失点约 -8%，封顶 -30%
        score -= min(0.30, 0.08 * state.missingOdometerCount)

        // 未录 missedVolume 比“已录 missedVolume”严重；已录只轻微扣，因为它已被加回油量
        score -= min(0.40, 0.18 * state.missedFuelUnrecordedCount)
        score -= min(0.10, 0.01 * state.missedFuelRecordedVolume)

        // 中间多次未加满：每个约 -6%，封顶 -25%
        score -= min(0.25, 0.06 * state.middlePartialFuelingCount)

        // 超短里程对油耗噪声很敏感
        when {
            distance < 30.0 -> score -= 0.15
            distance < 80.0 -> score -= 0.05
        }

        val clamped = score.coerceIn(0.05, 1.0)
        val confidence = when {
            clamped >= 0.80 -> IntervalConfidence.HIGH
            clamped >= 0.55 -> IntervalConfidence.MEDIUM
            clamped >= 0.30 -> IntervalConfidence.LOW
            else -> IntervalConfidence.UNRELIABLE
        }

        return IntervalQuality(
            startOdometerEstimated = startOdometerEstimated,
            missingOdometerCount = state.missingOdometerCount,
            missedFuelUnrecordedCount = state.missedFuelUnrecordedCount,
            missedFuelRecordedVolume = state.missedFuelRecordedVolume,
            middlePartialFuelingCount = state.middlePartialFuelingCount,
            intervalRecordCount = state.processedRecordCount,
            confidenceScore = clamped,
            confidence = confidence
        )
    }

    // ============================================================
    // 私有：鲁棒统计
    // ============================================================

    /** MAD 去离群：样本太少或 MAD=0 时只保留物理范围过滤 */
    private fun markOutliers(results: List<ConsumptionResult>): List<ConsumptionResult> {
        if (results.size < 5) return results
        val bounds = robustBounds(results.map { it.consumption }) ?: return results

        return results.map { r ->
            if (r.consumption < bounds.lower || r.consumption > bounds.upper) {
                val q = r.quality ?: IntervalQuality(
                    confidenceScore = if (r.isEstimated) 0.50 else 1.0,
                    confidence = if (r.isEstimated) IntervalConfidence.MEDIUM else IntervalConfidence.HIGH
                )
                r.copy(
                    isOutlier = true,
                    quality = q.copy(
                        confidence = IntervalConfidence.UNRELIABLE,
                        confidenceScore = 0.05,
                        exclusionReason = "OUTLIER_MAD"
                    )
                )
            } else {
                r
            }
        }
    }

    /**
     * 加权鲁棒平均：
     * 1) 先剔除 MAD 离群；
     * 2) 权重 = confidenceScore * sqrt(distance)。
     *    confidenceScore 惩罚未录里程/未录 missedVolume/多次未加满；
     *    sqrt(distance) 让长区间比短区间更稳，但不至于完全吞掉短区间。
     */
    private fun robustWeightedConsumption(results: List<ConsumptionResult>): Double? {
        val candidates = results.filter {
            it.consumption.isFinite() && it.distance > 0 && !it.isOutlier
        }
        if (candidates.isEmpty()) return null

        val bounds = robustBounds(candidates.map { it.consumption })
        val kept = if (bounds == null) {
            candidates
        } else {
            candidates.filter { it.consumption in bounds.lower..bounds.upper }
        }
        if (kept.isEmpty()) return null

        var weightSum = 0.0
        var weightedSum = 0.0
        for ((_, _, _, distance, _, consumption, isEstimated, _, quality) in kept) {
            val confidence = (quality?.confidenceScore ?: if (isEstimated) 0.50 else 1.0)
                .coerceIn(0.05, 1.0)
            val w = confidence * sqrt(distance.coerceAtLeast(1.0))
            weightSum += w
            weightedSum += consumption * w
        }

        return if (weightSum > 0) weightedSum / weightSum else null
    }

    private fun robustBounds(values: List<Double>): RobustBounds? {
        val finite = values.filter { it.isFinite() }.sorted()
        if (finite.size < 5) return null

        val median = median(finite) ?: return null
        val mad = median(finite.map { abs(it - median) }.sorted()) ?: return null

        val threshold = max(3.0 * mad, 0.35)
        return RobustBounds(
            median = median,
            mad = mad,
            lower = (median - threshold).coerceAtLeast(MIN_VALID_CONSUMPTION),
            upper = (median + threshold).coerceAtMost(MAX_VALID_CONSUMPTION)
        )
    }

    private fun median(sorted: List<Double>): Double? {
        if (sorted.isEmpty()) return null
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    }

}

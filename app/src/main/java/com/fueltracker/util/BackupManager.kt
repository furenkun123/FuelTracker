package com.fueltracker.util

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.fueltracker.data.AppDatabase
import com.fueltracker.data.FuelRecord
import com.fueltracker.data.Vehicle
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    private const val FORMAT_VERSION = 1

    /**
     * 生成规范的默认备份文件名，例如：随记油耗备份20260930.json
     */
    fun generateFileName(): String {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "随记油耗备份$dateStr.json"
    }

    suspend fun createBackupJson(context: Context): String {
        val db = AppDatabase.getInstance(context)
        val vehicles = db.vehicleDao().getAllVehicles()
        val records = db.fuelDao().getAllRecords()

        val root = JSONObject().apply {
            put("formatVersion", FORMAT_VERSION)
            put("createdAt", System.currentTimeMillis())

            put("vehicles", JSONArray().apply {
                vehicles.forEach { vehicle ->
                    put(JSONObject().apply {
                        put("id", vehicle.id)
                        put("brand", vehicle.brand)
                        put("series", vehicle.series)
                        put("year", vehicle.year)
                        put("model", vehicle.model)
                        put("fuelType", vehicle.fuelType)
                        put("fuelGrade", vehicle.fuelGrade)
                        put("tankCapacity", vehicle.tankCapacity)
                        put("initialOdometer", vehicle.initialOdometer)
                        put("deliveryDate", vehicle.deliveryDate ?: JSONObject.NULL)
                        put("isCurrent", vehicle.isCurrent)
                    })
                }
            })

            put("fuelRecords", JSONArray().apply {
                records.forEach { record ->
                    put(JSONObject().apply {
                        put("id", record.id)
                        put("vehicleId", record.vehicleId)
                        put("fuelGrade", record.fuelGrade)
                        put("timestamp", record.timestamp)
                        put("odometer", record.odometer)
                        put("volume", record.volume)
                        put("actualPaidAmount", record.actualPaidAmount)
                        put("unitPrice", record.unitPrice)
                        put("remainingFuel", record.remainingFuel ?: JSONObject.NULL)
                        put("isFull", record.isFull)
                        put("isInitialRecord", record.isInitialRecord)
                        put("initialFuel", record.initialFuel ?: JSONObject.NULL)
                        put("hasMissedRecord", record.hasMissedRecord)
                        put("missedOdometer", record.missedOdometer ?: JSONObject.NULL)
                        put("missedVolume", record.missedVolume ?: JSONObject.NULL)
                        put("note", record.note)
                    })
                }
            })
        }

        return root.toString(2)
    }

    suspend fun restoreFromJson(context: Context, jsonText: String) {
        val root = JSONObject(jsonText)

        if (root.optInt("formatVersion", -1) != FORMAT_VERSION) {
            throw IllegalArgumentException("不支持的备份版本")
        }

        val vehicles = mutableListOf<Vehicle>()
        val vehicleArray = root.optJSONArray("vehicles") ?: JSONArray()

        for (i in 0 until vehicleArray.length()) {
            val json = vehicleArray.getJSONObject(i)
            vehicles.add(
                Vehicle(
                    id = json.optLong("id"),
                    brand = json.optString("brand"),
                    series = json.optString("series"),
                    year = json.optInt("year"),
                    model = json.optString("model"),
                    fuelType = json.optString("fuelType", "汽油"),
                    fuelGrade = json.optString("fuelGrade", "92#"),
                    tankCapacity = json.optDouble("tankCapacity", 0.0),
                    initialOdometer = json.optDouble("initialOdometer", 0.0),
                    deliveryDate = json.getNullableLong("deliveryDate"),
                    isCurrent = json.optBoolean("isCurrent")
                )
            )
        }

        // 兜底处理：若导入的车辆列表不为空，但没有任何车辆标记为当前车辆，默认将第一辆车设为当前车辆
        if (vehicles.isNotEmpty() && vehicles.none { it.isCurrent }) {
            vehicles[0] = vehicles[0].copy(isCurrent = true)
        }

        val records = mutableListOf<FuelRecord>()
        val recordArray = root.optJSONArray("fuelRecords") ?: JSONArray()

        for (i in 0 until recordArray.length()) {
            val json = recordArray.getJSONObject(i)
            records.add(
                FuelRecord(
                    id = json.optLong("id"),
                    vehicleId = json.optLong("vehicleId"),
                    fuelGrade = json.optString("fuelGrade", "92#"),
                    timestamp = json.optLong("timestamp"),
                    odometer = json.optDouble("odometer"),
                    volume = json.optDouble("volume"),
                    actualPaidAmount = json.optDouble("actualPaidAmount"),
                    unitPrice = json.optDouble("unitPrice"),
                    remainingFuel = json.getNullableDouble("remainingFuel"),
                    isFull = json.optBoolean("isFull"),
                    isInitialRecord = json.optBoolean("isInitialRecord"),
                    initialFuel = json.getNullableDouble("initialFuel"),
                    hasMissedRecord = json.optBoolean("hasMissedRecord"),
                    missedOdometer = json.getNullableDouble("missedOdometer"),
                    missedVolume = json.getNullableDouble("missedVolume"),
                    note = json.optString("note", "")
                )
            )
        }

        val db = AppDatabase.getInstance(context)

        db.withTransaction {
            db.fuelDao().deleteAll()
            db.vehicleDao().deleteAll()

            vehicles.forEach {
                db.vehicleDao().insert(it)
            }

            records.forEach {
                db.fuelDao().insert(it)
            }
        }
    }

    suspend fun writeBackupToUri(context: Context, uri: Uri) {
        val json = createBackupJson(context)
        context.contentResolver.openOutputStream(uri)?.use {
            it.write(json.toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("无法创建备份文件")
    }

    suspend fun restoreFromUri(context: Context, uri: Uri) {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
            it.readText()
        } ?: throw IllegalStateException("无法读取备份文件")

        restoreFromJson(context, json)
    }
}

private fun JSONObject.getNullableDouble(key: String): Double? {
    return if (isNull(key)) null else optDouble(key)
}

private fun JSONObject.getNullableLong(key: String): Long? {
    return if (isNull(key)) null else optLong(key)
}
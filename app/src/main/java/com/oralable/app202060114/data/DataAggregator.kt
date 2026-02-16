package com.oralable.app202060114.data

import com.oralable.app202060114.bluetooth.PpgData

class DataAggregator {
    @Volatile var latestPpg: PpgData? = null
    @Volatile var latestAccelX: Short? = null
    @Volatile var latestAccelY: Short? = null
    @Volatile var latestAccelZ: Short? = null
    @Volatile var latestTemp: Float? = null
    @Volatile var latestEmg: Double? = null
    @Volatile var latestBattery: Int? = null
    @Volatile var latestHeartRate: Int? = null

    fun updatePpg(ppg: PpgData) {
        latestPpg = ppg
    }

    fun updateAccel(x: Short, y: Short, z: Short) {
        latestAccelX = x
        latestAccelY = y
        latestAccelZ = z
    }

    fun updateTemp(temp: Float) {
        latestTemp = temp
    }

    fun updateEmg(emg: Double) {
        latestEmg = emg
    }

    fun updateBattery(battery: Int) {
        latestBattery = battery
    }

    fun updateHeartRate(heartRate: Int) {
        latestHeartRate = heartRate
    }

    fun createDataPoint(timestamp: Long, deviceName: String): SensorDataPoint {
        return SensorDataPoint(
            timestamp = timestamp,
            deviceName = deviceName,
            ppgIr = latestPpg?.ppgIr,
            ppgRed = latestPpg?.ppgRed,
            ppgGreen = latestPpg?.ppgGreen,
            accelX = latestAccelX,
            accelY = latestAccelY,
            accelZ = latestAccelZ,
            temperature = latestTemp,
            emgValue = latestEmg,
            battery = latestBattery,
            heartRate = latestHeartRate
        )
    }
}

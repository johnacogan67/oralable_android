package com.oralable.app202060114.data

data class SensorDataPoint(
    val timestamp: Long,
    val deviceName: String,
    val ppgIr: Int? = null,
    val ppgRed: Int? = null,
    val ppgGreen: Int? = null,
    val accelX: Short? = null,
    val accelY: Short? = null,
    val accelZ: Short? = null,
    val temperature: Float? = null,
    val emgValue: Double? = null
)

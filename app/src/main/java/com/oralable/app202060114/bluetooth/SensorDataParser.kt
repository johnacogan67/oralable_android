package com.oralable.app202060114.bluetooth

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class PpgData(val ppgIr: Int, val ppgRed: Int, val ppgGreen: Int)
data class AccelerometerData(val x: Short, val y: Short, val z: Short)
data class TemperatureData(val celsius: Float)
data class EmgData(val value: Double)

object SensorDataParser {
    fun parsePpgData(data: ByteArray): List<PpgData>? {
        if (data.size < 16) { // Minimum size for one sample + header
            return null
        }

        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.int // Skip frame counter

        val samples = mutableListOf<PpgData>()
        val bytesPerSample = 12

        while (buffer.remaining() >= bytesPerSample) {
            val ppgGreen = buffer.int
            val ppgIr = buffer.int
            val ppgRed = buffer.int
            samples.add(PpgData(ppgIr, ppgRed, ppgGreen))
        }

        return if (samples.isEmpty()) null else samples
    }

    fun parseAccelerometerData(data: ByteArray): AccelerometerData? {
        // Accelerometer packet format:
        // Bytes 0-3: Frame counter (uint32_t)
        // Bytes 4+: 25 samples, each 6 bytes (3 × int16_t for X, Y, Z)
        val expectedSize = 4 + 6
        if (data.size < expectedSize) {
            return null
        }
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.int // Skip frame counter
        
        // Read the first sample
        val x = buffer.short
        val y = buffer.short
        val z = buffer.short
        return AccelerometerData(x, y, z)
    }
    
    fun parseTemperatureData(data: ByteArray): TemperatureData? {
        // Temperature packet format (8 bytes):
        // Bytes 0-3: Frame counter
        // Bytes 4-5: Temperature (int16, centidegrees Celsius)
        if (data.size < 6) {
            return null
        }
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.int // Skip frame counter
        val centiDegrees = buffer.short
        val celsius = centiDegrees / 100.0f
        return TemperatureData(celsius)
    }

    fun parseEmgData(data: ByteArray): EmgData? {
        if (data.size < 2) {
            return null
        }
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val rawValue = buffer.short.toInt() and 0xFFFF // Read as unsigned short
        
        val normalizedValue = rawValue / 1023.0
        return EmgData(normalizedValue)
    }

    fun parseTgmBatteryData(data: ByteArray): Int? {
        if (data.size < 4) {
            return null
        }
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val millivolts = buffer.int
        
        // Convert millivolts to percentage (approximate LiPo curve)
        // 4.2V (4200mV) = 100%, 3.3V (3300mV) = 0%
        val percentage = ((millivolts - 3300) / 900.0) * 100.0
        return percentage.toInt().coerceIn(0, 100)
    }

    fun parseStandardBatteryLevel(data: ByteArray): Int? {
        if (data.isEmpty()) {
            return null
        }
        return data[0].toInt().coerceIn(0, 100)
    }
}

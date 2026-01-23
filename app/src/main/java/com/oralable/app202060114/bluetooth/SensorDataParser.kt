package com.oralable.app202060114.bluetooth

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class PpgData(val ppgIr: Int, val ppgRed: Int, val ppgGreen: Int)
data class AccelerometerData(val x: Short, val y: Short, val z: Short)
data class TemperatureData(val celsius: Float)
data class EmgData(val value: Double)

object SensorDataParser {
    fun parsePpgData(data: ByteArray): PpgData? {
        // PPG packet format:
        // Bytes 0-3: Frame counter (uint32_t)
        // Bytes 4+: 20 samples, each 12 bytes (3 × uint32_t PPG values)
        val expectedSize = 4 + (20 * 12)
        if (data.size < expectedSize) {
            return null // Not a valid PPG packet
        }

        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.int // Skip frame counter

        // For now, we'll just read the first sample to confirm data is flowing.
        // In the Swift code, the order is Green, IR, Red. Let's assume that for now.
        val ppgGreen = buffer.int
        val ppgIr = buffer.int
        val ppgRed = buffer.int

        return PpgData(ppgIr, ppgRed, ppgGreen)
    }

    fun parseAccelerometerData(data: ByteArray): AccelerometerData? {
        // Accelerometer packet format:
        // Bytes 0-3: Frame counter (uint32_t)
        // Bytes 4+: 25 samples, each 6 bytes (3 × int16_t for X, Y, Z)
        val expectedSize = 4 + (25 * 6)
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
        
        // The Swift code normalizes the value by dividing by 1023.0
        val normalizedValue = rawValue / 1023.0
        return EmgData(normalizedValue)
    }
}

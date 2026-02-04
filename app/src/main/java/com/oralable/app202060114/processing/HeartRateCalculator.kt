package com.oralable.app202060114.processing

import android.util.Log
import java.util.LinkedList
import kotlin.math.pow
import kotlin.math.sqrt

class HeartRateCalculator(
    private val sampleRate: Double = 50.0,
    windowSeconds: Double = 5.0,
    private val minBPM: Double = 40.0,
    private val maxBPM: Double = 180.0,
    private val minPeaksRequired: Int = 4,
    private val peakThreshold: Double = 0.4
) {
    private val windowSize = (sampleRate * windowSeconds).toInt()
    private val buffer = LinkedList<Double>()
    private val minPeakInterval = (sampleRate * 60.0 / maxBPM * 0.8).toInt()

    fun process(sample: Double): Int {
        buffer.add(sample)
        while (buffer.size > windowSize) {
            buffer.removeFirst()
        }

        if (buffer.size < windowSize) {
            return 0 // Not enough data
        }

        val filtered = applyBandpassFilter(buffer.toList())
        val peaks = findPeaks(filtered)
        Log.d("HeartRate", "Found ${peaks.size} peaks in window")

        val (bpm, _) = calculateBPM(peaks)

        return if (bpm in minBPM.toInt()..maxBPM.toInt()) bpm else 0
    }

    private fun applyBandpassFilter(data: List<Double>): List<Double> {
        if (data.size < 5) return data
        val mean = data.average()
        val centered = data.map { it - mean }

        val smoothed = centered.toMutableList()
        for (i in 2 until centered.size - 2) {
            smoothed[i] = (centered[i - 2] + centered[i - 1] + centered[i] + centered[i + 1] + centered[i + 2]) / 5.0
        }
        return smoothed
    }

    private fun findPeaks(data: List<Double>): List<Int> {
        val peaks = mutableListOf<Int>()
        if (data.size < 3) return peaks

        val maxValue = data.maxOrNull() ?: 0.0
        val minValue = data.minOrNull() ?: 0.0
        val amplitude = maxValue - minValue
        val threshold = minValue + amplitude * peakThreshold

        for (i in 1 until data.size - 1) {
            if (data[i] > data[i - 1] && data[i] > data[i + 1] && data[i] > threshold) {
                if (peaks.isEmpty() || (i - peaks.last()) >= minPeakInterval) {
                    peaks.add(i)
                }
            }
        }
        return peaks
    }

    private fun calculateBPM(peaks: List<Int>): Pair<Int, Double> {
        if (peaks.size < minPeaksRequired) {
            return Pair(0, 0.0)
        }

        val intervals = (1 until peaks.size).map { (peaks[it] - peaks[it - 1]).toDouble() }
        if (intervals.isEmpty()) {
            return Pair(0, 0.0)
        }

        val avgInterval = intervals.average()
        val bpm = 60.0 / (avgInterval / sampleRate)

        val variance = intervals.map { (it - avgInterval).pow(2) }.average()
        val stdDev = sqrt(variance)
        val confidence = (1.0 - (stdDev / avgInterval)).coerceIn(0.0, 1.0)

        return Pair(bpm.toInt(), confidence)
    }
}

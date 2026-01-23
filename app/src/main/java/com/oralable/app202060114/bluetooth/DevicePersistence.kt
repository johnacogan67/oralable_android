package com.oralable.app202060114.bluetooth

import android.content.Context
import android.content.SharedPreferences

class DevicePersistence(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bt_devices", Context.MODE_PRIVATE)
    private val KEY_SAVED_DEVICES = "saved_devices"

    fun getSavedDeviceAddresses(): Set<String> {
        return prefs.getStringSet(KEY_SAVED_DEVICES, emptySet()) ?: emptySet()
    }

    fun saveDevice(address: String) {
        val currentDevices = getSavedDeviceAddresses().toMutableSet()
        currentDevices.add(address)
        prefs.edit().putStringSet(KEY_SAVED_DEVICES, currentDevices).apply()
    }

    fun removeDevice(address: String) {
        val currentDevices = getSavedDeviceAddresses().toMutableSet()
        currentDevices.remove(address)
        prefs.edit().putStringSet(KEY_SAVED_DEVICES, currentDevices).apply()
    }
}

package com.example.myapplication3

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.myapplication3.ui.theme.MyApplication3Theme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    private lateinit var bleManager: BLEManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            bleManager.startScanning()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bleManager = BLEManager(this)

        enableEdgeToEdge()
        setContent {
            MyApplication3Theme {
                MainScreen(bleManager)
            }
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            bleManager.startScanning()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(bleManager: BLEManager) {
    val discoveredDevices = remember { mutableStateListOf<DeviceItem>() }
    var connectedDeviceName by remember { mutableStateOf<String?>(null) }
    var irValue by remember { mutableLongStateOf(0L) }
    var redValue by remember { mutableLongStateOf(0L) }
    var greenValue by remember { mutableLongStateOf(0L) }
    var battery by remember { mutableIntStateOf(0) }
    var temperature by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        bleManager.events.collectLatest { event ->
            when (event) {
                is BLEEvent.DeviceDiscovered -> {
                    if (discoveredDevices.none { it.device.address == event.device.address }) {
                        discoveredDevices.add(DeviceItem(event.device, event.name))
                    }
                }
                is BLEEvent.DeviceConnected -> {
                    connectedDeviceName = event.device.name ?: "Connected Device"
                }
                is BLEEvent.DeviceDisconnected -> {
                    connectedDeviceName = null
                }
                is BLEEvent.DataReceived -> {
                    irValue = event.ir
                    redValue = event.red
                    greenValue = event.green
                }
                is BLEEvent.BatteryReceived -> {
                    battery = event.percentage
                }
                is BLEEvent.TemperatureReceived -> {
                    temperature = event.celsius
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Oralable Android") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (connectedDeviceName == null) {
                Text(
                    text = "Discovered Devices",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(discoveredDevices) { item ->
                        DeviceRow(item) {
                            bleManager.connect(item.device)
                        }
                    }
                }
                Button(
                    onClick = { bleManager.startScanning() },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Refresh Scan")
                }
            } else {
                Text(
                    text = "Connected: $connectedDeviceName",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                DataCard(label = "PPG Infrared", value = irValue.toString(), unit = "")
                DataCard(label = "PPG Red", value = redValue.toString(), unit = "")
                DataCard(label = "PPG Green", value = greenValue.toString(), unit = "")
                DataCard(label = "Temperature", value = "%.2f".format(temperature), unit = "°C")
                DataCard(label = "Battery", value = battery.toString(), unit = "%")

                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { bleManager.disconnect() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Disconnect")
                }
            }
        }
    }
}

data class DeviceItem(val device: BluetoothDevice, val name: String)

@Composable
fun DeviceRow(item: DeviceItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = item.name, fontWeight = FontWeight.Bold)
            Text(text = item.device.address, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun DataCard(label: String, value: String, unit: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = MaterialTheme.colorScheme.secondary)
            Text(text = "$value $unit", fontWeight = FontWeight.Bold)
        }
    }
}

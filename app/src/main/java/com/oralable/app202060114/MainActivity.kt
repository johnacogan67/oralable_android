package com.oralable.app202060114

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.oralable.app202060114.screens.DashboardScreen
import com.oralable.app202060114.screens.DevicesScreen
import com.oralable.app202060114.screens.SettingsScreen
import com.oralable.app202060114.screens.ShareScreen
import com.oralable.app202060114.ui.theme.App202060114Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App202060114Theme {
                App202060114App()
            }
        }
    }
}

@Composable
fun App202060114App() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.DASHBOARD) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.DASHBOARD -> DashboardScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.DEVICES -> DevicesScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.SHARE -> ShareScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.SETTINGS -> SettingsScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    DASHBOARD("Dashboard", Icons.Default.Home),
    DEVICES("Devices", Icons.Rounded.PhoneAndroid),
    SHARE("Share", Icons.Default.Share),
    SETTINGS("Settings", Icons.Default.Settings),
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    App202060114Theme {
        DashboardScreen()
    }
}

package com.oralable.app202060114.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oralable.app202060114.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "About",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.handleDeveloperTap() }
                .padding(vertical = 8.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = "About")
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("About")
                Text("Version 1.0")
            }
        }
        if (uiState.showDeveloperMessage) {
            Text("Developer settings unlocked!", modifier = Modifier.padding(top = 16.dp))
        }
    }
}

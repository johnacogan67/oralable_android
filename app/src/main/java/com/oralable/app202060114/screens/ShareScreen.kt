package com.oralable.app202060114.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oralable.app202060114.viewmodels.ShareViewModel

@Composable
fun ShareScreen(
    modifier: Modifier = Modifier,
    viewModel: ShareViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { viewModel.shareCsv() }) {
            Text("Share Data as CSV")
        }
        if (uiState.shareMessage.isNotEmpty()) {
            Text(uiState.shareMessage, modifier = Modifier.padding(top = 16.dp))
        }
    }
}

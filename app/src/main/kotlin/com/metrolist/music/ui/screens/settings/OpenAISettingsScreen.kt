package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.viewmodels.OpenAISettingsViewModel
import android.widget.Toast

@Composable
fun OpenAISettingsScreen(
    navController: NavController,
    viewModel: OpenAISettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val saveResult by viewModel.saveResult.collectAsStateWithLifecycle()
    
    // Handle save result
    LaunchedEffect(saveResult) {
        saveResult?.let { result ->
            when (result) {
                is OpenAISettingsViewModel.SaveResult.Success -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
                is OpenAISettingsViewModel.SaveResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
            viewModel.clearSaveResult()
        }
    }
    
    OpenAISettings(
        onBackClick = { navController.navigateUp() },
        apiKey = apiKey,
        onApiKeyChange = viewModel::updateApiKey,
        onSaveApiKey = viewModel::saveApiKey
    )
    
    // Show loading indicator
    if (isLoading) {
        Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
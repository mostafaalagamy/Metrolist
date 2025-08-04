package com.metrolist.music.viewmodels

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.constants.OpenAIApiKeyKey
import com.metrolist.music.translation.TranslationConfig
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OpenAISettingsViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {
    
    private val dataStore = application.dataStore
    
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult: StateFlow<SaveResult?> = _saveResult.asStateFlow()
    
    init {
        loadApiKey()
    }
    
    private fun loadApiKey() {
        viewModelScope.launch {
            try {
                val preferences = dataStore.data.first()
                val storedApiKey = preferences[OpenAIApiKeyKey] ?: ""
                _apiKey.value = storedApiKey
                TranslationConfig.updateOpenAIApiKey(storedApiKey)
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    fun updateApiKey(newApiKey: String) {
        _apiKey.value = newApiKey
        // Clear previous save result
        _saveResult.value = null
    }
    
    fun saveApiKey() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val apiKeyToSave = _apiKey.value.trim()
                
                // Validate API key format
                if (apiKeyToSave.isNotBlank() && !apiKeyToSave.startsWith("sk-")) {
                    _saveResult.value = SaveResult.Error("API Key يجب أن يبدأ بـ 'sk-'")
                    return@launch
                }
                
                // Save to DataStore
                dataStore.edit { preferences ->
                    preferences[OpenAIApiKeyKey] = apiKeyToSave
                }
                
                // Update TranslationConfig
                TranslationConfig.updateOpenAIApiKey(apiKeyToSave)
                
                _saveResult.value = if (apiKeyToSave.isBlank()) {
                    SaveResult.Success("تم حذف API Key بنجاح")
                } else {
                    SaveResult.Success("تم حفظ API Key بنجاح! ستحصل الآن على أفضل ترجمة بالذكاء الاصطناعي")
                }
                
            } catch (e: Exception) {
                _saveResult.value = SaveResult.Error("حدث خطأ أثناء الحفظ: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearSaveResult() {
        _saveResult.value = null
    }
    
    sealed class SaveResult {
        data class Success(val message: String) : SaveResult()
        data class Error(val message: String) : SaveResult()
    }
}
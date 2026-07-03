package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.ConversionDatabase
import com.example.data.database.ConversionEntity
import com.example.data.repository.ConversionRepository
import com.example.data.repository.ExtractionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(private val repository: ConversionRepository) : ViewModel() {

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _selectedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedImageBitmap: StateFlow<Bitmap?> = _selectedImageBitmap.asStateFlow()

    private val _extractionType = MutableStateFlow(ExtractionType.DOCUMENT_RECONSTRUCTION)
    val extractionType: StateFlow<ExtractionType> = _extractionType.asStateFlow()

    private val _isConverting = MutableStateFlow(false)
    val isConverting: StateFlow<Boolean> = _isConverting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentExtractedText = MutableStateFlow<String?>(null)
    val currentExtractedText: StateFlow<String?> = _currentExtractedText.asStateFlow()

    private val _currentConversionId = MutableStateFlow<Int?>(null)
    val currentConversionId: StateFlow<Int?> = _currentConversionId.asStateFlow()

    private val _documentTitle = MutableStateFlow("")
    val documentTitle: StateFlow<String> = _documentTitle.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtered list of history entries based on search query
    val conversions: StateFlow<List<ConversionEntity>> = repository.allConversions
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) {
                list
            } else {
                list.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.extractedText.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _selectedImageUri.value = uri
            _currentExtractedText.value = null
            _currentConversionId.value = null
            
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            _documentTitle.value = "Document_${sdf.format(Date())}"
            
            try {
                _isConverting.value = true
                val bitmap = loadBitmapFromUri(context, uri)
                _selectedImageBitmap.value = bitmap
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load image: ${e.message}"
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun loadMockDocument(bitmap: Bitmap, text: String, title: String) {
        _selectedImageUri.value = null
        _selectedImageBitmap.value = bitmap
        _currentExtractedText.value = text
        _currentConversionId.value = null
        _documentTitle.value = title
        _extractionType.value = ExtractionType.DOCUMENT_RECONSTRUCTION
        _errorMessage.value = null
    }

    fun setExtractionType(type: ExtractionType) {
        _extractionType.value = type
    }

    fun setDocumentTitle(title: String) {
        _documentTitle.value = title
    }

    fun startConversion(context: Context) {
        val bitmap = _selectedImageBitmap.value ?: run {
            _errorMessage.value = "Please select an image first."
            return
        }

        viewModelScope.launch {
            _isConverting.value = true
            _errorMessage.value = null
            try {
                val result = repository.convertScreenshot(bitmap, _extractionType.value)
                _currentExtractedText.value = result

                // Copy image to app's private files folder to keep it persistent
                val localPath = saveImageToInternalStorage(context, bitmap, _documentTitle.value)

                val entity = ConversionEntity(
                    title = _documentTitle.value,
                    localImagePath = localPath,
                    extractedText = result,
                    format = if (_extractionType.value == ExtractionType.STANDARD_OCR) "Plain Text" else "Word Doc",
                    extractionType = _extractionType.value.displayName
                )
                val newId = repository.saveConversion(entity)
                _currentConversionId.value = newId.toInt()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unknown error occurred during conversion."
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun updateExtractedText(text: String) {
        _currentExtractedText.value = text
        val currentId = _currentConversionId.value
        if (currentId != null) {
            viewModelScope.launch {
                // Find and update the database record
                val localImage = conversions.value.find { it.id == currentId }?.localImagePath
                val localFormat = conversions.value.find { it.id == currentId }?.format ?: "Word Doc"
                val localExtType = conversions.value.find { it.id == currentId }?.extractionType ?: ExtractionType.DOCUMENT_RECONSTRUCTION.displayName
                
                val updated = ConversionEntity(
                    id = currentId,
                    title = _documentTitle.value,
                    localImagePath = localImage,
                    extractedText = text,
                    format = localFormat,
                    extractionType = localExtType
                )
                repository.updateConversion(updated)
            }
        }
    }

    fun selectSavedConversion(entity: ConversionEntity) {
        _currentConversionId.value = entity.id
        _documentTitle.value = entity.title
        _currentExtractedText.value = entity.extractedText
        _selectedImageUri.value = null
        
        // Load local file path if present
        if (entity.localImagePath != null) {
            val file = File(entity.localImagePath)
            if (file.exists()) {
                _selectedImageBitmap.value = BitmapFactory.decodeFile(file.absolutePath)
            } else {
                _selectedImageBitmap.value = null
            }
        } else {
            _selectedImageBitmap.value = null
        }
    }

    fun clearCurrentSelection() {
        _selectedImageUri.value = null
        _selectedImageBitmap.value = null
        _currentExtractedText.value = null
        _currentConversionId.value = null
        _documentTitle.value = ""
        _errorMessage.value = null
    }

    fun deleteConversion(context: Context, entity: ConversionEntity) {
        viewModelScope.launch {
            repository.deleteConversion(entity.id)
            
            // Delete associated file if it exists
            entity.localImagePath?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // If we are currently viewing this deleted item, reset the screen
            if (_currentConversionId.value == entity.id) {
                clearCurrentSelection()
            }
        }
    }

    fun clearAllHistory(context: Context) {
        viewModelScope.launch {
            // Delete files first
            conversions.value.forEach { entity ->
                entity.localImagePath?.let { path ->
                    try {
                        val file = File(path)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            repository.clearHistory()
            clearCurrentSelection()
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream) ?: throw IllegalArgumentException("Could not decode image.")
    }

    private suspend fun saveImageToInternalStorage(context: Context, bitmap: Bitmap, name: String): String? = withContext(Dispatchers.IO) {
        try {
            val directory = File(context.filesDir, "saved_screenshots").apply { mkdirs() }
            val sanitizedName = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val file = File(directory, "IMG_${sanitizedName}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

class MainViewModelFactory(private val repository: ConversionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.data.repository

import android.graphics.Bitmap
import android.util.Base64
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.InlineData
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.database.ConversionDao
import com.example.data.database.ConversionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

enum class ExtractionType(val displayName: String, val systemPrompt: String) {
    DOCUMENT_RECONSTRUCTION(
        "Document Reconstruction",
        "You are an expert Document Reconstruction and OCR Engine.\n" +
                "Analyze the provided screenshot carefully and extract all of its contents.\n" +
                "Reconstruct the entire layout structure. Convert headers, bullet/numbered lists, paragraphs, formatted bold/italic text, and tables into clean, structured Markdown.\n" +
                "Do NOT include introductory notes, annotations, conversational remarks, or preambles like 'Here is the extracted text:'. Output ONLY the reconstructed markdown document itself.\n" +
                "Make sure tables are output as valid, clean Markdown tables with header separators (|---|---|\n). Use bold, italics, and headings (#, ##, ###) where appropriate to preserve visual hierarchy."
    ),
    STANDARD_OCR(
        "Standard Plain Text OCR",
        "You are an expert OCR Engine.\n" +
                "Extract all text from the provided image in standard, natural reading order, line by line.\n" +
                "Keep paragraphs, headings, and original line breaks intact, but do not force complex formatting or tables.\n" +
                "Output ONLY the extracted plain text in markdown style. Do NOT include conversational intros or outros."
    ),
    TABLE_EXTRACTION(
        "Table & Data Extractor",
        "You are an expert Data Extraction and Tabular Parsing Engine.\n" +
                "Scan the provided screenshot to identify all tables, columns, spreadsheet cells, and grids of structured data.\n" +
                "Extract all values exactly as they appear, preserving rows and columns. Format this data as clean, valid Markdown tables.\n" +
                "If there are multiple tables, separate them with clear section headings. Do not include conversational preambles or chat explanations. Output ONLY the markdown table block."
    )
}

class ConversionRepository(private val conversionDao: ConversionDao) {

    val allConversions: Flow<List<ConversionEntity>> = conversionDao.getAllConversions()

    suspend fun saveConversion(conversion: ConversionEntity): Long = withContext(Dispatchers.IO) {
        conversionDao.insertConversion(conversion)
    }

    suspend fun updateConversion(conversion: ConversionEntity) = withContext(Dispatchers.IO) {
        conversionDao.updateConversion(conversion)
    }

    suspend fun deleteConversion(id: Int) = withContext(Dispatchers.IO) {
        conversionDao.deleteConversionById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        conversionDao.clearAllConversions()
    }

    suspend fun convertScreenshot(
        bitmap: Bitmap,
        type: ExtractionType
    ): String = withContext(Dispatchers.IO) {
        val base64Image = bitmap.toBase64()
        val apiKey = RetrofitClient.getApiKey()

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API key is missing. Please configure it in the AI Studio Secrets panel.")
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                    )
                )
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = type.systemPrompt))
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val extractedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No readable text found in this screenshot. Please try a different or clearer image."
            extractedText.trim()
        } catch (e: Exception) {
            throw Exception("API Error: ${e.message ?: "Unknown error occurred"}")
        }
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        // Compress the bitmap to fit inside request limits while maintaining quality
        var quality = 90
        compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        
        // If image is still too large, we can scale or compress more
        while (outputStream.size() > 1.5 * 1024 * 1024 && quality > 50) {
            outputStream.reset()
            quality -= 10
            compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }
        
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}

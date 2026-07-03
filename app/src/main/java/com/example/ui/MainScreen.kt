package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.ConversionEntity
import com.example.data.repository.ExtractionType
import com.example.util.DocExporter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom modern palette
val SlateDark = Color(0xFF0F172A)
val SlateCard = Color(0xFF1E293B)
val MintGreen = Color(0xFF10B981)
val BlueAccent = Color(0xFF3B82F6)
val TextLight = Color(0xFFF8FAFC)
val TextMuted = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) }

    val selectedImageBitmap by viewModel.selectedImageBitmap.collectAsState()
    val isConverting by viewModel.isConverting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentExtractedText by viewModel.currentExtractedText.collectAsState()
    val documentTitle by viewModel.documentTitle.collectAsState()
    val extractionType by viewModel.extractionType.collectAsState()
    val conversions by viewModel.conversions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showClearHistoryDialog by remember { mutableStateOf(false) }

    // Launcher for image selection
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.selectImage(context, it)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MintGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "ScanToWord",
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextLight,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateDark,
                    titleContentColor = TextLight
                ),
                actions = {
                    if (activeTab == 1 && conversions.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearHistoryDialog = true },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear all history",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            )
        },
        containerColor = SlateDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Elegant M3 TabRow
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = SlateDark,
                contentColor = TextMuted,
                indicator = { tabPositions ->
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = MintGreen,
                        width = 48.dp
                    )
                },
                divider = { HorizontalDivider(color = SlateCard, thickness = 1.dp) }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (activeTab == 0) MintGreen else TextMuted
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Converter",
                                fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == 0) TextLight else TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    },
                    modifier = Modifier.testTag("converter_tab")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (activeTab == 1) MintGreen else TextMuted
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Library",
                                fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == 1) TextLight else TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    },
                    modifier = Modifier.testTag("library_tab")
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(SlateDark)
            ) {
                if (activeTab == 0) {
                    // CONVERTER TAB
                    if (selectedImageBitmap == null) {
                        UploadLandingView(
                            onSelectClick = { imagePickerLauncher.launch("image/*") },
                            onLoadSample = { sampleBitmap, sampleText, sampleTitle ->
                                viewModel.loadMockDocument(sampleBitmap, sampleText, sampleTitle)
                            }
                        )
                    } else {
                        ConversionResultView(
                            bitmap = selectedImageBitmap!!,
                            isConverting = isConverting,
                            extractedText = currentExtractedText,
                            documentTitle = documentTitle,
                            extractionType = extractionType,
                            errorMessage = errorMessage,
                            onTitleChange = { viewModel.setDocumentTitle(it) },
                            onExtractionTypeChange = { viewModel.setExtractionType(it) },
                            onConvertClick = { viewModel.startConversion(context) },
                            onTextChange = { viewModel.updateExtractedText(it) },
                            onCancelClick = { viewModel.clearCurrentSelection() },
                            onDismissError = { viewModel.dismissError() }
                        )
                    }
                } else {
                    // HISTORY LIBRARY TAB
                    LibraryView(
                        conversions = conversions,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onSelectConversion = { entity ->
                            viewModel.selectSavedConversion(entity)
                            activeTab = 0 // Switch back to editor
                        },
                        onDeleteConversion = { viewModel.deleteConversion(context, it) }
                    )
                }
            }
        }

        // Dialog to clear history
        if (showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = { Text("Clear Library", color = TextLight, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to permanently delete all your converted documents from the history? This action cannot be undone.", color = TextMuted) },
                containerColor = SlateCard,
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllHistory(context)
                            showClearHistoryDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("Delete All", color = TextLight)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showClearHistoryDialog = false },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight),
                        border = BorderStroke(1.dp, SlateCard)
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun UploadLandingView(
    onSelectClick: () -> Unit,
    onLoadSample: (Bitmap, String, String) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            // Elegant Hero Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_hero_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(MintGreen.copy(alpha = 0.2f), Color.Transparent)
                                ),
                                shape = RoundedCornerShape(40.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = MintGreen,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Text(
                        text = "Import Your Screenshot",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight,
                            textAlign = TextAlign.Center
                        )
                    )

                    Text(
                        text = "Upload any image or document screenshot. ScanToWord uses the Gemini 3.5 Flash vision API to extract the text, analyze layout rules, and build structured Word documents.",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onSelectClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("select_screenshot_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = SlateDark
                            )
                            Text(
                                "Choose Screenshot",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateDark
                                )
                            )
                        }
                    }
                }
            }
        }

        item {
            // Onboarding instruction highlights
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Features Included",
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateCard, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MintGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("M3 Word Reconstruction", fontWeight = FontWeight.Bold, color = TextLight, fontSize = 14.sp)
                        Text("Translates layouts, headers, tables, and lists into editable docs.", color = TextMuted, fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateCard, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        tint = BlueAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Tabular Table Parser", fontWeight = FontWeight.Bold, color = TextLight, fontSize = 14.sp)
                        Text("Scans grids/spreadsheets and yields structured Markdown tables.", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            // Simulated Sample Document Section (Crucial for emulators!)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "No Screenshots? Try a Quick Demo",
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                SampleItemRow(
                    title = "Monthly Revenue Report",
                    description = "Contains a complete financial spreadsheet table & headers",
                    icon = Icons.Default.TableChart,
                    color = BlueAccent,
                    onGetClick = {
                        val bitmap = generateSampleDocumentBitmap("Monthly Sales Report", "Tabular sales grid demo")
                        val text = """
                            # Monthly Revenue Report - Q3 2026
                            
                            This document shows the sales revenues and totals across products for the third quarter of 2026.
                            
                            ## Quarterly Summary Table
                            
                            | Product ID | Description | Sales Qty | Unit Price | Total Revenue | Status |
                            |:---|:---|:---:|:---:|:---:|:---|
                            | PRD-101 | Cloud Storage Premium | 1,420 | $15.00 | $21,300.00 | COMPLETED |
                            | PRD-102 | Team Workspace Pro | 840 | $24.00 | $20,160.00 | COMPLETED |
                            | PRD-103 | DevTools Ultimate | 210 | $49.00 | $10,290.00 | PENDING |
                            | PRD-104 | Database Cluster Managed | 65 | $199.00 | $12,935.00 | ACTIVE |
                            | PRD-105 | Security Gateway Advanced | 310 | $12.00 | $3,720.00 | COMPLETED |
                            
                            ## Operational Notes
                            
                            - **Cloud Storage** performance grew by 14% compared to the previous quarter.
                            - **DevTools** billing status is pending validation by the accounting department.
                            - Managed database packages show a strong user retention rate of 98.4%.
                        """.trimIndent()
                        onLoadSample(bitmap, text, "Revenue_Report_Demo")
                    }
                )

                SampleItemRow(
                    title = "Project Launch Action Plan",
                    description = "Contains styled headings, bullet checklists, and details",
                    icon = Icons.Default.Article,
                    color = MintGreen,
                    onGetClick = {
                        val bitmap = generateSampleDocumentBitmap("Project Apollo Plan", "Action plans & lists")
                        val text = """
                            # Project Apollo: Phase 1 Action Plan
                            
                            **Launch Date:** October 12, 2026
                            **Project Lead:** Sarah Jenkins (Product Strategy)
                            
                            ## Overview & Objectives
                            
                            The goal of Project Apollo is to overhaul our application infrastructure and deploy the fully modernized Jetpack Compose design system. This document outlines the pre-launch checklists and task owners.
                            
                            ## Pre-Launch Task Checklist
                            
                            - [x] **Design Validation**: Approve final typography, tokens, and asset packs. (Lead: Mark)
                            - [x] **Database Migration**: Deploy Room V2 schemas to private cluster testing. (Lead: David)
                            - [ ] **Security Auditing**: Run full vulnerability analysis and key scans. (Lead: Security Team)
                            - [ ] **App Store Assets**: Upload updated screenshot banners to dashboard. (Lead: Marketing)
                            - [ ] **Deployment Verification**: Smoke test continuous integration server builds. (Lead: QA Team)
                            
                            > **Important Notice**: All blocking tasks must be marked as complete by Thursday night to ensure a seamless Friday morning release.
                        """.trimIndent()
                        onLoadSample(bitmap, text, "Project_Launch_Demo")
                    }
                )
            }
        }
    }
}

@Composable
fun SampleItemRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onGetClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateCard, RoundedCornerShape(12.dp))
            .clickable { onGetClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    color = TextLight,
                    fontSize = 14.sp
                )
                Text(
                    description,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.Default.Input,
            contentDescription = "Load Sample",
            tint = MintGreen,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionResultView(
    bitmap: Bitmap,
    isConverting: Boolean,
    extractedText: String?,
    documentTitle: String,
    extractionType: ExtractionType,
    errorMessage: String?,
    onTitleChange: (String) -> Unit,
    onExtractionTypeChange: (ExtractionType) -> Unit,
    onConvertClick: () -> Unit,
    onTextChange: (String) -> Unit,
    onCancelClick: () -> Unit,
    onDismissError: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var textEditorTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top row navigation/cancel
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onCancelClick,
                modifier = Modifier.testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Go back",
                    tint = TextLight
                )
            }

            OutlinedTextField(
                value = documentTitle,
                onValueChange = onTitleChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .testTag("document_title_input"),
                placeholder = { Text("Document Name", color = TextMuted) },
                singleLine = true,
                textStyle = TextStyle(color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MintGreen,
                    unfocusedBorderColor = SlateCard,
                    focusedContainerColor = SlateCard,
                    unfocusedContainerColor = SlateCard
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Image Preview & Mode Select (Sticky height)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateCard, RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Screenshot Thumbnail
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, SlateDark, RoundedCornerShape(8.dp))
            ) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Selected screenshot preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Conversion details & trigger
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Screenshot Loaded",
                    fontWeight = FontWeight.Bold,
                    color = TextLight,
                    fontSize = 14.sp
                )
                Text(
                    "Dimensions: ${bitmap.width}x${bitmap.height}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                
                if (extractedText == null && !isConverting) {
                    Button(
                        onClick = onConvertClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("convert_button")
                    ) {
                        Text("Convert with Gemini", color = SlateDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Toggle Chip Row (only shown before conversion)
        if (extractedText == null && !isConverting) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Conversion Format Strategy", color = TextLight, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExtractionType.values().forEach { type ->
                        FilterChip(
                            selected = extractionType == type,
                            onClick = { onExtractionTypeChange(type) },
                            label = { Text(type.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MintGreen,
                                selectedLabelColor = SlateDark,
                                containerColor = SlateCard,
                                labelColor = TextLight
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = extractionType == type,
                                borderColor = SlateCard,
                                selectedBorderColor = MintGreen
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_${type.name}")
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Active State Display
        if (isConverting) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(SlateCard, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = MintGreen)
                    Text(
                        "Converting pixels to Word structure...",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Sending document to Gemini 3.5 Flash for OCR transcription, markdown formatting, and table alignment...",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(SlateCard, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Error",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Extraction Failed",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        errorMessage,
                        color = Color(0xFFF87171),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onDismissError,
                        colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                        border = BorderStroke(1.dp, MintGreen)
                    ) {
                        Text("Try Again", color = MintGreen)
                    }
                }
            }
        } else if (extractedText != null) {
            // Results & Formatted Editor
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Secondary Editor Tabs (Edit vs HTML Preview)
                TabRow(
                    selectedTabIndex = textEditorTab,
                    containerColor = SlateCard,
                    contentColor = TextMuted,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[textEditorTab]),
                            color = MintGreen
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                ) {
                    Tab(
                        selected = textEditorTab == 0,
                        onClick = { textEditorTab = 0 },
                        text = { Text("Edit Content", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (textEditorTab == 0) TextLight else TextMuted) },
                        modifier = Modifier.testTag("edit_tab")
                    )
                    Tab(
                        selected = textEditorTab == 1,
                        onClick = { textEditorTab = 1 },
                        text = { Text("Word Layout Preview", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (textEditorTab == 1) TextLight else TextMuted) },
                        modifier = Modifier.testTag("preview_tab")
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            SlateCard,
                            RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .border(
                            1.dp,
                            SlateCard,
                            RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .padding(16.dp)
                ) {
                    if (textEditorTab == 0) {
                        // Raw Editor (Full text)
                        OutlinedTextField(
                            value = extractedText,
                            onValueChange = onTextChange,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("extracted_text_editor"),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                color = TextLight,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    } else {
                        // Word Render Preview
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    "Visualized Layout Preview",
                                    color = MintGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            item {
                                RenderMarkdownAsUI(extractedText)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Document Exporter Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val uri = DocExporter.exportToWordFile(context, documentTitle, extractedText)
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/msword"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Word Document"))
                            } else {
                                Toast.makeText(context, "Failed to generate Word file", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_word_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.PostAdd, contentDescription = null, tint = SlateDark, modifier = Modifier.size(18.dp))
                            Text("Share Word Doc", fontWeight = FontWeight.Bold, color = SlateDark, fontSize = 13.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val uri = DocExporter.exportToTextFile(context, documentTitle, extractedText)
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Text File"))
                            } else {
                                Toast.makeText(context, "Failed to generate Text file", Toast.LENGTH_SHORT).show()
                            }
                        },
                        border = BorderStroke(1.dp, BlueAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_text_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(18.dp))
                            Text("Share TXT", fontWeight = FontWeight.Bold, color = BlueAccent, fontSize = 13.sp)
                        }
                    }

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(extractedText))
                            Toast.makeText(context, "Copied content to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(SlateCard, RoundedCornerShape(12.dp))
                            .testTag("copy_text_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy text to clipboard",
                            tint = TextLight
                        )
                    }
                }
            }
        } else {
            // Screen state: Selected screenshot, not converted, waiting for convert trigger
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(SlateCard, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MintGreen,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        "Ready to Convert",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Click the 'Convert with Gemini' button above to automatically run OCR transcription and document layout reconstruction.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryView(
    conversions: List<ConversionEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectConversion: (ConversionEntity) -> Unit,
    onDeleteConversion: (ConversionEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_history_input"),
            placeholder = { Text("Search your document library...", color = TextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextMuted
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = TextMuted
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MintGreen,
                unfocusedBorderColor = SlateCard,
                focusedContainerColor = SlateCard,
                unfocusedContainerColor = SlateCard,
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight
            ),
            shape = RoundedCornerShape(12.dp)
        )

        if (conversions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = SlateCard,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No Matching Documents" else "Library is Empty",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try adjusting your search criteria" else "Scanned screenshots and converted files appear here.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("history_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(conversions, key = { it.id }) { item ->
                    HistoryItemCard(
                        entity = item,
                        onClick = { onSelectConversion(item) },
                        onDeleteClick = { onDeleteConversion(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    entity: ConversionEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    val dateString = sdf.format(Date(entity.timestamp))
    
    // Load bitmap thumbnail from local files
    val bitmapThumbnail = remember(entity.localImagePath) {
        if (entity.localImagePath != null) {
            val file = File(entity.localImagePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } else {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_item_${entity.id}"),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SlateDark)
            ) {
                if (bitmapThumbnail != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmapThumbnail.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp)
                        )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Metadata
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = entity.title,
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    // Format badge
                    Box(
                        modifier = Modifier
                            .background(
                                if (entity.format == "Word Doc") MintGreen.copy(alpha = 0.15f) else BlueAccent.copy(alpha = 0.15f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = entity.format,
                            color = if (entity.format == "Word Doc") MintGreen else BlueAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = entity.extractionType,
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Text(
                    text = dateString,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete button
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.testTag("delete_item_button_${entity.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Basic Markdown UI renderer to display formatted preview inside Compose
 */
@Composable
fun RenderMarkdownAsUI(markdown: String) {
    val lines = markdown.lines()
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        var inList = false
        var inTable = false
        var tableHeaders: List<String>? = null
        
        for (rawLine in lines) {
            val line = rawLine.trim()
            
            // Handle table close
            if (inTable && !line.startsWith("|")) {
                inTable = false
                tableHeaders = null
            }

            // Skip table formatting dashes
            if (line.matches(Regex("^\\|?\\s*[-:]+\\s*\\|.*")) || line.matches(Regex("^\\|?\\s*[:---:]+\\s*\\|.*"))) {
                continue
            }

            when {
                line.startsWith("# ") -> {
                    Text(
                        text = line.substring(2),
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.substring(3),
                        color = MintGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                line.startsWith("### ") -> {
                    Text(
                        text = line.substring(4),
                        color = BlueAccent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                line.startsWith("|") -> {
                    val cols = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    if (!inTable) {
                        inTable = true
                        tableHeaders = cols
                        // Draw header row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MintGreen.copy(alpha = 0.1f))
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in cols) {
                                Text(
                                    text = col,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MintGreen,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        // Draw standard data row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SlateDark)
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in cols) {
                                Text(
                                    text = col,
                                    fontSize = 11.sp,
                                    color = TextLight,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(
                        modifier = Modifier.padding(start = 12.dp).padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("•  ", color = MintGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = line.substring(2),
                            color = TextLight,
                            fontSize = 12.sp
                        )
                    }
                }
                line.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val match = Regex("^\\d+\\.\\s+(.*)").find(line)
                    val num = line.substringBefore(".")
                    val content = match?.groupValues?.get(1) ?: line
                    Row(
                        modifier = Modifier.padding(start = 12.dp).padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("$num.  ", color = BlueAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = content,
                            color = TextLight,
                            fontSize = 12.sp
                        )
                    }
                }
                line.startsWith("> ") -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateDark.copy(alpha = 0.3f))
                            .border(1.dp, SlateDark)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = line.substring(2),
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
                line.isNotEmpty() -> {
                    Text(
                        text = line,
                        color = TextLight,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Generates a mock styled Document Bitmap for the live gallery preview.
 * This ensures that users always see a document preview when running the demo files!
 */
fun generateSampleDocumentBitmap(title: String, desc: String): Bitmap {
    val bitmap = Bitmap.createBitmap(400, 500, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        isAntiAlias = true
    }

    // 1. Background
    paint.color = android.graphics.Color.WHITE
    canvas.drawRect(0f, 0f, 400f, 500f, paint)

    // 2. Simulated header strip
    paint.color = android.graphics.Color.rgb(27, 54, 93) // Word Blue
    canvas.drawRect(0f, 0f, 400f, 80f, paint)

    // 3. Document Title text
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 22f
    paint.isFakeBoldText = true
    canvas.drawText("MS WORD TEMPLATE", 30f, 48f, paint)

    // 4. Document Body details
    paint.color = android.graphics.Color.DKGRAY
    paint.textSize = 18f
    paint.isFakeBoldText = true
    canvas.drawText(title, 30f, 130f, paint)

    paint.color = android.graphics.Color.GRAY
    paint.textSize = 13f
    paint.isFakeBoldText = false
    canvas.drawText("Sub-Header: $desc", 30f, 160f, paint)

    // 5. Drawing placeholder lines representing text paragraphs
    paint.color = android.graphics.Color.LTGRAY
    var currentY = 190f
    for (i in 0..4) {
        canvas.drawRect(30f, currentY, 370f, currentY + 10f, paint)
        currentY += 18f
    }

    // 6. Drawing placeholder table grid
    paint.color = android.graphics.Color.rgb(235, 248, 255) // Blue tint table head
    canvas.drawRect(30f, currentY, 370f, currentY + 25f, paint)
    paint.color = android.graphics.Color.rgb(43, 108, 176)
    canvas.drawLine(30f, currentY, 370f, currentY, paint)
    canvas.drawLine(30f, currentY + 25f, 370f, currentY + 25f, paint)

    // Grid vertical lines
    canvas.drawLine(30f, currentY, 30f, currentY + 120f, paint)
    canvas.drawLine(150f, currentY, 150f, currentY + 120f, paint)
    canvas.drawLine(270f, currentY, 270f, currentY + 120f, paint)
    canvas.drawLine(370f, currentY, 370f, currentY + 120f, paint)

    // Grid horizontal data rows
    paint.color = android.graphics.Color.LTGRAY
    currentY += 25f
    for (i in 0..2) {
        canvas.drawLine(30f, currentY, 370f, currentY, paint)
        currentY += 30f
    }
    paint.color = android.graphics.Color.rgb(43, 108, 176)
    canvas.drawLine(30f, currentY, 370f, currentY, paint)

    return bitmap
}

/**
 * Custom border modifier utility for styling M3 outlined components without XML layouts
 */
fun border(width: androidx.compose.ui.unit.Dp, color: Color, shape: RoundedCornerShape = RoundedCornerShape(0.dp)): Modifier {
    return Modifier.border(width, color, shape)
}

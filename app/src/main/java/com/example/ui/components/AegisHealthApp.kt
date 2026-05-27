package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.data.healthconnect.HealthConnectManager
import com.example.data.healthconnect.HealthConnectPermissionScreen
import com.example.ui.components.NotificationPermissionScreen
import com.example.data.common.Result
import com.example.data.database.entity.BiometricTelemetryEntity
import com.example.data.database.entity.UserLifestyleLogEntity
import com.example.data.database.entity.EnvironmentalLogEntity
import com.example.data.network.dto.TherapyInsight
import com.example.ui.HealthViewModel
import com.example.ui.components.TherapyInsightCard
import java.text.SimpleDateFormat
import java.util.*
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.togetherWith

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AegisHealthApp(viewModel: HealthViewModel) {
    val telemetryState by viewModel.telemetryState.collectAsState()
    val latestTelemetryState by viewModel.latestTelemetryState.collectAsState()
    val lifestyleState by viewModel.lifestyleState.collectAsState()
    val latestAqiState by viewModel.latestAqiState.collectAsState()
    val therapyInsightState by viewModel.therapyInsightState.collectAsState()
    val userProfile by viewModel.userProfileState.collectAsState()
    val latestBloodReportState by viewModel.latestBloodReportState.collectAsState()

    var showAddTelemetryDialog by remember { mutableStateOf(false) }
    var showAddLifestyleDialog by remember { mutableStateOf(false) }
    var showUserProfileDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Bento Dashboard, 1: Ledgers & Reports

    val baseBgColor = Color(0xFFFDF8FF)
    val headerPurple = Color(0xFF21005D)
    val dividerColor = Color(0xFFE7E0EC)

    Scaffold(
        containerColor = baseBgColor,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Avatar initials from profile
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEADDFF))
                                    .clickable { showUserProfileDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userProfile.initials,
                                    color = headerPurple,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "VitalSecure",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.3).sp,
                                        color = Color(0xFF1C1B1F)
                                    )
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2E7D32))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "SQLCipher Encrypted",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF49454F),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                }
                            }
                        }

                        // DB encryption status indicator
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .border(1.dp, dividerColor, CircleShape)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "SQLCipher Crypt Engine Active",
                                tint = headerPurple,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = baseBgColor
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 0.dp,
                color = Color(0xFFF3EDF7),
                modifier = Modifier.border(width = 1.dp, color = dividerColor, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Column {
                    // Operational Command Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.seedSampleHealthData() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("seed_record_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEADDFF),
                                contentColor = headerPurple
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Seed Sample Health Log",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Seed Data", fontSize = 13.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (selectedTab == 0) showAddTelemetryDialog = true
                                else showAddLifestyleDialog = true
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("add_log_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = headerPurple,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add health records",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (selectedTab == 0) "Add Vitals" else "Add Lifestyle", fontSize = 13.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = { viewModel.clearAllData() },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("clear_all_button")
                                .background(
                                    Color(0xFFF9DEDC),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Wipe Database",
                                tint = Color(0xFF410E0B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // MD3 Navigation system
                        Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tab Dashboard
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedTab = 0 }
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selectedTab == 0) Color(0xFFEADDFF) else Color.Transparent)
                                    .padding(horizontal = 20.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Dashboard",
                                    tint = if (selectedTab == 0) headerPurple else Color(0xFF49454F),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Dashboard",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) headerPurple else Color(0xFF49454F)
                            )
                        }

                        // Tab Ledgers
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedTab = 1 }
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selectedTab == 1) Color(0xFFEADDFF) else Color.Transparent)
                                    .padding(horizontal = 20.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = "Reports Ledger",
                                    tint = if (selectedTab == 1) headerPurple else Color(0xFF49454F),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Ledgers",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) headerPurple else Color(0xFF49454F)
                            )
                        }

                        // Tab Diet Advice
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedTab = 2 }
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selectedTab == 2) Color(0xFFEADDFF) else Color.Transparent)
                                    .padding(horizontal = 20.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.FavoriteBorder,
                                    contentDescription = "Diet Advice",
                                    tint = if (selectedTab == 2) headerPurple else Color(0xFF49454F),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Diet Advice",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 2) headerPurple else Color(0xFF49454F)
                            )
                        }

                        // Tab Medications
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedTab = 3 }
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selectedTab == 3) Color(0xFFEADDFF) else Color.Transparent)
                                    .padding(horizontal = 20.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Info,
                                    contentDescription = "Medications",
                                    tint = if (selectedTab == 3) headerPurple else Color(0xFF49454F),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Medications",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 3) headerPurple else Color(0xFF49454F)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = selectedTab,
                label = "tab_transition",
                transitionSpec = {
                    androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) togetherWith androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
                },
                modifier = Modifier.weight(1f)
            ) { targetTab ->
                if (targetTab == 0) {
                    BentoDashboardContent(
                        latestTelemetryState = latestTelemetryState,
                        lifestyleState = lifestyleState,
                        telemetryState = telemetryState,
                        latestAqiState = latestAqiState,
                        therapyInsightState = therapyInsightState,
                        latestBloodReportState = latestBloodReportState,
                        viewModel = viewModel
                    )
                } else if (targetTab == 1) {
                    // Secondary Table inspectors (Historical Ledger databases)
                    BentoLedgerTab(
                        telemetryState = telemetryState,
                        lifestyleState = lifestyleState,
                        viewModel = viewModel
                    )
                } else if (targetTab == 2) {
                    DietAdviceContent(viewModel = viewModel)
                } else if (targetTab == 3) {
                    MedicationsContent(viewModel = viewModel)
                }
            }
        }
    }

    // Modal dialog overlays
    if (showAddTelemetryDialog) {
        AddTelemetryDialog(
            onDismiss = { showAddTelemetryDialog = false },
            onSave = { hr, hrv, spo2 ->
                viewModel.addTelemetry(hr, hrv.toDouble(), spo2.toDouble())
                showAddTelemetryDialog = false
            }
        )
    }

    if (showAddLifestyleDialog) {
        AddLifestyleDialog(
            onDismiss = { showAddLifestyleDialog = false },
            onSave = { caffeine, alcohol, stress ->
                viewModel.addLifestyleLog(caffeine, alcohol.toDouble(), stress)
                showAddLifestyleDialog = false
            }
        )
    }

    if (showUserProfileDialog) {
        UserProfileDialog(
            profile = userProfile,
            onDismiss = { showUserProfileDialog = false },
            onSave = { name, age, weight, height, reportStatus ->
                viewModel.updateUserProfile(name, age, weight, height, reportStatus)
                showUserProfileDialog = false
            },
            onAnalyzeReport = { bitmap ->
                viewModel.analyzeBloodReportImage(bitmap)
            }
        )
    }
}

@Composable
fun UserProfileDialog(
    profile: com.example.data.user.UserProfile,
    onDismiss: () -> Unit,
    onSave: (String, Int, Float, Float, String) -> Unit,
    onAnalyzeReport: (android.graphics.Bitmap) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var ageStr by remember { mutableStateOf(profile.age.toString()) }
    var weightStr by remember { mutableStateOf(profile.weightKg.toString()) }
    var heightStr by remember { mutableStateOf(profile.heightCm.toString()) }
    var reportStatus by remember { mutableStateOf(profile.bloodReportStatus) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val pickMedia = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            reportStatus = "Analyzing AI..."
            val bitmap = if (android.os.Build.VERSION.SDK_INT >= 28) {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            // ensure mutable copy in software mode to avoid hardware bitmap issues with ML models
            val softwareBitmap = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
            onAnalyzeReport(softwareBitmap)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("User Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") }
                )
                OutlinedTextField(
                    value = ageStr,
                    onValueChange = { ageStr = it },
                    label = { Text("Age") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = heightStr,
                    onValueChange = { heightStr = it },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = reportStatus,
                    onValueChange = { reportStatus = it },
                    label = { Text("Blood Report Status") }
                )
                Button(
                    onClick = { 
                        pickMedia.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Upload & Analyze Blood Report")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val age = ageStr.toIntOrNull() ?: profile.age
                    val weight = weightStr.toFloatOrNull() ?: profile.weightKg
                    val height = heightStr.toFloatOrNull() ?: profile.heightCm
                    onSave(name, age, weight, height, reportStatus)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsContent(viewModel: com.example.ui.HealthViewModel) {
    val medicationsState by viewModel.medicationState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Color(0xFF21005D)) {
                Icon(Icons.Default.Add, contentDescription = "Add Medication", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Medication Tracker", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
            Spacer(Modifier.height(16.dp))

            when (val state = medicationsState) {
                is Result.Loading -> CircularProgressIndicator()
                is Result.Error -> Text("Error loading medications")
                is Result.Success -> {
                    val meds = state.data
                    if (meds.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No medications scheduled.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn {
                            items(meds) { med ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (med.isTaken) Color(0xFFE8DEF8) else Color.White),
                                    elevation = CardDefaults.elevatedCardElevation(if (med.isTaken) 0.dp else 2.dp)
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(med.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textDecoration = if (med.isTaken) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
                                            Text("${med.dosage} - ${med.frequency}", style = MaterialTheme.typography.bodyMedium)
                                            val timeString = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(med.targetTimeMs))
                                            Text("Time: $timeString", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        }
                                        Checkbox(
                                            checked = med.isTaken,
                                            onCheckedChange = { viewModel.toggleMedicationTakenStatus(med.id, it) },
                                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF21005D))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var dosage by remember { mutableStateOf("") }
        var frequency by remember { mutableStateOf("Daily") }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Medication") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    OutlinedTextField(value = dosage, onValueChange = { dosage = it }, label = { Text("Dosage (e.g. 1 pill, 50mg)") })
                    OutlinedTextField(value = frequency, onValueChange = { frequency = it }, label = { Text("Frequency") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cal = java.util.Calendar.getInstance()
                    cal.add(java.util.Calendar.SECOND, 30) // Set 30 secs for demo testing
                    viewModel.addMedication(name, dosage, frequency, cal.timeInMillis)
                    showAddDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Highly polished visual Bento Grid implementation
@Composable
fun DietAdviceContent(viewModel: com.example.ui.HealthViewModel) {
    val dietState by viewModel.dietInsightState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.fetchDietAdvice()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Personalized Diet Advice",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF21005D)
        )
        Text(
            text = "Based on your clinical telemetry, physical profile (BMI) and blood report features.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Button(
            onClick = { viewModel.fetchDietAdvice() },
            modifier = Modifier.fillMaxWidth().testTag("refresh_diet_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21005D))
        ) {
            Text("Generate New Diet Plan")
        }

        when (val state = dietState) {
            is Result.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF21005D))
                }
            }
            is Result.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9DEDC))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Error Generating Diet",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF410E0B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.throwable.message ?: "Unknown error",
                            color = Color(0xFF410E0B)
                        )
                    }
                }
            }
            is Result.Success -> {
                val insight = state.data
                if (insight != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Summary & Focus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
                            Spacer(Modifier.height(8.dp))
                            Text(insight.deficiency_focus, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Target Calories: ${insight.daily_calories} kcal", fontWeight = FontWeight.SemiBold, color = Color(0xFF21005D))
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.elevatedCardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Meal Plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            
                            val meals = listOf(
                                "Breakfast" to insight.breakfast_recommendation,
                                "Lunch" to insight.lunch_recommendation,
                                "Snacks" to insight.snacks_recommendation,
                                "Dinner" to insight.dinner_recommendation
                            )
                            
                            meals.forEach { (mealTitle, content) ->
                                Text(mealTitle, fontWeight = FontWeight.SemiBold, color = Color(0xFF6750A4))
                                Text(content, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                    
                    if (insight.actionable_tips.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8DEF8))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Actionable Tips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
                                Spacer(Modifier.height(8.dp))
                                insight.actionable_tips.forEach { tip ->
                                    Row(modifier = Modifier.padding(bottom = 6.dp)) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF6750A4), modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(tip, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text("No diet info available. Please generate.", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
fun BentoDashboardContent(
    latestTelemetryState: Result<BiometricTelemetryEntity?>,
    lifestyleState: Result<List<UserLifestyleLogEntity>>,
    telemetryState: Result<List<BiometricTelemetryEntity>>,
    latestAqiState: Result<EnvironmentalLogEntity?>,
    therapyInsightState: Result<TherapyInsight?>,
    latestBloodReportState: Result<com.example.data.database.entity.BloodReportEntity?>,
    viewModel: HealthViewModel
) {
    val context = LocalContext.current
    val manager = remember { HealthConnectManager(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Notification Permissions Integration
        item {
            NotificationPermissionScreen()
        }

        // Health Connect Permissions Integration
        item {
            HealthConnectPermissionScreen(
                manager = manager,
                onPermissionsGranted = {
                    viewModel.startHealthConnectSync()
                }
            )
        }

        // AQI Environmental Context Card
        item {
            val aqiEntity = (latestAqiState as? Result.Success)?.data
            val telemetryEntity = (latestTelemetryState as? Result.Success)?.data
            AqiContextCard(
                aqiEntity = aqiEntity,
                telemetryEntity = telemetryEntity,
                viewModel = viewModel
            )
        }

        // Therapy Insight Card
        item {
            TherapyInsightCard(
                insightState = therapyInsightState,
                onFetchInsight = { question -> viewModel.fetchPersonalizedTherapies(question) }
            )
        }

        // Blood Report Insights
        item {
            val bloodReportEntity = (latestBloodReportState as? Result.Success)?.data
            if (bloodReportEntity != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF9F9FF)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "AI Blood Analysis",
                                tint = Color(0xFF21005D),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Parsed Blood Report (AI Vision)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF21005D)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Cholesterol", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                Text(bloodReportEntity.cholesterol ?: "--", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Glucose", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                Text(bloodReportEntity.glucose ?: "--", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Hemoglobin", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                Text(bloodReportEntity.hemoglobin ?: "--", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Section header
        item {
            Text(
                text = "BIOMETRICS OVERVIEW",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF49454F),
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            )
        }

        // BENTO CARD 1: Large Heart rate & HRV block (D0BCFF background)
        item {
            val telemetryOrNull = when (latestTelemetryState) {
                is Result.Success -> latestTelemetryState.data
                else -> null
            }
            val heartRate = telemetryOrNull?.heartRate ?: 72
            val hrv = telemetryOrNull?.hrv ?: 48

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFD0BCFF)
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Soft decorative vector heart shape on background
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFF21005D).copy(alpha = 0.05f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 24.dp, y = 24.dp)
                            .size(160.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "BiometricTelemetry",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF21005D)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$heartRate",
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF21005D),
                                    lineHeight = 44.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "BPM",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF21005D),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Vico Interactive Chart
                            Box(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                InteractiveHeartRateChart(telemetryState, (latestBloodReportState as? Result.Success)?.data)
                            }

                            // Dynamic Pill showing HRV
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.3f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "HRV ${hrv}ms",
                                    color = Color(0xFF21005D),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Row of Side-by-Side Bento Blocks (SpO2 & Lifestyle summary)
        item {
            val telemetryOrNull = when (latestTelemetryState) {
                is Result.Success -> latestTelemetryState.data
                else -> null
            }
            val spO2 = telemetryOrNull?.spO2 ?: 98

            val latestLifestyle = when (lifestyleState) {
                is Result.Success -> lifestyleState.data.firstOrNull()
                else -> null
            }
            val caffeine = latestLifestyle?.caffeineMg ?: 250
            val stressScore = latestLifestyle?.subjectiveStressScore ?: 4

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // BENTO CARD 2: Oxygen Saturation (SpO2, E8DEF8 background)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8DEF8)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "SpO2",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D192B)
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6750A4))
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    "$spO2",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D192B)
                                )
                                Text(
                                    "%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF1D192B),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Repository: Online",
                                fontSize = 9.sp,
                                color = Color(0xFF49454F)
                            )
                        }
                    }
                }

                // BENTO CARD 3: Quick Lifestyle factors log summary
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF3EDF7)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE7E0EC))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "UserLifestyleLog",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D192B)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Caffeine
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("☕ Caffeine", fontSize = 10.sp, color = Color(0xFF1C1B1F))
                                    Text("${caffeine}mg", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE7E0EC))
                                ) {
                                    val progressFraction = (caffeine.toFloat() / 400f).coerceIn(0f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progressFraction)
                                            .clip(CircleShape)
                                            .background(Color(0xFF6750A4))
                                    )
                                }
                            }

                            // Stress
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("⚡ Stress Score", fontSize = 10.sp, color = Color(0xFF1C1B1F))
                                Text(
                                    text = when {
                                        stressScore < 4 -> "Level: Low"
                                        stressScore < 8 -> "Level: Med"
                                        else -> "Level: High"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6750A4)
                                )
                            }
                        }
                    }
                }
            }
        }

        // System / Secure Layer Header
        item {
            Text(
                text = "CRYPTOGRAPHIC ENGINE PERFORMANCE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF49454F),
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
            )
        }

        // BENTO CARD 4: Crypto Security Layer details (SQLCipher stats)
        item {
            // Compute telemetry and lifestyle row count for live feed display
            val totalBiometricsCount = when (telemetryState) {
                is Result.Success -> telemetryState.data.size
                else -> 0
            }
            val totalLifestyleCount = when (lifestyleState) {
                is Result.Success -> lifestyleState.data.size
                else -> 0
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFFF3EDF7))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "DAO Layer Status",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1F)
                            )
                        }
                        // Tag
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color(0xFFE7E0EC), RoundedCornerShape(6.dp))
                                .background(Color(0xFFF7F2FA), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "v4.5.4-SQLC",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF1D192B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "SafeRoomOpenHelperFactory.getInstance()",
                        fontSize = 11.sp,
                        color = Color(0xFF49454F)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Active Table Blocks",
                                fontSize = 9.sp,
                                color = Color(0xFF49454F)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${totalBiometricsCount + totalLifestyleCount}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1F)
                            )
                        }

                        Column {
                            Text(
                                "Encryption Latency",
                                fontSize = 9.sp,
                                color = Color(0xFF49454F)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "0.4 ms",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1F)
                            )
                        }

                        Column {
                            Text(
                                "Raw entropy entropy",
                                fontSize = 9.sp,
                                color = Color(0xFF49454F)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "256-bit",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Reports & Ledger list view
@Composable
fun BentoLedgerTab(
    telemetryState: Result<List<BiometricTelemetryEntity>>,
    lifestyleState: Result<List<UserLifestyleLogEntity>>,
    viewModel: HealthViewModel
) {
    var ledgerTabIndex by remember { mutableStateOf(0) } // 0: biometrics list, 1: lifestyle list, 2: blood reports & trends

    val trendsState by viewModel.bloodReportTrendsState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Tab row header specifically built for tables
        TabRow(
            selectedTabIndex = ledgerTabIndex,
            containerColor = Color.Transparent,
            contentColor = Color(0xFF21005D),
            divider = {}
        ) {
            Tab(
                selected = ledgerTabIndex == 0,
                onClick = { ledgerTabIndex = 0 },
                text = { Text("Biometrics Ledger", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.Favorite, contentDescription = "Biometrics", modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = ledgerTabIndex == 1,
                onClick = { ledgerTabIndex = 1 },
                text = { Text("Lifestyle Logs", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.Star, contentDescription = "Lifestyle", modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = ledgerTabIndex == 2,
                onClick = { ledgerTabIndex = 2 },
                text = { Text("Blood Trends", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.Face, contentDescription = "Blood Reports", modifier = Modifier.size(16.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        if (ledgerTabIndex == 0) {
            // Include interactive chart in Ledger top area
            val bloodReportData = (viewModel.latestBloodReportState.collectAsState().value as? Result.Success)?.data
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9FF))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Heart Rate History", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    InteractiveHeartRateChart(telemetryState, bloodReportData)
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (ledgerTabIndex == 0) {
                TelemetryList(
                    state = telemetryState,
                    onDelete = { id -> viewModel.deleteTelemetry(id) }
                )
            } else if (ledgerTabIndex == 1) {
                LifestyleList(
                    state = lifestyleState,
                    onDelete = { id -> viewModel.deleteLifestyleLog(id) }
                )
            } else if (ledgerTabIndex == 2) {
                BloodTrendsTabContent(viewModel = viewModel, trendsState = trendsState)
            }
        }
    }
}

@Composable
fun BloodTrendsTabContent(
    viewModel: HealthViewModel,
    trendsState: Result<String?>
) {
    LaunchedEffect(Unit) {
        viewModel.fetchBloodReportTrends()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("AI Trend Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        when (trendsState) {
            is Result.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF6750A4))
                }
            }
            is Result.Error -> {
                Text("Error analyzing trends: ${trendsState.throwable.localizedMessage}", color = Color.Red)
            }
            is Result.Success -> {
                val text = trendsState.data
                if (text.isNullOrBlank()) {
                    Text("No blood report trends available.", color = Color.Gray)
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7))
                    ) {
                        Text(text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryList(
    state: Result<List<BiometricTelemetryEntity>>,
    onDelete: (Long) -> Unit
) {
    when (state) {
        is Result.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF21005D))
            }
        }
        is Result.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Stream Read Error: ${state.throwable.localizedMessage}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        is Result.Success -> {
            val list = state.data
            if (list.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Favorite,
                    title = "Biometrics DB Empty",
                    description = "Write raw metric indexes onto the physical disk. Decryption occurs purely in-memory using 256-bit AES cipher wrappers."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(list, key = { it.id }) { item ->
                        TelemetryRow(item, onDelete = onDelete)
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryRow(
    item: BiometricTelemetryEntity,
    onDelete: (Long) -> Unit
) {
    val dateString = remember(item.timestamp) {
        SimpleDateFormat("MMM dd, yyyy - HH:mm:ss", Locale.getDefault()).format(Date(item.timestamp))
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("telemetry_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFE7E0EC)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF21005D)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("HEART RATE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                        Text(
                            text = "${item.heartRate} BPM",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F)
                        )
                    }
                    Column {
                        Text("HRV VALUE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                        Text(
                            text = "${item.hrv} ms",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D192B)
                        )
                    }
                    Column {
                        Text("SPO2 INDEX", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                        Text(
                            text = "${item.spO2}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D192B)
                        )
                    }
                }
            }
            IconButton(
                onClick = { onDelete(item.id) },
                modifier = Modifier
                    .testTag("delete_telemetry_${item.id}")
                    .background(Color(0xFFF7F2FA), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete record",
                    tint = Color(0xFFB3261E),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun LifestyleList(
    state: Result<List<UserLifestyleLogEntity>>,
    onDelete: (Long) -> Unit
) {
    when (state) {
        is Result.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF21005D))
            }
        }
        is Result.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Stream Read Error: ${state.throwable.localizedMessage}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        is Result.Success -> {
            val list = state.data
            if (list.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Star,
                    title = "Lifestyle DB Empty",
                    description = "Monitor physical lifestyle variable logs locally. Dynamic page ciphers prevent unauthorized physical device extractions."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(list, key = { it.id }) { item ->
                        LifestyleRow(item, onDelete = onDelete)
                    }
                }
            }
        }
    }
}

@Composable
fun LifestyleRow(
    item: UserLifestyleLogEntity,
    onDelete: (Long) -> Unit
) {
    val dateString = remember(item.timestamp) {
        SimpleDateFormat("MMM dd, yyyy - HH:mm:ss", Locale.getDefault()).format(Date(item.timestamp))
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lifestyle_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFE7E0EC)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6750A4)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("☕ CAFFEINE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                        Text(
                            text = "${item.caffeineMg} mg",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F)
                        )
                    }
                    Column {
                        Text("ALCOHOL UNITS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                        Text(
                            text = "${item.alcoholUnits} Units",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D192B)
                        )
                    }
                    Column {
                        Text("STRESS INDEX", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                        Text(
                            text = "${item.subjectiveStressScore}/10",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D192B)
                        )
                    }
                }
            }
            IconButton(
                onClick = { onDelete(item.id) },
                modifier = Modifier
                    .testTag("delete_lifestyle_${item.id}")
                    .background(Color(0xFFF7F2FA), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete record",
                    tint = Color(0xFFB3261E),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color(0xFF6750A4).copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1B1F)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF49454F),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun AddTelemetryDialog(
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int) -> Unit
) {
    var heartRate by remember { mutableStateOf("") }
    var hrv by remember { mutableStateOf("") }
    var spO2 by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Secured Vitals", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = heartRate,
                    onValueChange = { heartRate = it.filter { c -> c.isDigit() } },
                    label = { Text("Heart Rate (BPM)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_heart_rate")
                )
                OutlinedTextField(
                    value = hrv,
                    onValueChange = { hrv = it.filter { c -> c.isDigit() } },
                    label = { Text("HRV (ms)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_hrv")
                )
                OutlinedTextField(
                    value = spO2,
                    onValueChange = { spO2 = it.filter { c -> c.isDigit() } },
                    label = { Text("Blood Oxygen SpO2 (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_spo2")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hrVal = heartRate.toIntOrNull() ?: 72
                    val hrvVal = hrv.toIntOrNull() ?: 55
                    val spo2Val = spO2.toIntOrNull() ?: 98
                    onSave(hrVal, hrvVal, spo2Val)
                },
                modifier = Modifier.testTag("submit_telemetry_button")
            ) {
                Text("Encrypt & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddLifestyleDialog(
    onDismiss: () -> Unit,
    onSave: (Int, Float, Int) -> Unit
) {
    var caffeine by remember { mutableStateOf("") }
    var alcohol by remember { mutableStateOf("") }
    var stressScore by remember { mutableFloatStateOf(5f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Lifestyle Factors", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = caffeine,
                    onValueChange = { caffeine = it.filter { c -> c.isDigit() } },
                    label = { Text("Caffeine Intake (mg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_caffeine")
                )
                OutlinedTextField(
                    value = alcohol,
                    onValueChange = { alcohol = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Alcohol (Standard Units)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_alcohol")
                )
                Column {
                    Text(
                        text = "Subjective Stress Level: ${stressScore.toInt()}/10",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = stressScore,
                        onValueChange = { stressScore = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_stress_slider")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val caffeineVal = caffeine.toIntOrNull() ?: 0
                    val alcoholVal = alcohol.toFloatOrNull() ?: 0f
                    onSave(caffeineVal, alcoholVal, stressScore.toInt())
                },
                modifier = Modifier.testTag("submit_lifestyle_button")
            ) {
                Text("Encrypt & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LiveTelemetryPanel(latestTelemetryState: Result<BiometricTelemetryEntity?>) {
    val telemetryOrNull = when (latestTelemetryState) {
        is Result.Success -> latestTelemetryState.data
        else -> null
    }
    val heartRate = telemetryOrNull?.heartRate ?: 72
    val hrv = telemetryOrNull?.hrv ?: 48
    val spO2 = telemetryOrNull?.spO2 ?: 98

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFD0BCFF)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFF21005D).copy(alpha = 0.05f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 24.dp, y = 24.dp)
                    .size(160.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "BiometricTelemetry (Real-time)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF21005D)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$heartRate",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF21005D),
                            lineHeight = 44.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BPM",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF21005D),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Oxygen SpO2: $spO2%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF21005D)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Box(modifier = Modifier.width(4.dp).height(8.dp).clip(CircleShape).background(Color(0xFF21005D).copy(alpha = 0.4f)))
                            Box(modifier = Modifier.width(4.dp).height(14.dp).clip(CircleShape).background(Color(0xFF21005D).copy(alpha = 0.6f)))
                            Box(modifier = Modifier.width(4.dp).height(22.dp).clip(CircleShape).background(Color(0xFF21005D)))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "HRV ${hrv}ms",
                            color = Color(0xFF21005D),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveHeartRateChart(telemetryState: Result<List<BiometricTelemetryEntity>>, bloodReportEntity: com.example.data.database.entity.BloodReportEntity?) {
    val isAbnormal = remember(bloodReportEntity) {
        val cholStr = bloodReportEntity?.cholesterol?.filter { it.isDigit() || it == '.' }
        val chol = cholStr?.toDoubleOrNull()
        (chol != null && chol > 200.0)
    }

    val entries = remember(telemetryState) {
        if (telemetryState is Result.Success) {
            val list = (telemetryState.data as List<BiometricTelemetryEntity>).sortedBy { it.timestamp }.takeLast(20) // Last 20 readings
            if (list.isEmpty()) {
                listOf(entryOf(0f, 60f)) // Default baseline if empty
            } else {
                list.mapIndexed { index, entity ->
                    entryOf(index.toFloat(), entity.heartRate.toFloat())
                }
            }
        } else {
            listOf(entryOf(0f, 60f))
        }
    }
    
    val chartEntryModel = remember(entries) { entryModelOf(entries) }

    ProvideChartStyle(m3ChartStyle()) {
        val lineColor = if (isAbnormal) Color.Red else Color(0xFF21005D)
        
        Chart(
            chart = lineChart(
                lines = listOf(
                    com.patrykandpatrick.vico.compose.chart.line.lineSpec(
                        lineColor = lineColor,
                        lineBackgroundShader = com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient(
                            arrayOf(lineColor.copy(alpha = 0.5f), lineColor.copy(alpha = 0.1f))
                        )
                    )
                )
            ),
            model = chartEntryModel,
            startAxis = rememberStartAxis(
                label = com.patrykandpatrick.vico.compose.component.textComponent(
                    color = lineColor,
                    textSize = 10.sp
                ),
                axis = null,
                tick = null,
                guideline = com.patrykandpatrick.vico.compose.component.lineComponent(
                    color = lineColor.copy(alpha = 0.2f),
                    thickness = 1.dp
                )
            ),
            bottomAxis = rememberBottomAxis(
                label = null,
                axis = null,
                tick = null,
                guideline = null
            ),
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )
    }
}


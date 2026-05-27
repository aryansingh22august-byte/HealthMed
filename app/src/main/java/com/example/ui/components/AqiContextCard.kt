package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.database.entity.BiometricTelemetryEntity
import com.example.data.database.entity.EnvironmentalLogEntity
import com.example.ui.HealthViewModel

@Composable
fun AqiContextCard(
    aqiEntity: EnvironmentalLogEntity?,
    telemetryEntity: BiometricTelemetryEntity?,
    viewModel: HealthViewModel
) {
    var permissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        if (isGranted) {
            viewModel.fetchEnvironmentalContext()
        }
    }

    LaunchedEffect(Unit) {
        // Just directly call for the sample data if it's there
        viewModel.fetchEnvironmentalContext()
    }

    if (aqiEntity == null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.LocationOff, contentDescription = "Location Off")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Enable location to see local Air Quality and correlations with your respiratory data (SpO2).", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) {
                    Text("Grant Location Context")
                }
            }
        }
    } else {
        val aqiScore = aqiEntity.aqiScore
        val color = when {
            aqiScore <= 50 -> Color(0xFF81C784) // Soft Green
            aqiScore <= 100 -> Color(0xFFFFD54F) // Muted Yellow
            aqiScore <= 150 -> Color(0xFFFF8A65) // Light Orange
            else -> Color(0xFFE57373) // Dusty Rose
        }

        val healthAdvisory = if (telemetryEntity != null && telemetryEntity.spO2 < 95.0 && aqiScore > 100) {
            "Your SpO2 is slightly low (${telemetryEntity.spO2}%) and air quality is poor. Consider staying indoors."
        } else if (aqiScore > 100) {
            "Air quality is poor today. Limit intense outdoor exercise."
        } else {
            "Air quality is good. Great day for outdoor activities!"
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Air, contentDescription = "Air Quality", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Current AQI: $aqiScore (${aqiEntity.locationCity})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Dominant Pollutant: ${aqiEntity.dominantPollutant}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = healthAdvisory,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

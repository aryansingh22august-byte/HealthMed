package com.example.data.healthconnect

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class)
    )

    fun checkAvailability(): Int {
        return HealthConnectClient.getSdkStatus(context, "com.google.android.apps.healthdata")
    }

    suspend fun hasAllPermissions(): Boolean {
        if (checkAvailability() != HealthConnectClient.SDK_AVAILABLE) return false
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun requestPermissions(
        activityResultLauncher: ManagedActivityResultLauncher<Set<String>, Set<String>>
    ) {
        val permissionContract = PermissionController.createRequestPermissionResultContract()
        activityResultLauncher.launch(permissions)
    }
}

@Composable
fun HealthConnectPermissionScreen(
    manager: HealthConnectManager,
    onPermissionsGranted: () -> Unit
) {
    var hasPermissions by remember { mutableStateOf(false) }
    var sdkStatus by remember { mutableStateOf(manager.checkAvailability()) }

    LaunchedEffect(Unit) {
        if (sdkStatus == HealthConnectClient.SDK_AVAILABLE) {
            hasPermissions = manager.hasAllPermissions()
            if (hasPermissions) {
                onPermissionsGranted()
            }
        }
    }

    if (sdkStatus == HealthConnectClient.SDK_AVAILABLE) {
        val launcher = rememberLauncherForActivityResult(
            contract = PermissionController.createRequestPermissionResultContract()
        ) { granted ->
            if (granted.containsAll(manager.permissions)) {
                hasPermissions = true
                onPermissionsGranted()
            }
        }

        if (!hasPermissions) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Health Data Integration",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "To provide accurate biometric telemetry, we need read access to your Heart Rate and Blood Oxygen data via Android Health Connect.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { launcher.launch(manager.permissions) }) {
                        Text("Grant Permissions")
                    }
                }
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Health Connect Unavailable",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Please install or update Android Health Connect to use this feature.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

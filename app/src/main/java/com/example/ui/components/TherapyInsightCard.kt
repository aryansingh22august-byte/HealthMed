package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.common.Result
import com.example.data.network.dto.TherapyInsight

@Composable
fun TherapyInsightCard(
    insightState: Result<TherapyInsight?>,
    onFetchInsight: (String) -> Unit
) {
    var customQuestion by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Assistant",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "AI Wellness Drafting",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = customQuestion,
                onValueChange = { customQuestion = it },
                label = { Text("Ask about your data...") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (insightState) {
                is Result.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Analyzing biometric and environmental data...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                is Result.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Could not generate insight. Tap to retry.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { onFetchInsight(customQuestion) }) {
                        Text("Retry")
                    }
                }
                is Result.Success -> {
                    val insight = insightState.data
                    if (insight == null) {
                        Text(
                            text = "Draft a personalized therapy suggestion based on your recent health metrics, or ask a specific question.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onFetchInsight(customQuestion) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(if (customQuestion.isBlank()) "Generate General Insight" else "Ask AI")
                        }
                    } else {
                        val severityColor = when (insight.severity_bound) {
                            "CRITICAL" -> MaterialTheme.colorScheme.errorContainer
                            "ELEVATED" -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val severityTextColor = when (insight.severity_bound) {
                            "CRITICAL" -> MaterialTheme.colorScheme.onErrorContainer
                            "ELEVATED" -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(severityColor, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = insight.insight_summary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = severityTextColor
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = insight.physiological_mechanism,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = severityTextColor.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Action: ${insight.immediate_action_item}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = severityTextColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { onFetchInsight(customQuestion) }) {
                            Text(if (customQuestion.isBlank()) "Refresh Draft" else "Ask Custom Question")
                        }
                    }
                }
            }
        }
    }
}

package com.opendroid.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opendroid.ai.data.models.Plan
import com.opendroid.ai.data.models.PlanStatus
import com.opendroid.ai.data.models.PlanStep
import com.opendroid.ai.data.models.StepStatus
import com.opendroid.ai.ui.theme.*
import com.opendroid.ai.ui.viewmodel.PlanViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    viewModel: PlanViewModel,
    modifier: Modifier = Modifier
) {
    val currentPlan by viewModel.currentPlan.collectAsState()
    val planHistory by viewModel.planHistory.collectAsState()
    
    var selectedPlanId by remember { mutableStateOf<String?>(null) }
    val displayPlan = if (selectedPlanId != null) {
        planHistory.find { it.planId == selectedPlanId } ?: currentPlan
    } else {
        currentPlan
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PLAN ENGINE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = AccentNeonGreen,
                        fontSize = 20.sp,
                        letterSpacing = 2.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Main Section: Current Active Plan
            if (displayPlan != null) {
                item {
                    PlanHeaderCard(
                        plan = displayPlan!!,
                        isCurrentActive = displayPlan!!.planId == currentPlan?.planId,
                        onClearSelection = { selectedPlanId = null }
                    )
                }

                item {
                    Text(
                        text = "PLAN SEQUENCE STAGE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(displayPlan!!.steps) { step ->
                    PlanStepCard(step = step)
                }
            } else {
                item {
                    EmptyPlanPlaceholder()
                }
            }

            // History Section: Past Autonomous Runs
            if (planHistory.isNotEmpty()) {
                item {
                    Text(
                        text = "AUTONOMOUS EXECUTION HISTORY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }

                items(planHistory) { pastPlan ->
                    val isSelected = selectedPlanId == pastPlan.planId || (selectedPlanId == null && pastPlan.planId == currentPlan?.planId)
                    PastPlanRow(
                        plan = pastPlan,
                        isSelected = isSelected,
                        onSelect = { selectedPlanId = pastPlan.planId },
                        onDelete = { viewModel.deletePlan(pastPlan.planId) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlanHeaderCard(
    plan: Plan,
    isCurrentActive: Boolean,
    onClearSelection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isCurrentActive) AccentNeonGreen.copy(alpha = 0.4f) else BorderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (plan.status) {
                        PlanStatus.COMPLETED -> AccentNeonGreen
                        PlanStatus.RUNNING -> AccentCyan
                        PlanStatus.FAILED -> AccentRed
                        else -> TextSecondary
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = plan.status.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (!isCurrentActive) {
                    Text(
                        text = "Viewing Past Run",
                        fontSize = 10.sp,
                        color = AccentPurple,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentPurple.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clickable { onClearSelection() }
                    )
                } else {
                    Text(
                        text = "ACTIVE RUN",
                        fontSize = 10.sp,
                        color = AccentNeonGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentNeonGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = plan.goal,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = BorderColor)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Steps", fontSize = 10.sp, color = TextSecondary)
                    Text("${plan.steps.size} scheduled", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Estimated duration", fontSize = 10.sp, color = TextSecondary)
                    Text(plan.estimatedDuration, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun PlanStepCard(step: PlanStep) {
    var expanded by remember { mutableStateOf(false) }

    val statusBorderColor = when (step.status) {
        StepStatus.COMPLETED -> AccentNeonGreen.copy(alpha = 0.5f)
        StepStatus.RUNNING -> AccentCyan.copy(alpha = 0.5f)
        StepStatus.FAILED -> AccentRed.copy(alpha = 0.5f)
        else -> BorderColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, statusBorderColor, RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Circle badge for step order
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (step.status) {
                                    StepStatus.COMPLETED -> AccentNeonGreen.copy(alpha = 0.2f)
                                    StepStatus.RUNNING -> AccentCyan.copy(alpha = 0.2f)
                                    StepStatus.FAILED -> AccentRed.copy(alpha = 0.2f)
                                    else -> BorderColor
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${step.order}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (step.status) {
                                StepStatus.COMPLETED -> AccentNeonGreen
                                StepStatus.RUNNING -> AccentCyan
                                StepStatus.FAILED -> AccentRed
                                else -> TextSecondary
                            },
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = step.description,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = if (expanded) Int.MAX_VALUE else 1
                    )
                }

                // Step status icon
                Icon(
                    imageVector = when (step.status) {
                        StepStatus.COMPLETED -> Icons.Default.CheckCircle
                        StepStatus.RUNNING -> Icons.Default.Refresh
                        StepStatus.FAILED -> Icons.Default.Close
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = step.status.name,
                    tint = when (step.status) {
                        StepStatus.COMPLETED -> AccentNeonGreen
                        StepStatus.RUNNING -> AccentCyan
                        StepStatus.FAILED -> AccentRed
                        else -> TextSecondary
                    },
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))
                    
                    Text("Action Module: ${step.action}", fontSize = 11.sp, color = AccentPurple, fontFamily = FontFamily.Monospace)
                    
                    if (step.params.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Parameters:", fontSize = 11.sp, color = TextSecondary)
                        step.params.forEach { (key, valStr) ->
                            Text("- $key: $valStr", fontSize = 11.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                        }
                    }

                    if (step.dependsOn.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Depends On Steps: ${step.dependsOn.joinToString()}", fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    }

                    if (step.canParallelize) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Parallel execution supported", fontSize = 11.sp, color = AccentCyan)
                    }

                    if (step.fallback.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Fallback Routine:", fontSize = 11.sp, color = TextSecondary)
                        Text(step.fallback, fontSize = 11.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                    }

                    if (step.result != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBackground)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("Execution Result:", fontSize = 10.sp, color = AccentNeonGreen, fontWeight = FontWeight.Bold)
                                Text(step.result!!, fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                    }

                    if (step.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentRed.copy(alpha = 0.1f))
                                .border(1.dp, AccentRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("Execution Error:", fontSize = 10.sp, color = AccentRed, fontWeight = FontWeight.Bold)
                                Text(step.error!!, fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyPlanPlaceholder() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "No plan",
                tint = TextSecondary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No active plans running",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Plans formulated by the autonomous system will display here in real-time.",
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun PastPlanRow(
    plan: Plan,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) CardBackground else Color.Transparent)
            .border(1.dp, if (isSelected) BorderColor else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { onSelect() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = plan.goal,
                fontSize = 13.sp,
                color = TextPrimary,
                maxLines = 1,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateFormat.format(Date(plan.createdAt)),
                    fontSize = 10.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${plan.steps.size} steps",
                    fontSize = 10.sp,
                    color = AccentCyan,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when (plan.status) {
                    PlanStatus.COMPLETED -> Icons.Default.Check
                    PlanStatus.FAILED -> Icons.Default.Close
                    else -> Icons.Default.Info
                },
                contentDescription = plan.status.name,
                tint = when (plan.status) {
                    PlanStatus.COMPLETED -> AccentNeonGreen
                    PlanStatus.FAILED -> AccentRed
                    else -> TextSecondary
                },
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Plan",
                    tint = TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

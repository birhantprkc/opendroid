package com.opendroid.ai.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.opendroid.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    
    // Core permissions status state
    var recordAudioGranted by remember { mutableStateOf(checkPerm(context, Manifest.permission.RECORD_AUDIO)) }
    var locationGranted by remember { mutableStateOf(checkPerm(context, Manifest.permission.ACCESS_FINE_LOCATION)) }
    var smsGranted by remember { mutableStateOf(checkPerm(context, Manifest.permission.SEND_SMS)) }
    var phoneGranted by remember { mutableStateOf(checkPerm(context, Manifest.permission.CALL_PHONE)) }
    var contactsGranted by remember { mutableStateOf(checkPerm(context, Manifest.permission.READ_CONTACTS)) }
    var calendarGranted by remember { mutableStateOf(checkPerm(context, Manifest.permission.READ_CALENDAR)) }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        recordAudioGranted = it
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        locationGranted = it
    }
    val smsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        smsGranted = it
    }
    val phoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        phoneGranted = it
    }
    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        contactsGranted = it
    }
    val calendarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        calendarGranted = it
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OpenDroid Onboarding", color = AccentNeonGreen, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Text(
                text = "Welcome to OpenDroid",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "We need permissions to act as your autonomous device operator. Grant the following items:",
                fontSize = 14.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    PermissionCard(
                        title = "Microphone",
                        desc = "Needed for wake word and speech recognition.",
                        granted = recordAudioGranted,
                        onGrant = { audioLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                    )
                }
                item {
                    PermissionCard(
                        title = "Location",
                        desc = "Needed to fetch weather, directions, and maps.",
                        granted = locationGranted,
                        onGrant = { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                    )
                }
                item {
                    PermissionCard(
                        title = "SMS & Telephony",
                        desc = "Needed to read and send messages, and place calls.",
                        granted = smsGranted && phoneGranted,
                        onGrant = {
                            smsLauncher.launch(Manifest.permission.SEND_SMS)
                            phoneLauncher.launch(Manifest.permission.CALL_PHONE)
                        }
                    )
                }
                item {
                    PermissionCard(
                        title = "Contacts & Calendar",
                        desc = "Needed to resolve recipient names and manage events.",
                        granted = contactsGranted && calendarGranted,
                        onGrant = {
                            contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                            calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                        }
                    )
                }
                item {
                    PermissionCard(
                        title = "Accessibility Service",
                        desc = "Enables full agent screen automation (clicks & inputs).",
                        granted = false, // always let them click to go to settings
                        onGrant = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onFinished,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentNeonGreen, contentColor = DarkBackground),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Proceed to OpenDroid Agent", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    desc: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(desc, fontSize = 12.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (granted) BorderColor else AccentNeonGreen,
                contentColor = if (granted) TextSecondary else DarkBackground
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(if (granted) "Granted" else "Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun checkPerm(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

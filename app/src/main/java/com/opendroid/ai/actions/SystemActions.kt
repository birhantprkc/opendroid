package com.opendroid.ai.actions

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.provider.Settings
import com.opendroid.ai.accessibility.OpenDroidAccessibilityService
import com.opendroid.ai.actions.base.Action
import com.opendroid.ai.actions.base.ActionResult
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemActions @Inject constructor() {

    fun getActions(): List<Action> = listOf(
        ToggleWifiAction(),
        ToggleFlashlightAction(),
        SetVolumeAction(),
        SetBrightnessAction(),
        OpenAppAction(),
        LockScreenAction(),
        RestartDeviceAction(),
        ToggleBluetoothAction(),
        ToggleDndAction(),
        TakeScreenshotAction()
    )

    private class ToggleWifiAction : Action {
        override val name: String = "TOGGLE_WIFI"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val on = params["on"]?.toBoolean() ?: true
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            return try {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = on
                ActionResult(true, "WiFi set to $on", null)
            } catch (e: Exception) {
                // Fallback: Launch wifi settings panel so user can toggle manually
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(false, "Failed to toggle WiFi directly. Opened settings panel.", e.localizedMessage, true)
            }
        }
    }

    private class ToggleFlashlightAction : Action {
        override val name: String = "TOGGLE_FLASHLIGHT"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val on = params["on"]?.toBoolean() ?: true
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            return try {
                val cameraId = cameraManager.cameraIdList[0]
                cameraManager.setTorchMode(cameraId, on)
                ActionResult(true, "Flashlight set to $on", null)
            } catch (e: Exception) {
                ActionResult(false, null, "Failed to toggle flashlight: ${e.localizedMessage}")
            }
        }
    }

    private class SetVolumeAction : Action {
        override val name: String = "SET_VOLUME"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val typeStr = params["type"] ?: "media"
            val level = params["level"]?.toIntOrNull() ?: 50
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val streamType = when (typeStr.lowercase()) {
                "ring" -> AudioManager.STREAM_RING
                "alarm" -> AudioManager.STREAM_ALARM
                else -> AudioManager.STREAM_MUSIC
            }
            return try {
                val maxVolume = audioManager.getStreamMaxVolume(streamType)
                val targetVolume = (level * maxVolume) / 100
                audioManager.setStreamVolume(streamType, targetVolume, AudioManager.FLAG_SHOW_UI)
                ActionResult(true, "Volume for $typeStr set to $level%", null)
            } catch (e: Exception) {
                ActionResult(false, null, "Volume adjustment failed: ${e.localizedMessage}")
            }
        }
    }

    private class SetBrightnessAction : Action {
        override val name: String = "SET_BRIGHTNESS"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val level = params["level"]?.toIntOrNull() ?: 50
            val targetVal = (level * 255) / 100
            return try {
                if (Settings.System.canWrite(context)) {
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, targetVal)
                    ActionResult(true, "Brightness set to $level%", null)
                } else {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult(false, "Write settings permission not granted. Prompted user.", "Permission required", true)
                }
            } catch (e: Exception) {
                ActionResult(false, null, "Brightness adjustment failed: ${e.localizedMessage}")
            }
        }
    }

    private class OpenAppAction : Action {
        override val name: String = "OPEN_APP"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val appName = params["appName"] ?: return ActionResult(false, null, "appName parameter missing")
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(0)
            val appPackage = packages.find {
                pm.getApplicationLabel(it).toString().contains(appName, ignoreCase = true)
            }?.packageName
            return if (appPackage != null) {
                val intent = pm.getLaunchIntentForPackage(appPackage)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    ActionResult(true, "Opened $appName ($appPackage)", null)
                } else {
                    ActionResult(false, null, "Launcher intent not found for $appPackage")
                }
            } else {
                ActionResult(false, null, "App '$appName' not installed.")
            }
        }
    }

    private class LockScreenAction : Action {
        override val name: String = "LOCK_SCREEN"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val service = OpenDroidAccessibilityService.getInstance()
            return if (service != null) {
                val success = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                ActionResult(success, if (success) "Device locked" else "Failed to lock", null)
            } else {
                ActionResult(false, null, "Accessibility Service is not running or active.")
            }
        }
    }

    private class RestartDeviceAction : Action {
        override val name: String = "RESTART_DEVICE"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val service = OpenDroidAccessibilityService.getInstance()
            return if (service != null) {
                val success = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
                ActionResult(success, if (success) "Power dialog opened. Action pending user touch." else "Failed to open dialog", null)
            } else {
                ActionResult(false, null, "Accessibility Service not running to trigger power dialog.")
            }
        }
    }

    private class ToggleBluetoothAction : Action {
        override val name: String = "TOGGLE_BLUETOOTH"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val on = params["on"]?.toBoolean() ?: true
            return try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                @Suppress("DEPRECATION")
                if (on) adapter.enable() else adapter.disable()
                ActionResult(true, "Bluetooth set to $on", null)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(false, "Failed to toggle bluetooth directly. Opened settings panel.", e.localizedMessage, true)
            }
        }
    }

    private class ToggleDndAction : Action {
        override val name: String = "TOGGLE_DND"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val on = params["on"]?.toBoolean() ?: true
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return try {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    val filter = if (on) NotificationManager.INTERRUPTION_FILTER_NONE else NotificationManager.INTERRUPTION_FILTER_ALL
                    notificationManager.setInterruptionFilter(filter)
                    ActionResult(true, "DND set to $on", null)
                } else {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult(false, "DND policy permission not granted. Prompted user.", "Permission required", true)
                }
            } catch (e: Exception) {
                ActionResult(false, null, "Failed to toggle DND: ${e.localizedMessage}")
            }
        }
    }

    private class TakeScreenshotAction : Action {
        override val name: String = "TAKE_SCREENSHOT"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val service = OpenDroidAccessibilityService.getInstance()
            return if (service != null) {
                val success = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                ActionResult(success, if (success) "Screenshot captured successfully." else "Failed to capture screenshot", null)
            } else {
                ActionResult(false, null, "Accessibility Service is not running to trigger screenshot.")
            }
        }
    }
}

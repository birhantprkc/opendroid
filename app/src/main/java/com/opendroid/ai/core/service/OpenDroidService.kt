package com.opendroid.ai.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.opendroid.ai.core.agent.AgentLoop
import com.opendroid.ai.core.voice.SpeechRecognitionEngine
import com.opendroid.ai.core.voice.TextToSpeechEngine
import com.opendroid.ai.core.voice.WakeWordDetector
import com.opendroid.ai.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OpenDroidService : Service() {

    @Inject
    lateinit var agentLoop: AgentLoop

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private lateinit var wakeWordDetector: WakeWordDetector
    private lateinit var speechRecognitionEngine: SpeechRecognitionEngine
    private lateinit var textToSpeechEngine: TextToSpeechEngine

    companion object {
        private const val CHANNEL_ID = "opendroid_channel"
        private const val NOTIFICATION_ID = 2024
        
        fun start(context: Context) {
            val intent = Intent(context, OpenDroidService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, OpenDroidService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize engines
        wakeWordDetector = WakeWordDetector(this)
        speechRecognitionEngine = SpeechRecognitionEngine(this)
        textToSpeechEngine = TextToSpeechEngine(this, settingsRepository)

        // Bind Agent Loop TTS
        agentLoop.onSpeakCallback = { text ->
            textToSpeechEngine.speak(text)
        }

        // Start Foreground Notification
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // Start Wake Word Detection
        startWakeWordDetection()
    }

    private fun startWakeWordDetection() {
        wakeWordDetector.startListening {
            // Wake word detected! Prompt user with a sound or greeting and start listing for query
            textToSpeechEngine.speak("OpenDroid online.")
            startListeningForQuery()
        }
    }

    private fun startListeningForQuery() {
        // Temporarily pause wake word to avoid hearing itself
        wakeWordDetector.stopListening()

        // Start speech recognizer for query input
        speechRecognitionEngine.startListening(
            onResult = { query ->
                agentLoop.processQuery(query, this)
                // Resume wake word detection
                wakeWordDetector.startListening {
                    textToSpeechEngine.speak("OpenDroid online.")
                    startListeningForQuery()
                }
            },
            onError = { error ->
                // Resume wake word detection
                wakeWordDetector.startListening {
                    textToSpeechEngine.speak("OpenDroid online.")
                    startListeningForQuery()
                }
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeWordDetector.destroy()
        speechRecognitionEngine.destroy()
        textToSpeechEngine.destroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "OpenDroid Agent Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps OpenDroid background agent alive"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OpenDroid Active")
            .setContentText("Listening for wake word 'OpenDroid'")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }
}

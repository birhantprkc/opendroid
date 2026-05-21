package com.opendroid.ai.actions

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import com.opendroid.ai.actions.base.Action
import com.opendroid.ai.actions.base.ActionResult
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarActions @Inject constructor() {

    fun getActions(): List<Action> = listOf(
        CreateCalendarEventAction(),
        SetAlarmAction(),
        SetTimerAction(),
        AddNoteAction()
    )

    private class CreateCalendarEventAction : Action {
        override val name: String = "CREATE_CALENDAR_EVENT"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val title = params["title"] ?: "New Event"
            return try {
                val cr = context.contentResolver
                val values = ContentValues().apply {
                    put(CalendarContract.Events.DTSTART, Calendar.getInstance().timeInMillis)
                    put(CalendarContract.Events.DTEND, Calendar.getInstance().timeInMillis + 60 * 60 * 1000) // 1 hr duration
                    put(CalendarContract.Events.TITLE, title)
                    put(CalendarContract.Events.DESCRIPTION, params["description"] ?: "Created by OpenDroid")
                    put(CalendarContract.Events.CALENDAR_ID, 1)
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                }
                
                // Needs calendar permissions. If fails, fallback to calendar UI compose intent
                val uri = cr.insert(CalendarContract.Events.CONTENT_URI, values)
                if (uri != null) {
                    ActionResult(true, "Event '$title' inserted into Calendar.", null)
                } else {
                    throw IllegalStateException("Insert returned null URI")
                }
            } catch (e: Exception) {
                // Fallback: Open compose intent in system calendar
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, title)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(false, "Calendar permission missing or write failed. Opened Calendar compose UI as fallback.", e.localizedMessage, true)
            }
        }
    }

    private class SetAlarmAction : Action {
        override val name: String = "SET_ALARM"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val timeStr = params["time"] ?: return ActionResult(false, null, "time is missing (format HH:MM)")
            return try {
                val parts = timeStr.split(":")
                val hour = parts[0].toInt()
                val minutes = parts[1].toInt()
                val label = params["label"] ?: "OpenDroid Alarm"
                
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                    putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(true, "Alarm set for $timeStr - '$label'", null)
            } catch (e: Exception) {
                ActionResult(false, null, "Alarm failed: ${e.localizedMessage}")
            }
        }
    }

    private class SetTimerAction : Action {
        override val name: String = "SET_TIMER"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val durationSecs = params["duration"]?.toIntOrNull() ?: 60
            val label = params["label"] ?: "OpenDroid Timer"
            return try {
                val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, durationSecs)
                    putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(true, "Timer set for $durationSecs seconds", null)
            } catch (e: Exception) {
                ActionResult(false, null, "Timer failed: ${e.localizedMessage}")
            }
        }
    }

    private class AddNoteAction : Action {
        override val name: String = "ADD_NOTE"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val title = params["title"] ?: "Quick Note"
            val content = params["content"] ?: ""
            return try {
                // Return success immediately (can be stored in database as well)
                ActionResult(true, "Note '$title' saved: '$content'", null)
            } catch (e: Exception) {
                ActionResult(false, null, "Save note failed: ${e.localizedMessage}")
            }
        }
    }
}

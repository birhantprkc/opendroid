package com.opendroid.ai.actions

import android.content.Context
import com.opendroid.ai.actions.base.Action
import com.opendroid.ai.actions.base.ActionResult
import com.opendroid.ai.data.db.dao.MacroDao
import com.opendroid.ai.data.db.entities.MacroEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MacroActions @Inject constructor(
    private val macroDao: MacroDao
) {

    fun getActions(): List<Action> = listOf(
        RunMacroAction(macroDao),
        CreateMacroAction(macroDao),
        ScheduleMacroAction(macroDao)
    )

    private class RunMacroAction(private val macroDao: MacroDao) : Action {
        override val name: String = "RUN_MACRO"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val macroName = params["macroName"] ?: return ActionResult(false, null, "macroName parameter missing")
            return try {
                val macro = macroDao.getMacroByName(macroName)
                if (macro != null) {
                    ActionResult(true, macro.stepsJson, null)
                } else {
                    ActionResult(false, null, "Macro with name '$macroName' not found.")
                }
            } catch (e: Exception) {
                ActionResult(false, null, "Failed to run macro: ${e.localizedMessage}")
            }
        }
    }

    private class CreateMacroAction(private val macroDao: MacroDao) : Action {
        override val name: String = "CREATE_MACRO"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val name = params["name"] ?: return ActionResult(false, null, "name parameter missing")
            val steps = params["steps"] ?: return ActionResult(false, null, "steps parameter missing")
            return try {
                val entity = MacroEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    trigger = "manual",
                    stepsJson = steps,
                    isSystem = false,
                    isEnabled = true
                )
                macroDao.insertMacro(entity)
                ActionResult(true, "Macro '$name' created successfully with steps: $steps", null)
            } catch (e: Exception) {
                ActionResult(false, null, "Failed to create macro: ${e.localizedMessage}")
            }
        }
    }

    private class ScheduleMacroAction(private val macroDao: MacroDao) : Action {
        override val name: String = "SCHEDULE_MACRO"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val macroName = params["macroName"] ?: return ActionResult(false, null, "macroName parameter missing")
            val cronExpression = params["cronExpression"] ?: return ActionResult(false, null, "cronExpression parameter missing")
            return try {
                val macro = macroDao.getMacroByName(macroName)
                if (macro != null) {
                    val updated = macro.copy(trigger = "cron:$cronExpression")
                    macroDao.insertMacro(updated)
                    ActionResult(true, "Scheduled macro '$macroName' with cron: '$cronExpression'", null)
                } else {
                    // Try creating a new empty macro with schedule
                    val entity = MacroEntity(
                        id = UUID.randomUUID().toString(),
                        name = macroName,
                        trigger = "cron:$cronExpression",
                        stepsJson = "[]",
                        isSystem = false,
                        isEnabled = true
                    )
                    macroDao.insertMacro(entity)
                    ActionResult(true, "Created scheduled empty macro '$macroName' with cron: '$cronExpression'", null, true)
                }
            } catch (e: Exception) {
                ActionResult(false, null, "Failed to schedule macro: ${e.localizedMessage}")
            }
        }
    }
}

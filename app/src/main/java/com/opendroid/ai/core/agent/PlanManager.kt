package com.opendroid.ai.core.agent

import com.opendroid.ai.data.models.Plan
import com.opendroid.ai.data.models.PlanStatus
import com.opendroid.ai.data.models.PlanStep
import com.opendroid.ai.data.models.StepStatus
import com.opendroid.ai.data.repository.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanManager @Inject constructor(
    private val planRepository: PlanRepository
) {
    private val _currentPlan = MutableStateFlow<Plan?>(null)
    val currentPlan: StateFlow<Plan?> = _currentPlan.asStateFlow()

    suspend fun startNewPlan(plan: Plan) {
        _currentPlan.value = plan.copy(status = PlanStatus.RUNNING)
        saveCurrentPlan()
    }

    suspend fun updateStepStatus(stepId: String, status: StepStatus, result: String? = null, error: String? = null) {
        val plan = _currentPlan.value ?: return
        val updatedSteps = plan.steps.map { step ->
            if (step.stepId == stepId) {
                step.copy(status = status, result = result, error = error)
            } else {
                step
            }
        }
        _currentPlan.value = plan.copy(steps = updatedSteps)
        saveCurrentPlan()
    }

    suspend fun updatePlanStatus(status: PlanStatus) {
        val plan = _currentPlan.value ?: return
        _currentPlan.value = plan.copy(status = status)
        saveCurrentPlan()
    }

    suspend fun loadPlan(planId: String): Boolean {
        val plan = planRepository.getPlanById(planId)
        return if (plan != null) {
            _currentPlan.value = plan
            true
        } else {
            false
        }
    }

    suspend fun saveCurrentPlan() {
        val plan = _currentPlan.value ?: return
        planRepository.savePlan(plan)
    }

    fun clearPlan() {
        _currentPlan.value = null
    }

    fun getActiveStep(): PlanStep? {
        val plan = _currentPlan.value ?: return null
        if (plan.status != PlanStatus.RUNNING) return null
        // Return the first pending step that doesn't have pending dependencies
        return plan.steps.firstOrNull { step ->
            step.status == StepStatus.PENDING && hasDependenciesMet(step, plan.steps)
        }
    }

    private fun hasDependenciesMet(step: PlanStep, allSteps: List<PlanStep>): Boolean {
        if (step.dependsOn.isEmpty()) return true
        return step.dependsOn.all { depId ->
            allSteps.find { it.stepId == depId }?.status == StepStatus.COMPLETED
        }
    }
}

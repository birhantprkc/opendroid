package com.opendroid.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opendroid.ai.core.memory.MemoryManager
import com.opendroid.ai.data.models.Memory
import com.opendroid.ai.data.models.MemoryType
import com.opendroid.ai.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memoryManager: MemoryManager,
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    val memoriesList: StateFlow<List<Memory>> = memoryRepository.allMemories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun storeFact(key: String, value: String, type: MemoryType = MemoryType.SEMANTIC) {
        viewModelScope.launch {
            memoryRepository.saveMemory(
                Memory(
                    key = key,
                    value = value,
                    type = type,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteMemory(key: String) {
        viewModelScope.launch {
            memoryRepository.deleteMemory(key)
        }
    }

    fun clearMemories(type: MemoryType) {
        viewModelScope.launch {
            memoryManager.clearMemory(type)
        }
    }
}

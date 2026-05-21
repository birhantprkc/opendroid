package com.opendroid.ai.core.memory

import com.opendroid.ai.data.models.Memory
import com.opendroid.ai.data.models.MemoryType
import com.opendroid.ai.data.repository.MemoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SemanticMemory @Inject constructor(
    private val memoryRepository: MemoryRepository
) {
    suspend fun storeFact(key: String, value: String) {
        memoryRepository.saveMemory(
            Memory(key = key, value = value, type = MemoryType.SEMANTIC)
        )
    }

    suspend fun getFact(key: String): String? {
        return memoryRepository.getMemoriesByType(MemoryType.SEMANTIC)
            .find { it.key.equals(key, ignoreCase = true) }?.value
    }

    suspend fun getAllFacts(): List<Memory> {
        return memoryRepository.getMemoriesByType(MemoryType.SEMANTIC)
    }
}

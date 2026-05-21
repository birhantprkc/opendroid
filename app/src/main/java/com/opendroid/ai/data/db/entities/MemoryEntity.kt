package com.opendroid.ai.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val key: String,
    val value: String,
    val type: String, // WORKING, EPISODIC, SEMANTIC, PROCEDURAL
    val timestamp: Long
)

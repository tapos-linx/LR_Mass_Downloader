package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.RecordType
import com.example.data.model.TaskStatus

class Converters {
    @TypeConverter
    fun fromRecordTypeSet(value: Set<RecordType>): String {
        return value.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toRecordTypeSet(value: String): Set<RecordType> {
        if (value.isEmpty()) return emptySet()
        return value.split(",").map { RecordType.valueOf(it) }.toSet()
    }

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus): String {
        return value.name
    }

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus {
        return TaskStatus.valueOf(value)
    }
}

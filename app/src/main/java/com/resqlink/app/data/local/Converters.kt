package com.resqlink.app.data.local

import androidx.room.TypeConverter

/** Room type converters for storing simple Kotlin lists as delimited strings. */
class Converters {
    @TypeConverter
    fun fromStepList(steps: List<String>): String = steps.joinToString("|||")

    @TypeConverter
    fun toStepList(raw: String): List<String> =
        if (raw.isBlank()) emptyList() else raw.split("|||")
}

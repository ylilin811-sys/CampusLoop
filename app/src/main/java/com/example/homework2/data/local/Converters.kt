package com.example.homework2.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromListToString(list: List<String>?): String {
        if (list.isNullOrEmpty()) return ""
        return list.joinToString(separator = "|||")
    }

    @TypeConverter
    fun fromStringToList(data: String?): List<String> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split("|||").filter { it.isNotBlank() }
    }
}

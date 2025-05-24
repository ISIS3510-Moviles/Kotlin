package com.example.campusbites.data.local

import androidx.room.TypeConverter

class ListStringConverter {
    @TypeConverter
    fun fromListString(list: List<String>?): String? {
        return list?.joinToString(separator = ",")
    }

    @TypeConverter
    fun toListString(data: String?): List<String>? {
        return data?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
    }
}
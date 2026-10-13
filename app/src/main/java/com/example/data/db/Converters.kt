package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.MediaType

class Converters {
    @TypeConverter
    fun fromMediaType(type: MediaType): String = type.name

    @TypeConverter
    fun toMediaType(value: String): MediaType = runCatching {
        MediaType.valueOf(value)
    }.getOrDefault(MediaType.PHOTO)
}

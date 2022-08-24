package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.core_db.model.CurrencyLocal

class CurrencyConverters {

    @TypeConverter
    fun fromCategory(type: CurrencyLocal.Category): String {
        return type.name
    }

    @TypeConverter
    fun toCategory(name: String): CurrencyLocal.Category {
        return enumValueOf(name)
    }
}

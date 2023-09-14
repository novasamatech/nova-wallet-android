package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.core_db.model.ExternalBalanceLocal

class ExternalBalanceTypeConverters {

    @TypeConverter
    fun fromType(type: ExternalBalanceLocal.Type): String {
        return type.name
    }

    @TypeConverter
    fun toType(name: String): ExternalBalanceLocal.Type {
        return ExternalBalanceLocal.Type.valueOf(name)
    }
}

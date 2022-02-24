package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.core_db.model.NftLocal

class NftTypeConverters {

    @TypeConverter
    fun from(type: NftLocal.Type): String {
        return type.name
    }

    @TypeConverter
    fun to(name: String): NftLocal.Type {
        return enumValueOf(name)
    }
}

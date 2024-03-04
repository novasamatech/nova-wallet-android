package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.core_db.model.NftLocal

class NftConverters {

    @TypeConverter
    fun fromNftType(type: NftLocal.Type): String {
        return type.name
    }

    @TypeConverter
    fun toNftType(name: String): NftLocal.Type {
        return enumValueOf(name)
    }

    @TypeConverter
    fun fromNftIssuanceType(type: NftLocal.IssuanceType): String {
        return type.name
    }

    @TypeConverter
    fun toNftIssuanceType(name: String): NftLocal.IssuanceType {
        return enumValueOf(name)
    }
}

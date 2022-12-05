package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal

class AssetSourceConverter {

    @TypeConverter
    fun fromCategory(type: AssetSourceLocal): String {
        return type.name
    }

    @TypeConverter
    fun toCategory(name: String): AssetSourceLocal {
        return enumValueOf(name)
    }
}

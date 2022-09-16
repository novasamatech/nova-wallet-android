package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.core_db.model.ContributionLocal

class ContributionTypeConverter {

    @TypeConverter
    fun fromCategory(type: ContributionLocal.Type): String {
        return type.name
    }

    @TypeConverter
    fun toCategory(name: String): ContributionLocal.Type {
        return enumValueOf(name)
    }
}

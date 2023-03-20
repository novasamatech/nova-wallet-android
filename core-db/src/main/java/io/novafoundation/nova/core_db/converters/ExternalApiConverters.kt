package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.common.utils.enumValueOfOrNull
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal.SourceType

class ExternalApiConverters {

    @TypeConverter
    fun fromApiType(apiType: SourceType): String {
        return apiType.name
    }

    @TypeConverter
    fun toApiType(raw: String): SourceType {
        return enumValueOfOrNull<SourceType>(raw) ?: SourceType.UNKNOWN
    }
}

package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal

class ExternalApiConverters {

    @TypeConverter
    fun fromApiType(apiType: ChainExternalApiLocal.SourceType): String {
        return apiType.name
    }

    @TypeConverter
    fun toApiType(raw: String): ChainExternalApiLocal.SourceType {
        return enumValueOf(raw)
    }
}

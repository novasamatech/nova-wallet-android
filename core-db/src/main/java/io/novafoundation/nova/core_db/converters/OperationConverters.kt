package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.core_db.model.operation.ExtrinsicTypeLocal
import io.novafoundation.nova.core_db.model.operation.OperationBaseLocal

class OperationConverters {

    @TypeConverter
    fun fromOperationSource(source: OperationBaseLocal.Source) = source.ordinal

    @TypeConverter
    fun toOperationSource(ordinal: Int) = OperationBaseLocal.Source.values()[ordinal]

    @TypeConverter
    fun fromOperationStatus(status: OperationBaseLocal.Status) = status.ordinal

    @TypeConverter
    fun toOperationStatus(ordinal: Int) = OperationBaseLocal.Status.values()[ordinal]

    @TypeConverter
    fun fromExtrinsicContentType(type: ExtrinsicTypeLocal.ContentType) = type.name

    @TypeConverter
    fun toExtrinsicContentType(name: String): ExtrinsicTypeLocal.ContentType = enumValueOf(name)
}

package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.core_db.model.OperationLocal

class OperationConverters {
    @TypeConverter
    fun fromOperationSource(source: OperationLocal.Source) = source.ordinal

    @TypeConverter
    fun toOperationSource(ordinal: Int) = OperationLocal.Source.values()[ordinal]

    @TypeConverter
    fun fromOperationStatus(status: OperationLocal.Status) = status.ordinal

    @TypeConverter
    fun toOperationStatus(ordinal: Int) = OperationLocal.Status.values()[ordinal]

    @TypeConverter
    fun fromOperationType(type: OperationLocal.Type) = type.ordinal

    @TypeConverter
    fun toOperationType(ordinal: Int) = OperationLocal.Type.values()[ordinal]

    @TypeConverter
    fun fromExtrinsicContentType(type: OperationLocal.ExtrinsicContentType) = type.name

    @TypeConverter
    fun toExtrinsicContentType(name: String): OperationLocal.ExtrinsicContentType = enumValueOf(name)
}

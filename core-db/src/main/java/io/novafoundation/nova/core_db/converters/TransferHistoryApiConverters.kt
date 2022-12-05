package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.core_db.model.chain.ChainTransferHistoryApiLocal

class TransferHistoryApiConverters {

    @TypeConverter
    fun fromApiType(apiType: ChainTransferHistoryApiLocal.ApiType): String {
        return apiType.name
    }

    @TypeConverter
    fun toApiType(raw: String): ChainTransferHistoryApiLocal.ApiType {
        return enumValueOf(raw)
    }

    @TypeConverter
    fun fromAssetType(assetType: ChainTransferHistoryApiLocal.AssetType): String {
        return assetType.name
    }

    @TypeConverter
    fun toAssetType(raw: String): ChainTransferHistoryApiLocal.AssetType {
        return enumValueOf(raw)
    }
}

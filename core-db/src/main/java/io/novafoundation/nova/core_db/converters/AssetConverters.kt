package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.core_db.model.AssetLocal.EDCountingModeLocal
import io.novafoundation.nova.core_db.model.AssetLocal.TransferableModeLocal
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal

class AssetConverters {

    @TypeConverter
    fun fromCategory(type: AssetSourceLocal): String {
        return type.name
    }

    @TypeConverter
    fun toCategory(name: String): AssetSourceLocal {
        return enumValueOf(name)
    }

    @TypeConverter
    fun fromTransferableMode(mode: TransferableModeLocal): Int {
        return mode.ordinal
    }

    @TypeConverter
    fun toTransferableMode(index: Int): TransferableModeLocal {
        return TransferableModeLocal.values()[index]
    }

    @TypeConverter
    fun fromEdCountingMode(mode: EDCountingModeLocal): Int {
        return mode.ordinal
    }

    @TypeConverter
    fun toEdCountingMode(index: Int): EDCountingModeLocal {
        return EDCountingModeLocal.values()[index]
    }
}

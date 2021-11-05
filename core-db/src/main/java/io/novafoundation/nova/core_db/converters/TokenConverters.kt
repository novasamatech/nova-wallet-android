package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.core_db.model.TokenLocal

class TokenConverters {

    @TypeConverter
    fun fromToken(type: TokenLocal.Type): Int {
        return type.ordinal
    }

    @TypeConverter
    fun toToken(ordinal: Int): TokenLocal.Type {
        return TokenLocal.Type.values()[ordinal]
    }
}

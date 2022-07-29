package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.core.model.CryptoType

class CryptoTypeConverters {

    @TypeConverter
    fun from(cryptoType: CryptoType?): String? = cryptoType?.name

    @TypeConverter
    fun to(name: String?): CryptoType? = name?.let { enumValueOf<CryptoType>(it) }
}

package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal

class MetaAccountTypeConverters {

    @TypeConverter
    fun fromEnum(type: MetaAccountLocal.Type): String {
        return type.name
    }

    @TypeConverter
    fun toEnum(name: String): MetaAccountLocal.Type {
        return MetaAccountLocal.Type.valueOf(name)
    }
}

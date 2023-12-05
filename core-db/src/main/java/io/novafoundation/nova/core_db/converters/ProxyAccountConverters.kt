package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.ProxyAccountLocal

class ProxyAccountConverters {
    @TypeConverter
    fun fromStatusType(type: MetaAccountLocal.Status): String {
        return type.name
    }

    @TypeConverter
    fun toStatusType(name: String): MetaAccountLocal.Status {
        return MetaAccountLocal.Status.valueOf(name)
    }
}

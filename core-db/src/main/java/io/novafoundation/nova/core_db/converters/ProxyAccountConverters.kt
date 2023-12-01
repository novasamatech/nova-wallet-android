package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.core_db.model.chain.account.ProxyAccountLocal

class ProxyAccountConverters {
    @TypeConverter
    fun fromStatusType(type: ProxyAccountLocal.Status): String {
        return type.name
    }

    @TypeConverter
    fun toStatusType(name: String): ProxyAccountLocal.Status {
        return ProxyAccountLocal.Status.valueOf(name)
    }
}

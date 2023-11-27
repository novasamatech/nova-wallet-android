package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.core_db.model.chain.account.ProxyAccountLocal

class ProxyAccountConverters {
    @TypeConverter
    fun fromRightType(type: ProxyAccountLocal.RightType): String {
        return type.name
    }

    @TypeConverter
    fun toRightType(name: String): ProxyAccountLocal.RightType {
        return ProxyAccountLocal.RightType.valueOf(name)
    }

    @TypeConverter
    fun fromStatusType(type: ProxyAccountLocal.Status): String {
        return type.name
    }

    @TypeConverter
    fun toStatusType(name: String): ProxyAccountLocal.Status {
        return ProxyAccountLocal.Status.valueOf(name)
    }
}

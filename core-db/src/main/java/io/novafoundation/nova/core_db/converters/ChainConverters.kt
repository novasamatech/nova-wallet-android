package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.common.utils.enumValueOfOrNull
import io.novafoundation.nova.core_db.model.chain.ChainLocal.ConnectionStateLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal.AutoBalanceStrategyLocal

class ChainConverters {

    @TypeConverter
    fun fromNodeStrategy(strategy: AutoBalanceStrategyLocal): String = strategy.name

    @TypeConverter
    fun toNodeStrategy(name: String): AutoBalanceStrategyLocal {
        return enumValueOfOrNull<AutoBalanceStrategyLocal>(name) ?: AutoBalanceStrategyLocal.UNKNOWN
    }

    @TypeConverter
    fun fromConnection(connectionState: ConnectionStateLocal): String = connectionState.name

    @TypeConverter
    fun toConnection(name: String): ConnectionStateLocal {
        return enumValueOfOrNull<ConnectionStateLocal>(name) ?: ConnectionStateLocal.LIGHT_SYNC
    }
}

package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.common.utils.enumValueOfOrNull
import io.novafoundation.nova.core_db.model.chain.ChainLocal.ConnectionStateLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal.NodeSelectionStrategyLocal

class ChainConverters {

    @TypeConverter
    fun fromNodeStrategy(strategy: NodeSelectionStrategyLocal): String = strategy.name

    @TypeConverter
    fun toNodeStrategy(name: String): NodeSelectionStrategyLocal {
        return enumValueOfOrNull<NodeSelectionStrategyLocal>(name) ?: NodeSelectionStrategyLocal.UNKNOWN
    }

    @TypeConverter
    fun fromConnection(connectionState: ConnectionStateLocal): String = connectionState.name

    @TypeConverter
    fun toConnection(name: String): ConnectionStateLocal {
        return enumValueOfOrNull<ConnectionStateLocal>(name) ?: ConnectionStateLocal.LIGHT_SYNC
    }
}

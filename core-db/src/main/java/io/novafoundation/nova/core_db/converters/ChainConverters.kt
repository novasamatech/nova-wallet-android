package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import io.novafoundation.nova.common.utils.enumValueOfOrNull
import io.novafoundation.nova.core_db.model.chain.ChainLocal.NodeSelectionStrategyLocal


class ChainConverters {

    @TypeConverter
    fun from(strategy: NodeSelectionStrategyLocal): String = strategy.name

    @TypeConverter
    fun to(name: String): NodeSelectionStrategyLocal {
        return enumValueOfOrNull<NodeSelectionStrategyLocal>(name) ?: NodeSelectionStrategyLocal.UNKNOWN
    }
}

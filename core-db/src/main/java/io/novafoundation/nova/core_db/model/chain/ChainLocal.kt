package io.novafoundation.nova.core_db.model.chain

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.core_db.model.chain.ChainLocal.Default.NODE_SELECTION_STRATEGY_DEFAULT

const val PUSH_DEFAULT_VALUE = "0"

@Entity(tableName = "chains")
data class ChainLocal(
    @PrimaryKey val id: String,
    val parentId: String?,
    val name: String,
    val icon: String,
    @Embedded
    val types: TypesConfig?,
    val prefix: Int,
    val isEthereumBased: Boolean,
    val isTestNet: Boolean,
    @ColumnInfo(defaultValue = "1")
    val hasSubstrateRuntime: Boolean,
    @ColumnInfo(defaultValue = PUSH_DEFAULT_VALUE)
    val pushSupport: Boolean,
    val hasCrowdloans: Boolean,
    @ColumnInfo(defaultValue = "0")
    val supportProxy: Boolean,
    val swap: String,
    val customFee: String,
    val governance: String,
    val additional: String?,
    val connectionState: ConnectionStateLocal,
    @ColumnInfo(defaultValue = NODE_SELECTION_STRATEGY_DEFAULT)
    val nodeSelectionStrategy: NodeSelectionStrategyLocal,
    @ColumnInfo(defaultValue = DEFAULT_NETWORK_SOURCE_STR)
    val source: Source
) : Identifiable {

    companion object {

        const val EMPTY_CHAIN_ICON = ""

        const val DEFAULT_NETWORK_SOURCE_STR = "DEFAULT"
    }

    enum class NodeSelectionStrategyLocal {
        ROUND_ROBIN, UNIFORM, UNKNOWN
    }

    enum class ConnectionStateLocal {
        FULL_SYNC, LIGHT_SYNC, DISABLED
    }

    enum class Source {
        DEFAULT, CUSTOM
    }

    object Default {

        const val NODE_SELECTION_STRATEGY_DEFAULT = "ROUND_ROBIN"

        const val HAS_SUBSTRATE_RUNTIME = 1

        const val CONNECTION_STATE_DEFAULT = "LIGHT_SYNC"
    }

    @Ignore
    override val identifier: String = id

    data class TypesConfig(
        val url: String,
        val overridesCommon: Boolean,
    )
}

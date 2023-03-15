package io.novafoundation.nova.core_db.model.chain

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import io.novafoundation.nova.common.utils.Identifiable

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
    val hasCrowdloans: Boolean,
    val governance: String,
    val additional: String?,
) : Identifiable {

    object Default {

        const val HAS_SUBSTRATE_RUNTIME = 1
    }

    @Ignore
    override val identifier: String = id

    data class TypesConfig(
        val url: String,
        val overridesCommon: Boolean,
    )
}

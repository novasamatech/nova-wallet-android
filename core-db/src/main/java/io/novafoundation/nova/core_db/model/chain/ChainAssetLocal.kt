package io.novafoundation.nova.core_db.model.chain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import io.novafoundation.nova.common.utils.Identifiable

@Entity(
    tableName = "chain_assets",
    primaryKeys = ["chainId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = ChainLocal::class,
            parentColumns = ["id"],
            childColumns = ["chainId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["chainId"])
    ]
)
data class ChainAssetLocal(
    val id: Int,
    val chainId: String,
    val name: String,
    val symbol: String,
    val priceId: String?,
    val staking: String,
    val precision: Int,
    val icon: String?,
    val type: String?,
    @ColumnInfo(defaultValue = SOURCE_DEFAULT)
    val source: AssetSourceLocal,
    val buyProviders: String?,
    val typeExtras: String?,
    @ColumnInfo(defaultValue = ENABLED_DEFAULT)
    val enabled: Boolean,
) : Identifiable {

    companion object {

        const val SOURCE_DEFAULT = "DEFAULT"
        const val ENABLED_DEFAULT = "1"
    }

    @Ignore
    override val identifier: String = "$id:$chainId"
}

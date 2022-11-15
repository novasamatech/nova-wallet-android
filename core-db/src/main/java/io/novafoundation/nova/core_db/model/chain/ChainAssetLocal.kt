package io.novafoundation.nova.core_db.model.chain

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
    val source: AssetSourceLocal,
    val buyProviders: String?,
    val typeExtras: String?
) : Identifiable {

    @Ignore
    override val identifier: String = "$id:$chainId"
}

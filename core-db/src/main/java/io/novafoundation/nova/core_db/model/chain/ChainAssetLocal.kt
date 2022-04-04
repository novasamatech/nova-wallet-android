package io.novafoundation.nova.core_db.model.chain

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

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
class ChainAssetLocal(
    val id: Int,
    val chainId: String,
    val name: String,
    val symbol: String,
    val priceId: String?,
    val staking: String,
    val precision: Int,
    val icon: String?,
    val type: String?,
    val buyProviders: String?,
    val typeExtras: String?
)

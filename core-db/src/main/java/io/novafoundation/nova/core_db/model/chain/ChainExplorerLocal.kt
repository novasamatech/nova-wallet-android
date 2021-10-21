package io.novafoundation.nova.core_db.model.chain

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "chain_explorers",
    primaryKeys = ["chainId", "name"],
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
class ChainExplorerLocal(
    val chainId: String,
    val name: String,
    val extrinsic: String?,
    val account: String?,
    val event: String?
)

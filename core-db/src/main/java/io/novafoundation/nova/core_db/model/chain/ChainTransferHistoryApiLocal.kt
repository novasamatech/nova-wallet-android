package io.novafoundation.nova.core_db.model.chain

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import io.novafoundation.nova.common.utils.Identifiable

@Entity(
    tableName = "chain_transfer_history_apis",
    primaryKeys = ["chainId", "url"],
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
data class ChainTransferHistoryApiLocal(
    val chainId: String,
    val assetType: AssetType,
    val apiType: ApiType,
    val url: String
) : Identifiable {

    enum class AssetType {
        SUBSTRATE, EVM, UNSUPPORTED
    }

    enum class ApiType {
        SUBQUERY, GITHUB, UNKNOWN, POLKASSEMBLY, ETHERSCAN
    }

    @Ignore
    override val identifier: String = "$chainId:$url"
}

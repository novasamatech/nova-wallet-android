package io.novafoundation.nova.core_db.model.chain

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import io.novafoundation.nova.common.utils.Identifiable

@Entity(
    tableName = "chain_external_apis",
    primaryKeys = ["chainId", "url", "apiType"],
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
data class ChainExternalApiLocal(
    val chainId: String,
    val sourceType: SourceType,
    val apiType: ApiType,
    val parameters: String?,
    val url: String
) : Identifiable {

    enum class SourceType {
        SUBQUERY, GITHUB, POLKASSEMBLY, ETHERSCAN, SUBSQUARE,
        UNKNOWN
    }

    enum class ApiType {
        TRANSFERS, STAKING, CROWDLOANS,
        GOVERNANCE_REFERENDA, GOVERNANCE_DELEGATIONS,
        REFERENDUM_SUMMARY,

        UNKNOWN
    }

    @Ignore
    override val identifier: String = "$chainId:$url:$apiType"
}

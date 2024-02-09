package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import androidx.room.ForeignKey
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import java.math.BigInteger

@Entity(
    tableName = "externalBalances",
    primaryKeys = ["metaId", "chainId", "assetId", "type", "subtype"],
    foreignKeys = [
        ForeignKey(
            entity = ChainAssetLocal::class,
            parentColumns = ["id", "chainId"],
            childColumns = ["assetId", "chainId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MetaAccountLocal::class,
            parentColumns = ["id"],
            childColumns = ["metaId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class ExternalBalanceLocal(
    val metaId: Long,
    val chainId: String,
    val assetId: Int,
    val type: Type,
    val subtype: String,
    val amount: BigInteger
) {

    companion object {
        const val EMPTY_SUBTYPE = ""
    }

    enum class Type {
        CROWDLOAN, NOMINATION_POOL
    }
}

class AggregatedExternalBalanceLocal(
    val chainId: String,
    val assetId: Int,
    val type: ExternalBalanceLocal.Type,
    val aggregatedAmount: BigInteger
)

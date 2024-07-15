package io.novafoundation.nova.core_db.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import java.math.BigInteger

@Entity(
    tableName = "holds",
    primaryKeys = ["metaId", "chainId", "assetId", "id_module", "id_reason"],
    foreignKeys = [
        ForeignKey(
            entity = MetaAccountLocal::class,
            parentColumns = ["id"],
            childColumns = ["metaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChainLocal::class,
            parentColumns = ["id"],
            childColumns = ["chainId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChainAssetLocal::class,
            parentColumns = ["id", "chainId"],
            childColumns = ["assetId", "chainId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
)
class BalanceHoldLocal(
    val metaId: Long,
    val chainId: String,
    val assetId: Int,
    @Embedded(prefix = "id_") val id: HoldIdLocal,
    val amount: BigInteger
) {

    class HoldIdLocal(val module: String, val reason: String)
}

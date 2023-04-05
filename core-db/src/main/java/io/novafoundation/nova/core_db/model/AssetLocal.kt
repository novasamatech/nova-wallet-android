package io.novafoundation.nova.core_db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import java.math.BigInteger

@Entity(
    tableName = "assets",
    primaryKeys = ["assetId", "chainId", "metaId"],
    foreignKeys = [
        ForeignKey(
            entity = ChainAssetLocal::class,
            parentColumns = ["id", "chainId"],
            childColumns = ["assetId", "chainId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AssetLocal(
    val assetId: Int,
    val chainId: String,
    @ColumnInfo(index = true) val metaId: Long,

    val freeInPlanks: BigInteger,
    val frozenInPlanks: BigInteger,
    val reservedInPlanks: BigInteger,

    // TODO move to runtime storage
    val bondedInPlanks: BigInteger,
    val redeemableInPlanks: BigInteger,
    val unbondingInPlanks: BigInteger,
) : Identifiable {
    companion object {
        fun createEmpty(
            assetId: Int,
            chainId: String,
            metaId: Long
        ) = AssetLocal(
            assetId = assetId,
            chainId = chainId,
            metaId = metaId,
            freeInPlanks = BigInteger.ZERO,
            reservedInPlanks = BigInteger.ZERO,
            frozenInPlanks = BigInteger.ZERO,
            bondedInPlanks = BigInteger.ZERO,
            redeemableInPlanks = BigInteger.ZERO,
            unbondingInPlanks = BigInteger.ZERO
        )

    }

    override val identifier: String
        get() = "$metaId$chainId$assetId"
}

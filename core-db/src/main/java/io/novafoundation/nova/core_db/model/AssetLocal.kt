package io.novafoundation.nova.core_db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.math.BigInteger

@Entity(
    tableName = "assets",
    primaryKeys = ["tokenSymbol", "chainId", "metaId"],
)
data class AssetLocal(
    val tokenSymbol: String,
    val chainId: String,
    @ColumnInfo(index = true) val metaId: Long,

    val freeInPlanks: BigInteger,
    val frozenInPlanks: BigInteger,
    val reservedInPlanks: BigInteger,

    // TODO move to runtime storage
    val bondedInPlanks: BigInteger,
    val redeemableInPlanks: BigInteger,
    val unbondingInPlanks: BigInteger,
) {
    companion object {
        fun createEmpty(
            symbol: String,
            chainId: String,
            metaId: Long
        ) = AssetLocal(
            tokenSymbol = symbol,
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
}

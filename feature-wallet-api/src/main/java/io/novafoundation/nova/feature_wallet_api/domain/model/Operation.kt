package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

data class Operation(
    val id: String,
    val address: String,
    val type: Type,
    val time: Long,
    val chainAsset: Chain.Asset,
) {

    sealed class Type {

        data class Extrinsic(
            val hash: String,
            val module: String,
            val call: String,
            val fee: BigInteger,
            val status: Status,
        ) : Type()

        data class Reward(
            val amount: BigInteger,
            val isReward: Boolean,
            val era: Int,
            val validator: String?,
        ) : Type()

        data class Transfer(
            val hash: String?,
            val myAddress: String,
            val amount: BigInteger,
            val receiver: String,
            val sender: String,
            val status: Status,
            val fee: BigInteger?,
        ) : Type()
    }

    enum class Status {
        PENDING, COMPLETED, FAILED;

        companion object {
            fun fromSuccess(success: Boolean): Status {
                return if (success) COMPLETED else FAILED
            }
        }
    }
}

fun Operation.Type.satisfies(filters: Set<TransactionFilter>): Boolean {
    return matchingTransactionFilter() in filters
}

private fun Operation.Type.matchingTransactionFilter(): TransactionFilter {
    return when (this) {
        is Operation.Type.Extrinsic -> TransactionFilter.EXTRINSIC
        is Operation.Type.Reward -> TransactionFilter.REWARD
        is Operation.Type.Transfer -> TransactionFilter.TRANSFER
    }
}

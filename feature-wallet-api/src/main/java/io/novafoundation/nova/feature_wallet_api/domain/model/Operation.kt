package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

data class Operation(
    val id: String,
    val address: String,
    val type: Type,
    val time: Long,
    val chainAsset: Chain.Asset,
    val extrinsicHash: String?,
) {

    sealed class Type {

        data class Extrinsic(
            val content: Content,
            val fee: BigInteger,
            val fiatFee: BigDecimal?,
            val status: Status,
        ) : Type() {

            sealed class Content {

                class SubstrateCall(val module: String, val call: String) : Content()

                class ContractCall(val contractAddress: String, val function: String?) : Content()
            }
        }

        data class Reward(
            val amount: BigInteger,
            val fiatAmount: BigDecimal?,
            val isReward: Boolean,
            val kind: RewardKind
        ) : Type() {

            sealed class RewardKind {

                class Direct(val era: Int, val validator: String?): RewardKind()

                class Pool(val poolId: Int): RewardKind()
            }
        }

        data class Transfer(
            val myAddress: String,
            val amount: BigInteger,
            val fiatAmount: BigDecimal?,
            val receiver: String,
            val sender: String,
            val status: Status,
            val fee: BigInteger?
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

fun Operation.isZeroTransfer(): Boolean {
    return type is Operation.Type.Transfer && type.amount.isZero
}

private fun Operation.Type.matchingTransactionFilter(): TransactionFilter {
    return when (this) {
        is Operation.Type.Extrinsic -> TransactionFilter.EXTRINSIC
        is Operation.Type.Reward -> TransactionFilter.REWARD
        is Operation.Type.Transfer -> TransactionFilter.TRANSFER
    }
}

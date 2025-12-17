package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.math.BigDecimal
import java.math.BigInteger

data class Operation(
    val id: String,
    val address: String,
    val type: Type,
    val time: Long,
    val chainAsset: Chain.Asset,
    val extrinsicHash: String?,
    val status: Status,
) {

    sealed class Type {

        data class Extrinsic(
            val content: Content,
            val fee: BigInteger,
            val fiatFee: BigDecimal?,
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
            val eventId: String,
            val kind: RewardKind
        ) : Type() {

            sealed class RewardKind {

                class Direct(val era: Int?, val validator: String?) : RewardKind()

                class Pool(val poolId: Int) : RewardKind()
            }
        }

        data class Transfer(
            val myAddress: String,
            val amount: BigInteger,
            val fiatAmount: BigDecimal?,
            val receiver: String,
            val sender: String,
            val fee: BigInteger?
        ) : Type()

        data class Swap(
            val fee: ChainAssetWithAmount,
            val amountIn: ChainAssetWithAmount,
            val amountOut: ChainAssetWithAmount,
            val fiatAmount: BigDecimal?
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

data class ChainAssetWithAmount(
    val chainAsset: Chain.Asset,
    val amount: Balance,
)

val ChainAssetWithAmount.decimalAmount: BigDecimal
    get() = chainAsset.amountFromPlanks(amount)

fun ChainAssetWithAmount.toIdWithAmount(): ChainAssetIdWithAmount {
    return chainAsset.fullId.withAmount(amount)
}

data class ChainAssetIdWithAmount(
    val chainAssetId: FullChainAssetId,
    val amount: Balance,
)

fun FullChainAssetId.withAmount(amount: Balance): ChainAssetIdWithAmount {
    return ChainAssetIdWithAmount(this, amount)
}

fun Chain.Asset.withAmount(amount: Balance): ChainAssetWithAmount {
    return ChainAssetWithAmount(this, amount)
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
        is Operation.Type.Swap -> TransactionFilter.SWAP
    }
}

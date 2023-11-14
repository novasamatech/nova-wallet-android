package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class RealtimeHistoryUpdate(
    val txHash: String,
    val status: Operation.Status,
    val type: Type,
) {

    sealed class Type {

        abstract fun relates(accountId: AccountId): Boolean

        class Transfer(
            val senderId: AccountId,
            val recipientId: AccountId,
            val amountInPlanks: Balance,
            val chainAsset: Chain.Asset,
        ) : Type() {

            override fun relates(accountId: AccountId): Boolean {
                return senderId contentEquals accountId || recipientId contentEquals accountId
            }
        }

        class Swap(
            val amountIn: ChainAssetWithAmount,
            val amountOut: ChainAssetWithAmount,
            val amountFee: ChainAssetWithAmount,
            val senderId: AccountId,
            val receiverId: AccountId
        ) : Type() {

            override fun relates(accountId: AccountId): Boolean {
                return senderId contentEquals accountId || receiverId contentEquals accountId
            }
        }
    }
}

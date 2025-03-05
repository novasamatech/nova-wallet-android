package io.novafoundation.nova.feature_wallet_impl.data.network.model.response

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

class SubqueryHistoryElementResponse(val query: Query) {
    class Query(val historyElements: HistoryElements) {

        class HistoryElements(val nodes: Array<Node>, val pageInfo: PageInfo) {
            class PageInfo(
                val startCursor: String,
                val endCursor: String?
            )

            class Node(
                val id: String,
                val timestamp: Long,
                val extrinsicHash: String?,
                val address: String,
                val reward: Reward?,
                val blockNumber: Long,
                val poolReward: PoolReward?,
                val transfer: Transfer?,
                val extrinsic: Extrinsic?,
                val assetTransfer: AssetTransfer?,
                val swap: Swap?
            ) {
                class Reward(
                    val era: Int?,
                    val amount: String?,
                    val eventIdx: Int,
                    val isReward: Boolean,
                    val validator: String?,
                )

                class PoolReward(
                    val amount: BigInteger,
                    val eventIdx: Int,
                    val poolId: Int,
                    val isReward: Boolean,
                )

                class Transfer(
                    val amount: BigInteger,
                    val to: String,
                    val from: String,
                    val fee: BigInteger,
                    val success: Boolean
                )

                class Extrinsic(
                    val module: String,
                    val call: String,
                    val fee: BigInteger,
                    val success: Boolean
                )

                class AssetTransfer(
                    val assetId: String,
                    val amount: BigInteger,
                    val to: String,
                    val from: String,
                    val fee: BigInteger,
                    val success: Boolean
                )

                class Swap(
                    val assetIdIn: String?,
                    val amountIn: Balance,
                    val assetIdOut: String?,
                    val amountOut: Balance,
                    val sender: String,
                    val receiver: String,
                    val fee: Balance,
                    val assetIdFee: String?,
                    val success: Boolean?
                )
            }
        }
    }
}

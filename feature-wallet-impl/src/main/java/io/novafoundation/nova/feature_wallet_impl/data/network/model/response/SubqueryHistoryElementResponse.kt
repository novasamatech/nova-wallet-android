package io.novafoundation.nova.feature_wallet_impl.data.network.model.response

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
                val poolReward: PoolReward?,
                val transfer: Transfer?,
                val extrinsic: Extrinsic?,
                val assetTransfer: AssetTransfer?,
            ) {
                class Reward(
                    val era: Int,
                    val amount: BigInteger,
                    val isReward: Boolean,
                    val validator: String,
                )

                class PoolReward(
                    val era: Int,
                    val amount: BigInteger,
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
            }
        }
    }
}

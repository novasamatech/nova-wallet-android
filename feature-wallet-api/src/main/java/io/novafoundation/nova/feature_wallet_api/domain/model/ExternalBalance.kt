package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

class ExternalBalance(
    val chainAssetId: FullChainAssetId,
    val amount: Balance,
    val type: Type
) {

    enum class Type {
        CROWDLOAN, NOMINATION_POOL
    }
}

fun List<ExternalBalance>.aggregatedBalanceByAsset(): Map<FullChainAssetId, Balance> = groupBy { it.chainAssetId }
    .mapValues { (_, assetExternalBalances) -> assetExternalBalances.sumByBigInteger(ExternalBalance::amount) }

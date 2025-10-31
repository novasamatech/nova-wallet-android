package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets

import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

suspend fun AssetSourceRegistry.existentialDeposit(chainAsset: Chain.Asset): BigDecimal {
    return chainAsset.amountFromPlanks(existentialDepositInPlanks(chainAsset))
}

suspend fun AssetSourceRegistry.existentialDepositInPlanks(chainAsset: Chain.Asset): BigInteger {
    return sourceFor(chainAsset).balance.existentialDeposit(chainAsset)
}

suspend fun AssetSourceRegistry.totalCanBeDroppedBelowMinimumBalance(chainAsset: Chain.Asset): Boolean {
    return sourceFor(chainAsset).transfers.totalCanDropBelowMinimumBalance(chainAsset)
}

fun AssetSourceRegistry.isSelfSufficientAsset(chainAsset: Chain.Asset): Boolean {
    return sourceFor(chainAsset).balance.isSelfSufficient(chainAsset)
}

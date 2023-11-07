package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets

import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

suspend fun AssetSourceRegistry.existentialDeposit(chain: Chain, chainAsset: Chain.Asset): BigDecimal {
    return chainAsset.amountFromPlanks(existentialDepositInPlanks(chain, chainAsset))
}

suspend fun AssetSourceRegistry.existentialDepositInPlanks(chain: Chain, chainAsset: Chain.Asset): BigInteger {
    return sourceFor(chainAsset).balance.existentialDeposit(chain, chainAsset)
}

suspend fun AssetSourceRegistry.isSelfSufficientAsset(chainAsset: Chain.Asset): Boolean {
    return sourceFor(chainAsset).balance.isSelfSufficient(chainAsset)
}

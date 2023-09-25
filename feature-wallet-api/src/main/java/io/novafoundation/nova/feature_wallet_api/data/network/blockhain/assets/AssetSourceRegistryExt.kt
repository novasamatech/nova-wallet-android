package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets

import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

suspend fun AssetSourceRegistry.existentialDeposit(chain: Chain, chainAsset: Chain.Asset): BigDecimal {
    val existentialDeposit = sourceFor(chainAsset).balance.existentialDeposit(chain, chainAsset)
    return chainAsset.amountFromPlanks(existentialDeposit)
}

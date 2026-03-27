package io.novafoundation.nova.feature_assets.domain.assets.filters

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.math.BigDecimal

class DustBalanceFilter(
    val thresholdUsd: BigDecimal,
    private val exemptTokenIds: Set<FullChainAssetId>
) : AssetFilter {

    override val name: String = "DustBalance"

    override fun shouldInclude(model: Asset): Boolean {
        val fullId = model.token.configuration.fullId
        if (fullId in exemptTokenIds) return true

        // If no price data, hide the token (likely spam/dust)
        val fiatValue = model.token.toFiatOrNull(model.total) ?: return false

        return fiatValue >= thresholdUsd
    }
}

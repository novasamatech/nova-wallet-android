package io.novafoundation.nova.feature_assets.domain.assets.filters

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

object NonZeroBalanceFilter : AssetFilter {

    override val name: String = "NonZeroBalance"

    override fun shouldInclude(model: Asset) = model.total > BigDecimal.ZERO
}

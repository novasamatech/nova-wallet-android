package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.math.BigDecimal

class AmountWithAssetId(
    val assetId: FullChainAssetId,
    val amount: BigDecimal
)

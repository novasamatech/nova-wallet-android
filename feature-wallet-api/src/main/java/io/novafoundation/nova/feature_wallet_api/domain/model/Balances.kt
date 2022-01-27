package io.novafoundation.nova.feature_wallet_api.domain.model

import java.math.BigDecimal

class Balances(
    val assets: List<Asset>,
    val totalBalanceFiat: BigDecimal,
    val lockedBalanceFiat: BigDecimal
)

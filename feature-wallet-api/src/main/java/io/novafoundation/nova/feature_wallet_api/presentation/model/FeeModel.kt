package io.novafoundation.nova.feature_wallet_api.presentation.model

import java.math.BigDecimal

class FeeModel(
    val fee: BigDecimal,
    val displayToken: String,
    val displayFiat: String?
)

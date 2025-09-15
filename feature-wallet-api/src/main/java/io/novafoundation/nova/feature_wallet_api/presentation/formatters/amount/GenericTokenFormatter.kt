package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount

import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import java.math.BigDecimal

interface GenericTokenFormatter<T> {

    fun formatToken(
        amount: BigDecimal,
        token: TokenBase,
        config: TokenConfig = TokenConfig()
    ): T
}

package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount

import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import java.math.BigDecimal

interface GenericAmountFormatter<T> {
    fun formatAmountToAmountModel(amount: BigDecimal, token: TokenBase, config: AmountConfig = AmountConfig()): T
}

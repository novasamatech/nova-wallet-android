package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.utils.TokenSymbol
import java.math.BigDecimal

internal data class BittensorMaxAmount(val amount: BigDecimal, val symbol: TokenSymbol)

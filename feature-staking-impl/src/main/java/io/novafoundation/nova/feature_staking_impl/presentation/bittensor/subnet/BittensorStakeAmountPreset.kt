package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import java.math.BigDecimal

enum class BittensorStakeAmountPreset {
    ONE,
    FIVE,
    TEN,
    MAX;

    fun amountFor(availableBalance: BigDecimal): BigDecimal = when (this) {
        ONE -> BigDecimal.ONE
        FIVE -> 5.toBigDecimal()
        TEN -> BigDecimal.TEN
        MAX -> availableBalance
    }
}

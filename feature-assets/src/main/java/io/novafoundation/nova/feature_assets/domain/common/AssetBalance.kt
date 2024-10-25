package io.novafoundation.nova.feature_assets.domain.common

import java.math.BigDecimal

class AssetBalance(
    val total: PricedAmount,
    val transferable: PricedAmount
) {

    companion object {
        val ZERO = AssetBalance(PricedAmount(BigDecimal.ZERO, BigDecimal.ZERO), PricedAmount(BigDecimal.ZERO, BigDecimal.ZERO))
    }

    operator fun plus(other: AssetBalance): AssetBalance {
        return AssetBalance(
            total + other.total,
            transferable + other.transferable
        )
    }
}

class PricedAmount(
    val amount: BigDecimal,
    val fiat: BigDecimal
) {

    operator fun plus(other: PricedAmount): PricedAmount {
        return PricedAmount(
            amount + other.amount,
            fiat + other.fiat
        )
    }
}

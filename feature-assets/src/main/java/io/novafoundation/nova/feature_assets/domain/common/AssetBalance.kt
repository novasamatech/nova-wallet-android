package io.novafoundation.nova.feature_assets.domain.common

import java.math.BigDecimal

class AssetBalance(
    val total: Amount,
    val transferable: Amount
) {

    class Amount(
        val amount: BigDecimal,
        val fiat: BigDecimal
    ) {

        operator fun plus(other: Amount): Amount {
            return Amount(
                amount + other.amount,
                fiat + other.fiat
            )
        }
    }

    companion object {
        val ZERO = AssetBalance(Amount(BigDecimal.ZERO, BigDecimal.ZERO), Amount(BigDecimal.ZERO, BigDecimal.ZERO))
    }

    operator fun plus(other: AssetBalance): AssetBalance {
        return AssetBalance(
            total + other.total,
            transferable + other.transferable
        )
    }
}

package io.novafoundation.nova.feature_assets.domain.common

import java.math.BigDecimal

class TotalAndTransferableBalance(
    val total: Amount,
    val transferable: Amount
) {

    companion object {
        val ZERO = TotalAndTransferableBalance(Amount(BigDecimal.ZERO, BigDecimal.ZERO), Amount(BigDecimal.ZERO, BigDecimal.ZERO))
    }

    operator fun plus(other: TotalAndTransferableBalance): TotalAndTransferableBalance {
        return TotalAndTransferableBalance(
            total + other.total,
            transferable + other.transferable
        )
    }
}

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

package io.novafoundation.nova.feature_wallet_impl.presentation.balance.list.model

import io.novafoundation.nova.feature_wallet_impl.presentation.model.AssetModel
import java.math.BigDecimal
import java.math.BigInteger

class BalanceModel(val assetModels: List<AssetModel>) {
    val totalBalance = calculateTotalBalance()

    private fun calculateTotalBalance(): BigDecimal {
        return assetModels.fold(BigDecimal(BigInteger.ZERO)) { acc, current ->
            val toAdd = current.dollarAmount ?: BigDecimal.ZERO

            acc + toAdd
        }
    }
}

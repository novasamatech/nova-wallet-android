package io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion

import io.novafoundation.nova.feature_swap_api.domain.model.NovaSwapCommission
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.amountOutMin
import io.novafoundation.nova.feature_swap_api.domain.model.estimatedAmountOut
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

class AssetHubCommissionResolver(
    private val commission: NovaSwapCommission,
) {

    fun resolveRequired(
        swapLimit: SwapLimit,
        minimumBalance: Balance,
    ): Balance? {
        val amount = commission.commissionIncludedIn(swapLimit)
        if (amount == BigInteger.ZERO) return null

        check(commissionFits(amount, swapLimit, minimumBalance)) {
            "Asset Hub commission does not fit the updated swap amount"
        }

        return amount
    }

    fun resolveOrWaive(
        swapLimit: SwapLimit,
        minimumBalance: Balance,
    ): Balance? {
        val amount = commission.commissionIncludedIn(swapLimit)

        return amount.takeIf {
            it > BigInteger.ZERO && commissionFits(it, swapLimit, minimumBalance)
        }
    }

    private fun commissionFits(amount: Balance, swapLimit: SwapLimit, minimumBalance: Balance): Boolean {
        return amount < swapLimit.estimatedAmountOut && swapLimit.amountOutMin - amount >= minimumBalance
    }
}

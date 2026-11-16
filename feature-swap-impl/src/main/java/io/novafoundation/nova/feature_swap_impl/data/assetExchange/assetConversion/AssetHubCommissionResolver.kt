package io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion

import io.novafoundation.nova.feature_swap_api.domain.model.NovaSwapCommission
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.estimatedAmountOut
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

class AssetHubCommissionResolver(
    private val commission: NovaSwapCommission,
) {

    fun resolveRequired(swapLimit: SwapLimit): Balance? {
        val amount = commission.commissionIncludedIn(swapLimit)
        if (amount == BigInteger.ZERO) return null

        // Output ED is validated against the wallet's existing balance plus the net swap output.
        // Requiring the swap output alone to cover ED rejects small swaps into an already funded account.
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
        val netMinimum = commission.minimumAmountOutAfterCommission(swapLimit, amount)
        return amount < swapLimit.estimatedAmountOut && netMinimum >= minimumBalance
    }
}

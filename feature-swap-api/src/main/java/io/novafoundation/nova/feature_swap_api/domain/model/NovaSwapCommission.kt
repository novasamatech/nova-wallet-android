package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.extensions.fromHex
import java.math.BigInteger

class NovaSwapCommission {

    val hydrationFeeAccountId: ByteArray by lazy {
        HYDRATION_FEE_ACCOUNT_HEX.fromHex()
    }

    fun assetHubFeeAccountId(chainId: ChainId): ByteArray? {
        return ASSET_HUB_FEE_ACCOUNTS[chainId]?.fromHex()
    }

    fun commissionToAddOnTop(net: Balance): Balance {
        return net * FEE_NUMERATOR / FEE_DENOMINATOR
    }

    fun commissionIncludedIn(gross: Balance): Balance {
        return gross * FEE_NUMERATOR / (FEE_DENOMINATOR + FEE_NUMERATOR)
    }

    fun commissionIncludedIn(swapLimit: SwapLimit): Balance {
        return commissionIncludedIn(swapLimit.estimatedAmountOut).max(BigInteger.ZERO)
    }

    fun minimumAmountOutAfterCommission(swapLimit: SwapLimit, commissionAmount: Balance): Balance {
        val grossAmountOut = swapLimit.estimatedAmountOut
        if (grossAmountOut <= BigInteger.ZERO) return swapLimit.amountOutMin

        val netAmountOut = (grossAmountOut - commissionAmount).max(BigInteger.ZERO)
        return swapLimit.amountOutMin * netAmountOut / grossAmountOut
    }

    /**
     * The commission is a separate transfer after the swap. Raise the swap's gross on-chain floor
     * so that deducting that fixed transfer cannot leave less than the net minimum shown to the user.
     */
    fun protectMinimumOutput(swapLimit: SwapLimit, commissionAmount: Balance): SwapLimit {
        if (swapLimit !is SwapLimit.SpecifiedIn || commissionAmount <= BigInteger.ZERO) return swapLimit

        val netMinimum = minimumAmountOutAfterCommission(swapLimit, commissionAmount)
        return swapLimit.copy(amountOutMin = netMinimum + commissionAmount)
    }

    companion object {

        val FEE_NUMERATOR: BigInteger = BigInteger.valueOf(85)
        val FEE_DENOMINATOR: BigInteger = BigInteger.valueOf(10000)
        const val FEE_PERCENT_DISPLAY = "0.85"

        // Hydration SS58: 15ReoCRFgpGXjuaFXzGv7qaqiRrFE5uEMGVC7tBQhaWfzXh
        const val HYDRATION_FEE_ACCOUNT_HEX = "035ff76d86ca67ef0499f8597101aab0e6ad894a805cd93a51409bd6d71a8841"

        // Polkadot Asset Hub SS58: 15WGd8nfLawEAZcMjrWecJ3ngofF3UzTVU2YQjt4PApf6JCR
        const val POLKADOT_ASSET_HUB_FEE_ACCOUNT_HEX = "c743a46b2294ae6fc9bcc2de952b52899e8d1f335fc7810ee7a4d4a59d91d087"

        private val ASSET_HUB_FEE_ACCOUNTS: Map<ChainId, String> = mapOf(
            Chain.Geneses.POLKADOT_ASSET_HUB to POLKADOT_ASSET_HUB_FEE_ACCOUNT_HEX
        )
    }
}

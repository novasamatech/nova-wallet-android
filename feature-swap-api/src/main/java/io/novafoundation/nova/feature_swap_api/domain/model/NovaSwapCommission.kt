package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.extensions.fromHex
import java.math.BigInteger

object NovaSwapCommission {

    val FEE_NUMERATOR: BigInteger = BigInteger.valueOf(85)
    val FEE_DENOMINATOR: BigInteger = BigInteger.valueOf(10000)
    const val FEE_PERCENT_DISPLAY = "0.85"

    // Fee recipient account on Hydration (SS58: 15ReoCRFgpGXjuaFXzGv7qaqiRrFE5uEMGVC7tBQhaWfzXh)
    const val FEE_ACCOUNT_HEX = "035ff76d86ca67ef0499f8597101aab0e6ad894a805cd93a51409bd6d71a8841"

    val feeAccountId: ByteArray by lazy {
        FEE_ACCOUNT_HEX.fromHex()
    }

    fun feeAmount(amountOut: Balance): Balance {
        return amountOut * FEE_NUMERATOR / FEE_DENOMINATOR
    }

    fun amountOutAfterFee(amountOut: Balance): Balance {
        return amountOut - feeAmount(amountOut)
    }
}

package io.novafoundation.nova.feature_account_api.data.model

import java.math.BigInteger

interface Fee {

    companion object

    val amount: BigInteger
}

data class EvmFee(val gasLimit: BigInteger, val gasPrice: BigInteger) : Fee {
    override val amount = gasLimit * gasPrice
}

@JvmInline
value class InlineFee(override val amount: BigInteger) : Fee


fun Fee.Companion.zero(): Fee = InlineFee(BigInteger.ZERO)

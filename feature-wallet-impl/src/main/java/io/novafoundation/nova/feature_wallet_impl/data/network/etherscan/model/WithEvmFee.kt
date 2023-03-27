package io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.model

import java.math.BigInteger

interface WithEvmFee {

    val gasPrice: BigInteger

    val gasUsed: BigInteger
}

val WithEvmFee.feeUsed
    get() = gasUsed * gasPrice

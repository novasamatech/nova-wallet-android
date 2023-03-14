package io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.model

import java.math.BigInteger

class EtherscanAccountTransfer(
    val timeStamp: Long,
    val hash: String,
    val from: String,
    val to: String,
    val value: BigInteger,
    override val gasPrice: BigInteger,
    override val gasUsed: BigInteger,
) : WithEvmFee

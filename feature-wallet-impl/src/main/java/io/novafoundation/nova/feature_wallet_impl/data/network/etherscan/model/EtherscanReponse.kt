package io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.model

class EtherscanResponse<T>(
    val status: String,
    val message: String,
    val result: T
)

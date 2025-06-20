package io.novafoundation.nova.runtime.network.etherscan.model

class EtherscanResponse<T>(
    val status: String,
    val message: String,
    val result: T
)

package io.novafoundation.nova.feature_dapp_impl.web3.metamask.model

class MetamaskTransaction(
    val gas: String?,
    val gasPrice: String?,
    val from: String,
    val to: String,
    val data: String?,
    val value: String?,
    val nonce: String?,
)

class MetamaskTypedMessage(
    val data: String,
    val raw: String?
)

class MetamaskPersonalSignMessage(
    val data: String
)

package io.novafoundation.nova.feature_dapp_impl.domain.browser

class DAppInfo(
    val baseUrl: String,
    val metadata: DappMetadata?
)

class DappMetadata(
    val name: String,
    val iconLink: String,
    val url: String
)

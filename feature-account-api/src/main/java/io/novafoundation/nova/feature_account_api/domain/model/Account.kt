package io.novafoundation.nova.feature_account_api.domain.model

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.core.model.Network
import io.novasama.substrate_sdk_android.extensions.fromHex

data class Account(
    val address: String,
    val name: String?,
    val accountIdHex: String,
    val cryptoType: CryptoType, // TODO make optional
    val position: Int,
    val network: Network, // TODO remove when account management will be rewritten,
) {

    val accountId = accountIdHex.fromHex()
}

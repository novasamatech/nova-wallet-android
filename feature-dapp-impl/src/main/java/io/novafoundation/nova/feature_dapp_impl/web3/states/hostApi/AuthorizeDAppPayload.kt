package io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi

import io.novafoundation.nova.common.address.AddressModel

class AuthorizeDAppPayload(
    val title: String,
    val dAppIconUrl: String?,
    val dAppUrl: String,
    val walletAddressModel: AddressModel
)

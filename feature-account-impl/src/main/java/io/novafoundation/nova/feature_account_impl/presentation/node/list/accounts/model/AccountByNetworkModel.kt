package io.novafoundation.nova.feature_account_impl.presentation.node.list.accounts.model

import io.novafoundation.nova.common.address.AddressModel

data class AccountByNetworkModel(
    val nodeId: Int,
    val accountAddress: String,
    val name: String?,
    val addressModel: AddressModel
)

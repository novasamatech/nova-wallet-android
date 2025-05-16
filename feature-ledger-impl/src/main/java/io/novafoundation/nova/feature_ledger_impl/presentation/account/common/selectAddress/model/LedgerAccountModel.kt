package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.model

import io.novafoundation.nova.common.address.AddressModel

data class LedgerAccountModel(
    val id: Int,
    val label: String,
    val substrate: AddressModel,
    val evm: AddressModel?
)

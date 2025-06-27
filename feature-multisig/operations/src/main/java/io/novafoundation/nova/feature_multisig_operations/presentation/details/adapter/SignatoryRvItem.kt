package io.novafoundation.nova.feature_multisig_operations.presentation.details.adapter

import io.novafoundation.nova.common.address.AddressModel

data class SignatoryRvItem(
    val address: AddressModel,
    val subtitle: CharSequence?,
    val isApproved: Boolean,
)

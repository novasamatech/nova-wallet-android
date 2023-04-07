package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount

import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputState

class ExternalAccount(
    val address: String,
    val description: String?,
    val addressWithDescription: String,
    val isValid: Boolean,
    val icon: AddressInputState.IdenticonState
)

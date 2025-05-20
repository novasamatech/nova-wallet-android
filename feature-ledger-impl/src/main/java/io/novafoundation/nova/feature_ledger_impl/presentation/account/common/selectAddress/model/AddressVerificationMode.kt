package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.model

import io.novafoundation.nova.common.address.format.AddressScheme

sealed class AddressVerificationMode {

    data object Disabled: AddressVerificationMode()

    class Enabled(val addressSchemesToVerify: List<AddressScheme>): AddressVerificationMode()
}

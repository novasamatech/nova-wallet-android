package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import android.content.Context
import io.novafoundation.nova.common.presentation.toShortAddressFormat
import io.novafoundation.nova.feature_account_api.R
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class ExternalAccount(
    val accountId: AccountId?,
    val address: String,
    val description: String?,
    val addressWithDescription: String,
    val isValid: Boolean,
    val icon: AddressInputState.IdenticonState
)

fun ExternalAccount.addressWithDescription(context: Context): String {
    return if (description != null) {
        return context.getString(R.string.web3names_address_with_description, address.toShortAddressFormat(), description)
    } else {
        address.toShortAddressFormat()
    }
}

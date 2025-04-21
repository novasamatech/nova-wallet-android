package io.novafoundation.nova.feature_account_impl.data.multisig.model

import io.novafoundation.nova.common.address.AccountIdKey

class DiscoveredMultisig(
    val accountId: AccountIdKey,
    val allSignatories: List<AccountIdKey>,
    val threshold: Int,
)

fun DiscoveredMultisig.otherSignatories(signatory: AccountIdKey): List<AccountIdKey> {
    return allSignatories - signatory
}

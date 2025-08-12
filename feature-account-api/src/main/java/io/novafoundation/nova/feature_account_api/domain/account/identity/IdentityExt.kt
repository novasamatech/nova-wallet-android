package io.novafoundation.nova.feature_account_api.domain.account.identity

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.presentation.ellipsizeAddress
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain


suspend fun IdentityProvider.getNameOrAddress(accountId: AccountIdKey, chain: Chain): String {
    return identityFor(accountId.value, chain.id)?.name ?: chain.addressOf(accountId).ellipsizeAddress()
}

package io.novafoundation.nova.feature_account_api.domain.account.identity

import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity

data class Identity(val name: String)

fun Identity(onChainIdentity: OnChainIdentity): Identity? {
    return onChainIdentity.display?.let { Identity(it) }
}

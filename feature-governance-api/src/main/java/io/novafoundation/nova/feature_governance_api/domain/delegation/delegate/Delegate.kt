package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate

import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import io.novasama.substrate_sdk_android.runtime.AccountId

interface Delegate {

    val accountId: AccountId

    val metadata: Metadata?

    val onChainIdentity: OnChainIdentity?

    interface Metadata {

        val name: String?

        val iconUrl: String?

        val accountType: DelegateAccountType
    }
}

package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.label

import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.Delegate
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegateAccountType
import io.novasama.substrate_sdk_android.runtime.AccountId

class DelegateLabel(
    override val accountId: AccountId,
    override val metadata: Delegate.Metadata?,
    override val onChainIdentity: OnChainIdentity?
) : Delegate {

    class Metadata(
        override val name: String?,
        override val iconUrl: String?,
        override val accountType: DelegateAccountType
    ) : Delegate.Metadata
}

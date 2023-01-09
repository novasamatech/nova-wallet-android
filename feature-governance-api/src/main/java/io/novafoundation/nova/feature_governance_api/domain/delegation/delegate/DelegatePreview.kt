package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate

import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import jp.co.soramitsu.fearless_utils.runtime.AccountId

data class DelegatePreview(
    override val accountId: AccountId,
    override val stats: DelegateStats,
    override val metadata: Metadata?,
    override val onChainIdentity: OnChainIdentity?
) : Delegate {

    data class Metadata(
        val shortDescription: String,
        override val iconUrl: String?,
        override val accountType: DelegateAccountType,
        override val name: String?
    ) : Delegate.Metadata
}

package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate

import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import jp.co.soramitsu.fearless_utils.runtime.AccountId

data class DelegatePreview(
    override val accountId: AccountId,
    override val stats: DelegateStats,
    override val metadata: Metadata?,
    val onChainIdentity: OnChainIdentity?
) : Delegate {

    data class Metadata(
        val shortDescription: String,
        val profileImageUrl: String?,
        override val accountType: DelegateAccountType
    ) : Delegate.Metadata
}

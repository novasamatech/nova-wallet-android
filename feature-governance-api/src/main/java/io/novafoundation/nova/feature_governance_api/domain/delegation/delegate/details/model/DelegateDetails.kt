package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model

import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.Delegate
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegateAccountType
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.runtime.AccountId

data class DelegateDetails(
    override val accountId: AccountId,
    val stats: Stats?,
    override val metadata: Metadata?,
    override val onChainIdentity: OnChainIdentity?
) : Delegate {

    data class Metadata(
        val shortDescription: String,
        val longDescription: String?,
        override val iconUrl: String?,
        override val accountType: DelegateAccountType,
        override val name: String?
    ) : Delegate.Metadata

    data class Stats(val delegationsCount: Int, val delegatedVotes: Balance, val recentVotes: Int, val allVotes: Int)
}

val DelegateDetails.Metadata.description: String?
    get() = longDescription ?: shortDescription

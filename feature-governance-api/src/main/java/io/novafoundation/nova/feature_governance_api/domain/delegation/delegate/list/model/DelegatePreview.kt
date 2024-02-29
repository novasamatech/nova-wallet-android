package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model

import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.Delegate
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegateAccountType
import io.novafoundation.nova.feature_governance_api.domain.track.Track
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.runtime.AccountId

data class DelegatePreview(
    override val accountId: AccountId,
    val stats: Stats?,
    override val metadata: Metadata?,
    override val onChainIdentity: OnChainIdentity?,
    val userDelegations: Map<Track, Voting.Delegating>
) : Delegate {

    data class Metadata(
        val shortDescription: String,
        override val iconUrl: String?,
        override val accountType: DelegateAccountType,
        override val name: String?
    ) : Delegate.Metadata

    data class Stats(val delegationsCount: Int, val delegatedVotes: Balance, val recentVotes: Int)
}

fun DelegatePreview.hasMetadata(): Boolean {
    return metadata != null
}

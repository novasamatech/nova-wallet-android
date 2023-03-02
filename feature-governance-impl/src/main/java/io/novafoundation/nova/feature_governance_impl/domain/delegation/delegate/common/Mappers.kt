package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common

import io.novafoundation.nova.common.address.get
import io.novafoundation.nova.feature_account_api.data.model.AccountIdKeyMap
import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateMetadata
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateStats
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegateAccountType
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegatePreview
import io.novafoundation.nova.feature_governance_api.domain.track.Track

fun mapAccountTypeToDomain(isOrganization: Boolean): DelegateAccountType {
    return if (isOrganization) DelegateAccountType.ORGANIZATION else DelegateAccountType.INDIVIDUAL
}

fun mapDelegateStatsToPreviews(
    delegateStatsList: List<DelegateStats>,
    delegateMetadata: AccountIdKeyMap<DelegateMetadata>,
    identities: AccountIdKeyMap<OnChainIdentity?>,
    userDelegations: AccountIdKeyMap<List<Pair<Track, Voting.Delegating>>>,
): List<DelegatePreview> {
    return delegateStatsList.map { delegateStats ->
        val metadata = delegateMetadata[delegateStats.accountId]
        val identity = identities[delegateStats.accountId]

        DelegatePreview(
            accountId = delegateStats.accountId,
            stats = mapStatsToDomain(delegateStats),
            metadata = mapMetadataToDomain(metadata),
            onChainIdentity = identity,
            userDelegations = userDelegations[delegateStats.accountId]?.toMap().orEmpty()
        )
    }
}

private fun mapStatsToDomain(stats: DelegateStats): DelegatePreview.Stats {
    return DelegatePreview.Stats(
        delegatedVotes = stats.delegatedVotes,
        delegationsCount = stats.delegationsCount,
        recentVotes = stats.recentVotes
    )
}

private fun mapMetadataToDomain(metadata: DelegateMetadata?): DelegatePreview.Metadata? {
    if (metadata == null) return null

    return DelegatePreview.Metadata(
        shortDescription = metadata.shortDescription,
        accountType = mapAccountTypeToDomain(metadata.isOrganization),
        iconUrl = metadata.profileImageUrl,
        name = metadata.name
    )
}

package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common

import io.novafoundation.nova.common.address.intoKey
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
    delegateStats: List<DelegateStats>,
    delegateMetadata: AccountIdKeyMap<DelegateMetadata>,
    identities: AccountIdKeyMap<OnChainIdentity?>,
    userDelegations: AccountIdKeyMap<List<Pair<Track, Voting.Delegating>>>,
): List<DelegatePreview> {
    val delegateStatsById = delegateStats.associateBy { it.accountId.intoKey() }
    val allIds = delegateStatsById.keys + delegateMetadata.keys + userDelegations.keys

    return allIds.map { accountId ->
        val stats = delegateStatsById[accountId]
        val metadata = delegateMetadata[accountId]
        val identity = identities[accountId]

        DelegatePreview(
            accountId = accountId.value,
            stats = stats?.let { mapStatsToDomain(it) },
            metadata = mapMetadataToDomain(metadata),
            onChainIdentity = identity,
            userDelegations = userDelegations[accountId]?.toMap().orEmpty()
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

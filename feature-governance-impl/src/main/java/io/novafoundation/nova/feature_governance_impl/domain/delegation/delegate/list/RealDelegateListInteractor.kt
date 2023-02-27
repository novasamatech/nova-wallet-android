package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.list

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.get
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.applyFilter
import io.novafoundation.nova.feature_account_api.data.model.AccountIdKeyMap
import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.getIdOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateMetadata
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateStats
import io.novafoundation.nova.feature_governance_api.data.repository.getDelegatesMetadataOrEmpty
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.DelegateListInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegateFiltering
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegatePreview
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegateSorting
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.delegateComparator
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.hasMetadata
import io.novafoundation.nova.feature_governance_api.domain.track.Track
import io.novafoundation.nova.feature_governance_impl.data.repository.DelegationBannerRepository
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.RECENT_VOTES_PERIOD
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.mapAccountTypeToDomain
import io.novafoundation.nova.feature_governance_impl.domain.track.mapTrackInfoToTrack
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.blockDurationEstimator
import io.novafoundation.nova.runtime.util.blockInPast
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RealDelegateListInteractor(
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val chainStateRepository: ChainStateRepository,
    private val identityRepository: OnChainIdentityRepository,
    private val delegationBannerService: DelegationBannerRepository,
    private val accountRepository: AccountRepository
) : DelegateListInteractor {

    override fun shouldShowDelegationBanner(): Flow<Boolean> {
        return delegationBannerService.shouldShowBannerFlow()
    }

    override fun hideDelegationBanner() {
        delegationBannerService.hideBanner()
    }

    override suspend fun getDelegates(
        governanceOption: SupportedGovernanceOption,
    ): Flow<List<DelegatePreview>> = coroutineScope {
        val chain = governanceOption.assetWithChain.chain
        val delegateMetadataDeferred = async { getMetadata(governanceOption) }
        val delegatesStatsDeferred = async { getDelegatesStats(governanceOption) }
        val tracksDeferred = async { getTracks(governanceOption) }

        chainStateRepository.currentBlockNumberFlow(chain.id).map {
            val userDelegates = getUserDelegationsOrEmpty(governanceOption, tracksDeferred.await())
            val userDelegateIds = userDelegates.keys.map { it.value }

            val identities = identityRepository.getIdentitiesFromIds(userDelegateIds, chain.id)

            mapDelegateStatsToPreviews(
                delegatesStatsDeferred.await(),
                delegateMetadataDeferred.await(),
                identities,
                userDelegates
            )
        }
    }

    override suspend fun getUserDelegates(governanceOption: SupportedGovernanceOption): Flow<List<DelegatePreview>> = coroutineScope {
        val chain = governanceOption.assetWithChain.chain
        val delegateMetadataDeferred = async { getMetadata(governanceOption) }
        val tracksDeferred = async { getTracks(governanceOption) }
        var delegatesStats: List<DelegateStats>? = null
        var oldUserDelegateIds: Set<AccountIdKey> = setOf()

        chainStateRepository.currentBlockNumberFlow(chain.id).map {
            val userDelegations = getUserDelegationsOrEmpty(governanceOption, tracksDeferred.await())
            val userDelegateIdsSet = userDelegations.keys
            val userDelegateIdsList = userDelegateIdsSet.map { it.value }

            if (delegatesStats == null || oldUserDelegateIds != userDelegateIdsSet) {
                oldUserDelegateIds = userDelegateIdsSet
                delegatesStats = getDelegatesStats(governanceOption, userDelegateIdsList)
            }

            val identities = identityRepository.getIdentitiesFromIds(userDelegateIdsList, chain.id)

            mapDelegateStatsToPreviews(
                delegatesStats!!,
                delegateMetadataDeferred.await()
                    .filterKeys { userDelegateIdsSet.contains(it) },
                identities,
                userDelegations
            )
        }
    }

    override suspend fun applySortingAndFiltering(
        sorting: DelegateSorting,
        filtering: DelegateFiltering,
        delegates: List<DelegatePreview>
    ): List<DelegatePreview> {
        val filteredDelegates = delegates.applyFilter(filtering)
        return applySorting(sorting, filteredDelegates)
    }

    override suspend fun applySorting(
        sorting: DelegateSorting,
        delegates: List<DelegatePreview>
    ): List<DelegatePreview> {
        val comparator = getMetadataComparator()
            .thenComparing(sorting.delegateComparator())

        return delegates.sortedWith(comparator)
    }

    private fun getMetadataComparator(): Comparator<DelegatePreview> {
        return compareByDescending { it.hasMetadata() }
    }

    private suspend fun getDelegatesStats(
        governanceOption: SupportedGovernanceOption,
        accountIds: List<AccountId>? = null
    ): List<DelegateStats> {
        val chain = governanceOption.assetWithChain.chain
        val delegationsRepository = governanceSourceRegistry.sourceFor(governanceOption).delegationsRepository
        val blockDurationEstimator = chainStateRepository.blockDurationEstimator(chain.id)
        val recentVotesBlockThreshold = blockDurationEstimator.blockInPast(RECENT_VOTES_PERIOD)

        return if (accountIds == null) {
            delegationsRepository.getDelegatesStats(recentVotesBlockThreshold, chain)
        } else {
            delegationsRepository.getDelegatesStatsByAccountIds(recentVotesBlockThreshold, accountIds, chain)
        }
    }

    private suspend fun getMetadata(governanceOption: SupportedGovernanceOption): AccountIdKeyMap<DelegateMetadata> {
        val chain = governanceOption.assetWithChain.chain
        val delegationsRepository = governanceSourceRegistry.sourceFor(governanceOption)
            .delegationsRepository
        return delegationsRepository.getDelegatesMetadataOrEmpty(chain)
            .associateBy { AccountIdKey(it.accountId) }
    }

    private suspend fun getTracks(governanceOption: SupportedGovernanceOption): Map<TrackId, Track> {
        val chain = governanceOption.assetWithChain.chain
        val referendaRepository = governanceSourceRegistry.sourceFor(governanceOption).referenda
        return referendaRepository.getTracks(chain.id)
            .map { mapTrackInfoToTrack(it) }
            .associateBy { it.id }
    }

    private suspend fun getUserDelegationsOrEmpty(
        governanceOption: SupportedGovernanceOption,
        tracks: Map<TrackId, Track>
    ): AccountIdKeyMap<List<Pair<Track, Voting.Delegating>>> {
        val chain = governanceOption.assetWithChain.chain
        val convictionVotingRepository = governanceSourceRegistry.sourceFor(governanceOption).convictionVoting

        val accountId = accountRepository.getIdOfSelectedMetaAccountIn(chain) ?: return emptyMap()

        val delegatingDeferred = convictionVotingRepository.delegatingFor(accountId, chain.id)

        return delegatingDeferred
            .mapKeys { tracks.getValue(it.key) }
            .toList()
            .groupBy { it.second.target.intoKey() }
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

    private fun mapDelegateStatsToPreviews(
        delegateStats: List<DelegateStats>,
        delegateMetadata: AccountIdKeyMap<DelegateMetadata>,
        identities: AccountIdKeyMap<OnChainIdentity?>,
        userDelegations: AccountIdKeyMap<List<Pair<Track, Voting.Delegating>>>,
    ): List<DelegatePreview> {
        val statsAndMetadata = uniteStatsAndMetadata(delegateStats, delegateMetadata)
        return statsAndMetadata.map { entry ->
            val accountId = entry.key.value
            val metadata = entry.value.first
            val stats = entry.value.second
            val identity = identities[accountId]

            DelegatePreview(
                accountId = accountId,
                stats = stats?.let { mapStatsToDomain(it) } ?: emptyDelegateStats(),
                metadata = mapMetadataToDomain(metadata),
                onChainIdentity = identity,
                userDelegations = userDelegations[accountId]?.toMap().orEmpty()
            )
        }
    }

    private fun emptyDelegateStats(): DelegatePreview.Stats {
        return DelegatePreview.Stats(0, 0.toBigInteger(), 0)
    }

    private fun uniteStatsAndMetadata(
        delegateStats: List<DelegateStats>,
        delegateMetadata: AccountIdKeyMap<DelegateMetadata>,
    ): Map<AccountIdKey, Pair<DelegateMetadata?, DelegateStats?>> {
        val result = mutableMapOf<AccountIdKey, Pair<DelegateMetadata?, DelegateStats?>>()
        delegateStats.map {
            val metadata = delegateMetadata[it.accountId]
            result[it.accountId.intoKey()] = Pair(metadata, it)
        }
        delegateMetadata.map {
            if (!result.containsKey(it.key)) {
                result[it.key] = Pair(it.value, null)
            }
        }
        return result
    }
}

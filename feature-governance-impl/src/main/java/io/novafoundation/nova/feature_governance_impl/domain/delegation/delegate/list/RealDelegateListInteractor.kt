package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.list

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.get
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.applyFilter
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.getIdOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateMetadata
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateStats
import io.novafoundation.nova.feature_governance_api.data.repository.ConvictionVotingRepository
import io.novafoundation.nova.feature_governance_api.data.repository.OnChainReferendaRepository
import io.novafoundation.nova.feature_governance_api.data.repository.getDelegatesMetadataOrEmpty
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.DelegateListInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegateFiltering
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegatePreview
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegateSorting
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.delegateComparator
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.hasMetadata
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.hasUserDelegations
import io.novafoundation.nova.feature_governance_api.domain.track.Track
import io.novafoundation.nova.feature_governance_impl.data.repository.DelegationBannerRepository
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.RECENT_VOTES_PERIOD
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.mapAccountTypeToDomain
import io.novafoundation.nova.feature_governance_impl.domain.track.mapTrackInfoToTrack
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.blockDurationEstimator
import io.novafoundation.nova.runtime.util.blockInPast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

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
    ): Result<List<DelegatePreview>> = withContext(Dispatchers.Default) {
        runCatching {
            getDelegatesInternal(governanceOption)
        }
    }

    override suspend fun getDelegatedDelegates(governanceOption: SupportedGovernanceOption): Result<List<DelegatePreview>> {
        return withContext(Dispatchers.Default) {
            runCatching {
                getDelegatesInternal(governanceOption)
                    .filter { it.hasUserDelegations() }
            }
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

    private suspend fun getDelegatesInternal(
        governanceOption: SupportedGovernanceOption,
    ): List<DelegatePreview> = coroutineScope {
        val chain = governanceOption.assetWithChain.chain

        val governanceSource = governanceSourceRegistry.sourceFor(governanceOption)
        val convictionVotingRepository = governanceSource.convictionVoting
        val delegationsRepository = governanceSource.delegationsRepository
        val referendaRepository = governanceSource.referenda

        val blockDurationEstimator = chainStateRepository.blockDurationEstimator(chain.id)
        val recentVotesBlockThreshold = blockDurationEstimator.blockInPast(RECENT_VOTES_PERIOD)

        val delegatesStatsDeferred = async { delegationsRepository.getDelegatesStats(recentVotesBlockThreshold, chain) }
        val delegateMetadatasDeferred = async { delegationsRepository.getDelegatesMetadataOrEmpty(chain) }

        val delegateAccountIds = delegatesStatsDeferred.await().map(DelegateStats::accountId)
        val delegateMetadatasByAccountId = delegateMetadatasDeferred.await().associateBy { AccountIdKey(it.accountId) }

        val identities = identityRepository.getIdentitiesFromIds(delegateAccountIds, chain.id)

        val userDelegations = getUserDelegationsOrEmpty(chain, convictionVotingRepository, referendaRepository)

        val delegates = delegatesStatsDeferred.await().map { delegateStats ->
            val metadata = delegateMetadatasByAccountId[delegateStats.accountId]
            val identity = identities[delegateStats.accountId]

            DelegatePreview(
                accountId = delegateStats.accountId,
                stats = mapStatsToDomain(delegateStats),
                metadata = mapMetadataToDomain(metadata),
                onChainIdentity = identity,
                userDelegations = userDelegations[delegateStats.accountId]?.toMap().orEmpty()
            )
        }

        delegates
    }

    private suspend fun getUserDelegationsOrEmpty(
        chain: Chain,
        convictionVotingRepository: ConvictionVotingRepository,
        referendaRepository: OnChainReferendaRepository
    ): Map<AccountIdKey, List<Pair<Track, Voting.Delegating>>> {
        val accountId = accountRepository.getIdOfSelectedMetaAccountIn(chain) ?: return emptyMap()

        val tracks = referendaRepository.getTracks(chain.id)
            .map { mapTrackInfoToTrack(it) }
            .associateBy { it.id }
        return convictionVotingRepository.delegatingFor(accountId, chain.id)
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
}

package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.repository

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.feature_account_api.data.model.AccountIdKeyMap
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.getIdOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateMetadata
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateStats
import io.novafoundation.nova.feature_governance_api.data.repository.getDelegatesMetadataOrEmpty
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.track.Track
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.RECENT_VOTES_PERIOD
import io.novafoundation.nova.feature_governance_impl.domain.track.mapTrackInfoToTrack
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.blockDurationEstimator
import io.novafoundation.nova.runtime.util.blockInPast
import io.novasama.substrate_sdk_android.runtime.AccountId

interface DelegateCommonRepository {
    suspend fun getDelegatesStats(governanceOption: SupportedGovernanceOption, accountIds: List<AccountId>? = null): List<DelegateStats>

    suspend fun getMetadata(governanceOption: SupportedGovernanceOption): AccountIdKeyMap<DelegateMetadata>

    suspend fun getTracks(governanceOption: SupportedGovernanceOption): Map<TrackId, Track>

    suspend fun getUserDelegationsOrEmpty(
        governanceOption: SupportedGovernanceOption,
        tracks: Map<TrackId, Track>
    ): AccountIdKeyMap<List<Pair<Track, Voting.Delegating>>>
}

class RealDelegateCommonRepository(
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val accountRepository: AccountRepository,
    private val chainStateRepository: ChainStateRepository,
) : DelegateCommonRepository {

    override suspend fun getDelegatesStats(
        governanceOption: SupportedGovernanceOption,
        accountIds: List<AccountId>?
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

    override suspend fun getMetadata(governanceOption: SupportedGovernanceOption): AccountIdKeyMap<DelegateMetadata> {
        val chain = governanceOption.assetWithChain.chain
        val delegationsRepository = governanceSourceRegistry.sourceFor(governanceOption)
            .delegationsRepository
        return delegationsRepository.getDelegatesMetadataOrEmpty(chain)
            .associateBy { AccountIdKey(it.accountId) }
    }

    override suspend fun getTracks(governanceOption: SupportedGovernanceOption): Map<TrackId, Track> {
        val chain = governanceOption.assetWithChain.chain
        val referendaRepository = governanceSourceRegistry.sourceFor(governanceOption).referenda
        return referendaRepository.getTracks(chain.id)
            .map { mapTrackInfoToTrack(it) }
            .associateBy { it.id }
    }

    override suspend fun getUserDelegationsOrEmpty(
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
}

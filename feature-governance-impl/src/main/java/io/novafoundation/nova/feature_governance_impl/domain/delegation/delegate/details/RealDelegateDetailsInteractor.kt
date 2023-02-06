package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.details

import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateDetailedStats
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateMetadata
import io.novafoundation.nova.feature_governance_api.data.repository.getDelegateMetadataOrNull
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model.DelegateDetails
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model.DelegateDetailsInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.RECENT_VOTES_PERIOD
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.mapAccountTypeToDomain
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.blockDurationEstimator
import io.novafoundation.nova.runtime.state.selectedOption
import io.novafoundation.nova.runtime.util.blockInPast
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class RealDelegateDetailsInteractor(
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val chainStateRepository: ChainStateRepository,
    private val identityRepository: OnChainIdentityRepository,
    private val governanceSharedState: GovernanceSharedState,
) : DelegateDetailsInteractor {

    override suspend fun getDelegateDetails(
        delegateAccountId: AccountId,
    ): Result<DelegateDetails> {
        return withContext(Dispatchers.Default) {
            runCatching {
                getDelegateDetailsInternal(delegateAccountId)
            }
        }
    }

    private suspend fun getDelegateDetailsInternal(
        delegateAccountId: AccountId,
    ): DelegateDetails = coroutineScope {
        val governanceOption = governanceSharedState.selectedOption()

        val chain = governanceOption.assetWithChain.chain
        val delegateAddress = chain.addressOf(delegateAccountId)

        val governanceSource = governanceSourceRegistry.sourceFor(governanceOption)
        val delegationsRepository = governanceSource.delegationsRepository

        val blockDurationEstimator = chainStateRepository.blockDurationEstimator(chain.id)
        val recentVotesBlockThreshold = blockDurationEstimator.blockInPast(RECENT_VOTES_PERIOD)

        val delegatesStatsDeferred = async { delegationsRepository.getDetailedDelegateStats(delegateAddress, recentVotesBlockThreshold, chain) }
        val delegateMetadatasDeferred = async { delegationsRepository.getDelegateMetadataOrNull(chain, delegateAccountId) }
        val identity = async { identityRepository.getIdentityFromId(chain.id, delegateAccountId) }

        DelegateDetails(
            accountId = delegateAccountId,
            stats = delegatesStatsDeferred.await()?.let(::mapStatsToDomain),
            metadata = delegateMetadatasDeferred.await()?.let(::mapMetadataToDomain),
            onChainIdentity = identity.await()
        )
    }

    private fun mapStatsToDomain(detailedStats: DelegateDetailedStats): DelegateDetails.Stats {
        return DelegateDetails.Stats(
            delegationsCount = detailedStats.delegationsCount,
            delegatedVotes = detailedStats.delegatedVotes,
            recentVotes = detailedStats.recentVotes,
            allVotes = detailedStats.allVotes
        )
    }

    private fun mapMetadataToDomain(metadata: DelegateMetadata): DelegateDetails.Metadata {
        return DelegateDetails.Metadata(
            shortDescription = metadata.shortDescription,
            longDescription = metadata.longDescription,
            accountType = mapAccountTypeToDomain(metadata.isOrganization),
            iconUrl = metadata.profileImageUrl,
            name = metadata.name
        )
    }
}

package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.details

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.getIdOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_account_api.domain.validation.hasChainAccount
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateDetailedStats
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateMetadata
import io.novafoundation.nova.feature_governance_api.data.repository.ConvictionVotingRepository
import io.novafoundation.nova.feature_governance_api.data.repository.getDelegateMetadataOrNull
import io.novafoundation.nova.feature_governance_api.data.repository.getTracksById
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model.AddDelegationValidationFailure.NoChainAccountFailure
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model.AddDelegationValidationSystem
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model.DelegateDetails
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model.DelegateDetailsInteractor
import io.novafoundation.nova.feature_governance_api.domain.track.matchWith
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.RECENT_VOTES_PERIOD
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.mapAccountTypeToDomain
import io.novafoundation.nova.feature_governance_impl.domain.track.TracksUseCase
import io.novafoundation.nova.feature_governance_impl.domain.track.mapTrackInfoToTrack
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.timelineChainIdOrSelf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.blockDurationEstimator
import io.novafoundation.nova.runtime.state.selectedOption
import io.novafoundation.nova.runtime.util.blockInPast
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RealDelegateDetailsInteractor(
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val chainStateRepository: ChainStateRepository,
    private val identityRepository: OnChainIdentityRepository,
    private val governanceSharedState: GovernanceSharedState,
    private val accountRepository: AccountRepository,
    private val tracksUseCase: TracksUseCase,
) : DelegateDetailsInteractor {

    override fun delegateDetailsFlow(delegateAccountId: AccountId): Flow<DelegateDetails> {
        return flowOfAll {
            delegateDetailsFlowInternal(delegateAccountId)
        }
    }

    override fun validationSystemFor(): AddDelegationValidationSystem = ValidationSystem {
        hasChainAccount(
            chain = { it.chain },
            metaAccount = { it.metaAccount },
            error = ::NoChainAccountFailure
        )
    }

    private suspend fun delegateDetailsFlowInternal(
        delegateAccountId: AccountId,
    ): Flow<DelegateDetails> {
        val governanceOption = governanceSharedState.selectedOption()

        val chain = governanceOption.assetWithChain.chain
        val delegateAddress = chain.addressOf(delegateAccountId)

        val governanceSource = governanceSourceRegistry.sourceFor(governanceOption)
        val delegationsRepository = governanceSource.delegationsRepository

        val userAccountId = accountRepository.getIdOfSelectedMetaAccountIn(chain)

        val tracks = governanceSource.referenda.getTracksById(chain.id)
            .mapValues { (_, trackInfo) -> mapTrackInfoToTrack(trackInfo) }

        val (metadata, identity) = coroutineScope {
            val delegateMetadatasDeferred = async { delegationsRepository.getDelegateMetadataOrNull(chain, delegateAccountId) }
            val identity = async { identityRepository.getIdentityFromId(chain.id, delegateAccountId) }

            delegateMetadatasDeferred.await() to identity.await()
        }

        return chainStateRepository.currentBlockNumberFlow(chain.timelineChainIdOrSelf()).map {
            coroutineScope {
                val blockDurationEstimator = chainStateRepository.blockDurationEstimator(chain.timelineChainIdOrSelf())
                val recentVotesBlockThreshold = blockDurationEstimator.blockInPast(RECENT_VOTES_PERIOD)

                val delegatesStatsDeferred = async {
                    delegationsRepository.getDetailedDelegateStats(delegateAddress, recentVotesBlockThreshold, chain)
                }
                val delegationsDeferred = async {
                    userAccountId?.let { governanceSource.convictionVoting.delegationsOf(it, delegateAccountId, chain.id) }
                        .orEmpty()
                        .matchWith(tracks)
                }

                DelegateDetails(
                    accountId = delegateAccountId,
                    stats = delegatesStatsDeferred.await()?.let(::mapStatsToDomain),
                    metadata = metadata?.let(::mapMetadataToDomain),
                    onChainIdentity = identity,
                    userDelegations = delegationsDeferred.await()
                )
            }
        }
    }

    private suspend fun ConvictionVotingRepository.delegationsOf(
        userAccountId: AccountId,
        delegate: AccountId,
        chainId: ChainId
    ): Map<TrackId, Voting.Delegating> {
        return delegatingFor(userAccountId, chainId)
            .filterValues { it.target.contentEquals(delegate) }
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

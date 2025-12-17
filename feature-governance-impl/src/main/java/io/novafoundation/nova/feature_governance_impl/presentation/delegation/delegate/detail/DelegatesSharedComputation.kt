package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegatePreview
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.mapDelegateStatsToPreviews
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.repository.DelegateCommonRepository
import io.novafoundation.nova.runtime.ext.timelineChainIdOrSelf
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DelegatesSharedComputation(
    private val computationalCache: ComputationalCache,
    private val delegateCommonRepository: DelegateCommonRepository,
    private val chainStateRepository: ChainStateRepository,
    private val identityRepository: OnChainIdentityRepository
) {

    suspend fun delegates(
        governanceOption: SupportedGovernanceOption,
        scope: CoroutineScope
    ): Flow<List<DelegatePreview>> {
        val chainId = governanceOption.assetWithChain.chain.id
        val key = "DELEGATES:$chainId"

        return computationalCache.useSharedFlow(key, scope) {
            val chain = governanceOption.assetWithChain.chain
            val delegateMetadataDeferred = scope.async { delegateCommonRepository.getMetadata(governanceOption) }
            val delegatesStatsDeferred = scope.async { delegateCommonRepository.getDelegatesStats(governanceOption) }
            val tracksDeferred = scope.async { delegateCommonRepository.getTracks(governanceOption) }

            chainStateRepository.currentBlockNumberFlow(chain.timelineChainIdOrSelf()).map {
                val userDelegates = delegateCommonRepository.getUserDelegationsOrEmpty(governanceOption, tracksDeferred.await())
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
    }
}

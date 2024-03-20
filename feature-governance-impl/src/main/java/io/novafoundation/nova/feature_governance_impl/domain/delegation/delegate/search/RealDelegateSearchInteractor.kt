package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.search

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.emitError
import io.novafoundation.nova.common.domain.emitLoaded
import io.novafoundation.nova.common.domain.emitLoading
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceAdditionalState
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegatePreview
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.search.DelegateSearchInteractor
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.repository.DelegateCommonRepository
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.DelegatesSharedComputation
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.isValidAddress
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.onStart

class RealDelegateSearchInteractor(
    private val identityRepository: OnChainIdentityRepository,
    private val delegateCommonRepository: DelegateCommonRepository,
    private val delegatesSharedComputation: DelegatesSharedComputation
) : DelegateSearchInteractor {

    override suspend fun searchDelegates(
        queryFlow: Flow<String>,
        selectedOption: SelectedAssetOptionSharedState.SupportedAssetOption<GovernanceAdditionalState>,
        scope: CoroutineScope
    ): Flow<ExtendedLoadingState<List<DelegatePreview>>> {
        val chain = selectedOption.assetWithChain.chain
        return combineTransform(queryFlow, delegatesSharedComputation.delegates(selectedOption, scope)) { query, delegates ->
            if (query.isEmpty()) {
                emitLoaded(emptyList<DelegatePreview>())
                return@combineTransform
            }

            var searchResult = filterDelegates(chain, query, delegates)

            if (searchResult.isEmpty() && chain.isValidAddress(query)) {
                emitLoading<List<DelegatePreview>>()
                val searchedAccountId = chain.accountIdOf(query)
                searchResult = listOf(loadDelegateById(chain, searchedAccountId))
            }

            emitLoaded(searchResult)
        }.onStart { emitLoading() }
            .catch { emitError(it) }
    }

    private fun filterDelegates(chain: Chain, query: String, delegates: List<DelegatePreview>): List<DelegatePreview> {
        val lowercaseQuery = query.lowercase()

        return delegates.filter {
            val metadataName = it.metadata?.name?.lowercase().orEmpty()
            val identityName = it.onChainIdentity?.display?.lowercase().orEmpty()
            lowercaseQuery in metadataName ||
                lowercaseQuery in identityName ||
                chain.addressOf(it.accountId).startsWith(query)
        }
    }

    private suspend fun loadDelegateById(chain: Chain, accountId: AccountId): DelegatePreview {
        val identity = identityRepository.getIdentityFromId(chain.id, accountId)

        return DelegatePreview(
            accountId = accountId,
            stats = null,
            metadata = null,
            onChainIdentity = identity,
            userDelegations = emptyMap()
        )
    }
}

package io.novafoundation.nova.feature_governance_impl.domain.referendum.list

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.common.utils.withLoadingShared
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.Voter
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.repository.ReferendaCommonRepository
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.repository.ReferendaState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class ReferendaSharedComputation(
    private val computationalCache: ComputationalCache,
    private val referendaCommonRepository: ReferendaCommonRepository
) {

    suspend fun referenda(
        voter: Voter?,
        governanceOption: SupportedGovernanceOption,
        scope: CoroutineScope
    ): Flow<ExtendedLoadingState<ReferendaState>> {
        val chainId = governanceOption.assetWithChain.chain.id
        val key = "REFERENDA:$chainId"

        return computationalCache.useSharedFlow(key, scope) {
            referendaCommonRepository.referendaStateFlow(voter, governanceOption)
        }
    }
}

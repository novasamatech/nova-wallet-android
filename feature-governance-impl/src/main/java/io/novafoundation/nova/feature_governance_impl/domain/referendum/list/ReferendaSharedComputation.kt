package io.novafoundation.nova.feature_governance_impl.domain.referendum.list

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.Voter
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.repository.ReferendaCommonRepository
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class ReferendaSharedComputation(
    private val computationalCache: ComputationalCache,
    private val referendaCommonRepository: ReferendaCommonRepository
) {

    suspend fun referenda(
        metaAccount: MetaAccount,
        voter: Voter?,
        governanceOption: SupportedGovernanceOption,
        scope: CoroutineScope
    ): Flow<ExtendedLoadingState<ReferendaState>> {
        val metaId = metaAccount.id
        val chainId = governanceOption.assetWithChain.chain.id
        val assetId = governanceOption.assetWithChain.asset.id
        val version = governanceOption.additional.governanceType.name
        val key = "REFERENDA:$metaId:$chainId:$assetId:$version"

        return computationalCache.useSharedFlow(key, scope) {
            referendaCommonRepository.referendaStateFlow(voter, governanceOption)
        }
    }
}

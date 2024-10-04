package io.novafoundation.nova.feature_governance_impl.domain.summary

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.ReferendumDetailsRepository
import kotlinx.coroutines.CoroutineScope

class ReferendaSummarySharedComputation(
    private val computationalCache: ComputationalCache,
    private val referendumDetailsRepository: ReferendumDetailsRepository,
    private val accountRepository: AccountRepository
) {

    suspend fun summaries(
        governanceOption: SupportedGovernanceOption,
        referendaIds: List<ReferendumId>,
        scope: CoroutineScope
    ): Map<ReferendumId, String> {
        val chainId = governanceOption.assetWithChain.chain.id
        val referendaSet = referendaIds.toSet()
        val selectedLanguage = accountRepository.selectedLanguage()
        val key = "REFERENDA_SUMMARIES:$chainId:$referendaSet:${selectedLanguage.iso639Code}"

        return computationalCache.useCache(key, scope) {
            referendumDetailsRepository.loadSummaries(governanceOption.assetWithChain.chain, referendaIds, selectedLanguage)
        }
    }
}

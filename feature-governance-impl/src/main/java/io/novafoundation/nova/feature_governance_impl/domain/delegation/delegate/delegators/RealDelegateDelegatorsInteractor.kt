package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.delegators

import io.novafoundation.nova.common.address.get
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Delegation
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegators.DelegateDelegatorsInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegators.model.Delegator
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.runtime.state.selectedOption
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow

class RealDelegateDelegatorsInteractor(
    private val identityRepository: OnChainIdentityRepository,
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val governanceSharedState: GovernanceSharedState,
): DelegateDelegatorsInteractor {

    override fun delegatorsFlow(delegateId: AccountId): Flow<List<Delegator>> {
        return flowOf { delegatorsOf(delegateId) }
    }

    private suspend fun delegatorsOf(delegateId: AccountId): List<Delegator> {
        val governanceOption = governanceSharedState.selectedOption()
        val chain = governanceOption.assetWithChain.chain

        val delegationRepository = governanceSourceRegistry.sourceFor(governanceOption).delegationsRepository
        val delegations = delegationRepository.getDelegationsTo(delegateId, chain)

        val delegatorIds = delegations.map(Delegation::delegator)
        val identities = identityRepository.getIdentitiesFromIds(delegatorIds, chain.id)

        return delegations.map {
            val delegatorAccountId = it.delegator

            Delegator(
                accountId = delegatorAccountId,
                identity = identities[delegatorAccountId],
                delegatedVote = it.vote
            )
        }
    }
}

package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.delegators

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Delegation
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegators.DelegateDelegatorsInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegators.model.Delegator
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.runtime.state.selectedOption
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow

class RealDelegateDelegatorsInteractor(
    private val identityRepository: OnChainIdentityRepository,
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val governanceSharedState: GovernanceSharedState,
) : DelegateDelegatorsInteractor {

    override fun delegatorsFlow(delegateId: AccountId): Flow<List<Delegator>> {
        return flowOf { delegatorsOf(delegateId) }
    }

    private suspend fun delegatorsOf(delegateId: AccountId): List<Delegator> {
        val governanceOption = governanceSharedState.selectedOption()
        val chain = governanceOption.assetWithChain.chain
        val chainAsset = governanceOption.assetWithChain.asset

        val delegationRepository = governanceSourceRegistry.sourceFor(governanceOption).delegationsRepository
        val delegations = delegationRepository.getDelegationsTo(delegateId, chain)

        val delegatorIds = delegations.map(Delegation::delegator)
        val identities = identityRepository.getIdentitiesFromIds(delegatorIds, chain.id)

        return delegations.groupBy { it.delegator.intoKey() }
            .map { (accountIdKey, delegations) ->
                Delegator(
                    accountId = accountIdKey.value,
                    identity = identities[accountIdKey]?.let(::Identity),
                    delegatorTrackDelegations = delegations.map { it.vote },
                    chainAsset = chainAsset
                )
            }.sortedByDescending { it.vote.totalVotes }
    }
}

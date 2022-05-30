package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository

import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.ScheduledDelegationRequest
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindDelegationRequests
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindDelegatorState
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow

interface DelegatorStateRepository {

    suspend fun scheduledDelegationRequests(delegatorState: DelegatorState.Delegator): List<ScheduledDelegationRequest>

    suspend fun scheduledDelegationRequest(delegatorState: DelegatorState.Delegator, collatorId: AccountId): ScheduledDelegationRequest?

    fun observeDelegatorState(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
    ): Flow<DelegatorState>

    suspend fun getDelegationState(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId
    ): DelegatorState
}

class RealDelegatorStateRepository(
    private val localStorage: StorageDataSource,
    private val remoteStorage: StorageDataSource,
) : DelegatorStateRepository {

    override suspend fun scheduledDelegationRequests(delegatorState: DelegatorState.Delegator): List<ScheduledDelegationRequest> {
        return remoteStorage.query(delegatorState.chain.id) {
            val keyArguments = delegatorState.delegations.map { listOf(it.owner) }

            val delegationRequestsByCollator = runtime.metadata.parachainStaking().storage("DelegationScheduledRequests").entries(
                keyArguments,
                keyExtractor = { (collatorId: AccountId) -> collatorId.toHexString() },
                binding = { dynamicInstance, _ -> bindDelegationRequests(dynamicInstance) }
            )

            delegatorState.delegations.mapNotNull { delegation ->
                val collatorDelegationRequests = delegationRequestsByCollator[delegation.owner.toHexString()]

                collatorDelegationRequests?.find { it.delegator.contentEquals(delegatorState.accountId) }
            }
        }
    }

    override suspend fun scheduledDelegationRequest(delegatorState: DelegatorState.Delegator, collatorId: AccountId): ScheduledDelegationRequest? {
        return remoteStorage.query(delegatorState.chain.id) {
            val allCollatorDelegationRequests = runtime.metadata.parachainStaking().storage("DelegationScheduledRequests").query(
                collatorId,
                binding = ::bindDelegationRequests
            )

            allCollatorDelegationRequests.find { it.delegator.contentEquals(delegatorState.accountId) }
        }
    }

    override fun observeDelegatorState(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): Flow<DelegatorState> {
        return localStorage.subscribe(chain.id) {
            runtime.metadata.parachainStaking().storage("DelegatorState").observe(
                accountId,
                binding = { bindDelegatorState(it, accountId, chain) }
            )
        }
    }

    override suspend fun getDelegationState(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): DelegatorState {
        return localStorage.query(chain.id) {
            runtime.metadata.parachainStaking().storage("DelegatorState").query(
                accountId,
                binding = { bindDelegatorState(it, accountId, chain) }
            )
        }
    }
}

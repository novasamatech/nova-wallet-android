package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository

import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.mapValuesNotNull
import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.ScheduledDelegationRequest
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.hasDelegation
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindDelegationRequests
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindDelegatorState
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface DelegatorStateRepository {

    /**
     * Returns mapping from collator id to scheduled delegation request
     */
    suspend fun scheduledDelegationRequests(delegatorState: DelegatorState.Delegator): AccountIdMap<ScheduledDelegationRequest>

    fun scheduledDelegationRequestsFlow(delegatorState: DelegatorState.Delegator): Flow<Collection<ScheduledDelegationRequest>>

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

    override suspend fun scheduledDelegationRequests(delegatorState: DelegatorState.Delegator): AccountIdMap<ScheduledDelegationRequest> {
        return localStorage.query(delegatorState.chain.id) {
            val keyArguments = delegatorState.delegations.map { listOf(it.owner) }

            val delegationRequestsByCollator = runtime.metadata.parachainStaking().storage("DelegationScheduledRequests").entries(
                keyArguments,
                keyExtractor = { (collatorId: AccountId) -> collatorId.toHexString() },
                binding = { dynamicInstance, collatorId -> bindDelegationRequests(dynamicInstance, collatorId.fromHex()) }
            )

            delegationRequestsByCollator.byDelegator(delegatorState.accountId)
        }
    }

    override fun scheduledDelegationRequestsFlow(delegatorState: DelegatorState.Delegator): Flow<Collection<ScheduledDelegationRequest>> {
        return localStorage.subscribe(delegatorState.chain.id) {
            val keyArguments = delegatorState.delegations.map { listOf(it.owner) }

            runtime.metadata.parachainStaking().storage("DelegationScheduledRequests").observe(
                keyArguments,
                keyExtractor = { (collatorId: AccountId) -> collatorId.toHexString() },
                binding = { dynamicInstance, collatorId -> bindDelegationRequests(dynamicInstance, collatorId.fromHex()) }
            )
        }
            .map { delegationRequestsByCollator -> delegationRequestsByCollator.filterNotNull().byDelegator(delegatorState.accountId).values }
    }

    override suspend fun scheduledDelegationRequest(delegatorState: DelegatorState.Delegator, collatorId: AccountId): ScheduledDelegationRequest? {
        if (!delegatorState.hasDelegation(collatorId)) return null

        return localStorage.query(delegatorState.chain.id) {
            val allCollatorDelegationRequests = runtime.metadata.parachainStaking().storage("DelegationScheduledRequests").query(
                collatorId,
                binding = { bindDelegationRequests(it, collatorId) }
            )

            allCollatorDelegationRequests.find { it.delegator.contentEquals(delegatorState.accountId) }
        }
    }

    override fun observeDelegatorState(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): Flow<DelegatorState> {
        return localStorage.subscribe(chain.id) {
            runtime.metadata.parachainStaking().storage("DelegatorState").observe(
                accountId,
                binding = { bindDelegatorState(it, accountId, chain, chainAsset) }
            )
        }
    }

    override suspend fun getDelegationState(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): DelegatorState {
        return localStorage.query(chain.id) {
            runtime.metadata.parachainStaking().storage("DelegatorState").query(
                accountId,
                binding = { bindDelegatorState(it, accountId, chain, chainAsset) }
            )
        }
    }

    private fun Map<String, List<ScheduledDelegationRequest>>.byDelegator(delegator: AccountId) = mapValuesNotNull { (_, pendingRequests) ->
        pendingRequests.find { it.delegator.contentEquals(delegator) }
    }
}

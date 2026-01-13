package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.scheduledRequests

import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.utils.mapValuesNotNull
import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.ScheduledDelegationRequest
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindDelegationAction
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindRoundIndex
import io.novafoundation.nova.runtime.storage.source.StorageEntries
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.firstOrNull
import kotlin.collections.mapKeys
import kotlin.collections.orEmpty

private class RequestKey(val collatorId: String, val delegatorId: String)

class NewDelegationScheduledRequestExecutor : DelegationScheduledRequestExecutor {
    context(StorageQueryContext)
    override suspend fun entries(delegatorState: DelegatorState.Delegator): Map<String, ScheduledDelegationRequest> {
        val keyArguments = delegatorState.delegations.map { listOf(it.owner, delegatorState.accountId) }

        return runtime.metadata.parachainStaking().storage("DelegationScheduledRequests").entries(
            keyArguments,
            keyExtractor = { (collatorId: AccountId, delegatorId: AccountId) -> RequestKey(collatorId.toHexString(), delegatorId.toHexString()) },
            binding = { dynamicInstance, key ->
                bindDelegationRequests(
                    instance = dynamicInstance,
                    collatorId = key.collatorId.fromHex(),
                    delegatorId = key.delegatorId.fromHex()
                )
            }
        ).mapKeys { (key, _) -> key.collatorId }
            .mapValuesNotNull { it.value.firstOrNull() }
    }

    context(StorageQueryContext)
    override suspend fun observe(delegatorState: DelegatorState.Delegator): Flow<Collection<ScheduledDelegationRequest>> {
        val keyArguments = delegatorState.delegations.map { listOf(it.owner, delegatorState.accountId) }

        return runtime.metadata.parachainStaking().storage("DelegationScheduledRequests").observe(
            keyArguments,
            keyExtractor = { (collatorId: AccountId, delegatorId: AccountId) -> RequestKey(collatorId.toHexString(), delegatorId.toHexString()) },
            binding = { dynamicInstance, key ->
                bindDelegationRequests(
                    dynamicInstance,
                    collatorId = key.collatorId.fromHex(),
                    delegatorId = key.delegatorId.fromHex()
                )
            }
        ).mapNotNull { instances -> instances.values.flatMap { it.orEmpty() } }
    }

    context(StorageQueryContext)
    override suspend fun query(delegatorState: DelegatorState.Delegator, collatorId: AccountId): ScheduledDelegationRequest? {
        return runtime.metadata.parachainStaking().storage("DelegationScheduledRequests").query(
            collatorId,
            delegatorState.accountId,
            binding = { bindDelegationRequests(it, collatorId, delegatorState.accountId) }
        ).firstOrNull()
    }

    context(StorageQueryContext)
    override suspend fun entriesRaw(delegatorState: DelegatorState.Delegator): StorageEntries {
        val delegatorIdsArgs = delegatorState.delegations.map { listOf(it.owner, delegatorState.accountId) }

        return runtime.metadata.parachainStaking().storage("DelegationScheduledRequests").entriesRaw(delegatorIdsArgs)
    }

    fun bindDelegationRequests(
        instance: Any?,
        collatorId: AccountId,
        delegatorId: AccountId,
    ) = instance?.let {
        bindList(instance) { listElement -> bindDelegationRequest(collatorId, delegatorId, listElement) }
    }.orEmpty()

    private fun bindDelegationRequest(
        collatorId: AccountId,
        delegatorId: AccountId,
        instance: Any?,
    ): ScheduledDelegationRequest {
        val delegationRequestStruct = instance.castToStruct()

        return ScheduledDelegationRequest(
            delegator = delegatorId,
            whenExecutable = bindRoundIndex(delegationRequestStruct["whenExecutable"]),
            action = bindDelegationAction(delegationRequestStruct.getTyped("action")),
            collator = collatorId
        )
    }
}

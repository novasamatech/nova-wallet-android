package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.scheduledRequests

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.mapValuesNotNull
import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.ScheduledDelegationRequest
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindDelegationAction
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindRoundIndex
import io.novafoundation.nova.runtime.storage.source.StorageEntries
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.wrapSingleArgumentKeys
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.find
import kotlin.collections.orEmpty

class LegacyDelegationScheduledRequestExecutor : DelegationScheduledRequestExecutor {
    context(StorageQueryContext)
    override suspend fun entries(delegatorState: DelegatorState.Delegator): Map<String, ScheduledDelegationRequest> {
        val keyArguments = delegatorState.delegations.map { listOf(it.owner) }

        return runtime.metadata.parachainStaking().storage("DelegationScheduledRequests").entries(
            keyArguments,
            keyExtractor = { (collatorId: AccountId) -> collatorId.toHexString() },
            binding = { dynamicInstance, collatorId -> bindDelegationRequests(dynamicInstance, collatorId.fromHex()) }
        ).byDelegator(delegatorState.accountId)
    }

    context(StorageQueryContext)
    override suspend fun observe(delegatorState: DelegatorState.Delegator): Flow<Collection<ScheduledDelegationRequest>> {
        val keyArguments = delegatorState.delegations.map { listOf(it.owner) }

        return runtime.metadata.parachainStaking().storage("DelegationScheduledRequests").observe(
            keyArguments,
            keyExtractor = { (collatorId: AccountId) -> collatorId.toHexString() },
            binding = { dynamicInstance, collatorId -> bindDelegationRequests(dynamicInstance, collatorId.fromHex()) }
        ).map { it.filterNotNull().byDelegator(delegatorState.accountId).values }
    }

    context(StorageQueryContext)
    override suspend fun query(
        delegatorState: DelegatorState.Delegator,
        collatorId: AccountId
    ): ScheduledDelegationRequest? {
        val allCollatorDelegationRequests = runtime.metadata.parachainStaking().storage("DelegationScheduledRequests").query(
            collatorId,
            binding = { bindDelegationRequests(it, collatorId) }
        )

        return allCollatorDelegationRequests.find { it.delegator.contentEquals(delegatorState.accountId) }
    }

    context(StorageQueryContext)
    override suspend fun entriesRaw(delegatorState: DelegatorState.Delegator): StorageEntries {
        val delegatorIdsArgs = delegatorState.delegations.map { it.owner }.wrapSingleArgumentKeys()

        return runtime.metadata.parachainStaking().storage("DelegationScheduledRequests").entriesRaw(delegatorIdsArgs)
    }

    private fun bindDelegationRequests(instance: Any?, collatorId: AccountId) = instance?.let {
        bindList(instance) { listElement -> bindDelegationRequest(collatorId, listElement) }
    }.orEmpty()

    private fun bindDelegationRequest(
        collatorId: AccountId,
        instance: Any?,
    ): ScheduledDelegationRequest {
        val delegationRequestStruct = instance.castToStruct()

        return ScheduledDelegationRequest(
            delegator = bindAccountId(delegationRequestStruct["delegator"]),
            whenExecutable = bindRoundIndex(delegationRequestStruct["whenExecutable"]),
            action = bindDelegationAction(delegationRequestStruct.getTyped("action")),
            collator = collatorId
        )
    }

    private fun Map<String, List<ScheduledDelegationRequest>>.byDelegator(delegator: AccountId) = mapValuesNotNull { (_, pendingRequests) ->
        pendingRequests.find { it.delegator.contentEquals(delegator) }
    }
}

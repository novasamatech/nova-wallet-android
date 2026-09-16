package io.novafoundation.nova.feature_staking_impl.data.parachainAvnStaking.repository.scheduledRequests

import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.utils.mapValuesNotNull
import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.ScheduledDelegationRequest
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindDelegationAction
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindRoundIndex
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.scheduledRequests.DelegationScheduledRequestExecutor
import io.novafoundation.nova.runtime.storage.source.StorageEntries
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

private class RequestKey(val collatorId: String, val nominatorId: String)

class AvnNewNominationScheduledRequestExecutor : DelegationScheduledRequestExecutor {

    context(StorageQueryContext)
    override suspend fun entries(delegatorState: DelegatorState.Delegator): Map<String, ScheduledDelegationRequest> {
        val keyArguments = delegatorState.delegations.map { listOf(it.owner, delegatorState.accountId) }

        return runtime.metadata.parachainStaking().storage("NominationScheduledRequests").entries(
            keyArguments,
            keyExtractor = { (collatorId: AccountId, nominatorId: AccountId) ->
                RequestKey(collatorId.toHexString(), nominatorId.toHexString())
            },
            binding = { dynamicInstance, key ->
                bindNominationRequests(
                    instance = dynamicInstance,
                    collatorId = key.collatorId.fromHex(),
                    nominatorId = key.nominatorId.fromHex()
                )
            }
        ).mapKeys { (key, _) -> key.collatorId }
            .mapValuesNotNull { it.value.firstOrNull() }
    }

    context(StorageQueryContext)
    override suspend fun observe(delegatorState: DelegatorState.Delegator): Flow<Collection<ScheduledDelegationRequest>> {
        val keyArguments = delegatorState.delegations.map { listOf(it.owner, delegatorState.accountId) }

        return runtime.metadata.parachainStaking().storage("NominationScheduledRequests").observe(
            keyArguments,
            keyExtractor = { (collatorId: AccountId, nominatorId: AccountId) ->
                RequestKey(collatorId.toHexString(), nominatorId.toHexString())
            },
            binding = { dynamicInstance, key ->
                bindNominationRequests(
                    dynamicInstance,
                    collatorId = key.collatorId.fromHex(),
                    nominatorId = key.nominatorId.fromHex()
                )
            }
        ).mapNotNull { instances -> instances.values.flatMap { it.orEmpty() } }
    }

    context(StorageQueryContext)
    override suspend fun query(delegatorState: DelegatorState.Delegator, collatorId: AccountId): ScheduledDelegationRequest? {
        return runtime.metadata.parachainStaking().storage("NominationScheduledRequests").query(
            collatorId,
            delegatorState.accountId,
            binding = { bindNominationRequests(it, collatorId, delegatorState.accountId) }
        ).firstOrNull()
    }

    context(StorageQueryContext)
    override suspend fun entriesRaw(delegatorState: DelegatorState.Delegator): StorageEntries {
        val nominatorIdsArgs = delegatorState.delegations.map { listOf(it.owner, delegatorState.accountId) }

        return runtime.metadata.parachainStaking().storage("NominationScheduledRequests").entriesRaw(nominatorIdsArgs)
    }

    private fun bindNominationRequests(
        instance: Any?,
        collatorId: AccountId,
        nominatorId: AccountId,
    ) = instance?.let {
        bindList(instance) { listElement -> bindNominationRequest(collatorId, nominatorId, listElement) }
    }.orEmpty()

    private fun bindNominationRequest(
        collatorId: AccountId,
        nominatorId: AccountId,
        instance: Any?,
    ): ScheduledDelegationRequest {
        val struct = instance.castToStruct()

        return ScheduledDelegationRequest(
            delegator = nominatorId,
            whenExecutable = bindRoundIndex(struct["whenExecutable"]),
            action = bindDelegationAction(struct.getTyped("action")),
            collator = collatorId
        )
    }
}

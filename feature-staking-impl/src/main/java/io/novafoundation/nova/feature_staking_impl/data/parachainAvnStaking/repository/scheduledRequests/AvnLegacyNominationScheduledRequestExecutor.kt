package io.novafoundation.nova.feature_staking_impl.data.parachainAvnStaking.repository.scheduledRequests

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
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.scheduledRequests.DelegationScheduledRequestExecutor
import io.novafoundation.nova.runtime.storage.source.StorageEntries
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.wrapSingleArgumentKeys
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AvnLegacyNominationScheduledRequestExecutor : DelegationScheduledRequestExecutor {

    context(StorageQueryContext)
    override suspend fun entries(delegatorState: DelegatorState.Delegator): Map<String, ScheduledDelegationRequest> {
        val keyArguments = delegatorState.delegations.map { listOf(it.owner) }

        return runtime.metadata.parachainStaking().storage("NominationScheduledRequests").entries(
            keyArguments,
            keyExtractor = { (collatorId: AccountId) -> collatorId.toHexString() },
            binding = { dynamicInstance, collatorId -> bindNominationRequests(dynamicInstance, collatorId.fromHex()) }
        ).byNominator(delegatorState.accountId)
    }

    context(StorageQueryContext)
    override suspend fun observe(delegatorState: DelegatorState.Delegator): Flow<Collection<ScheduledDelegationRequest>> {
        val keyArguments = delegatorState.delegations.map { listOf(it.owner) }

        return runtime.metadata.parachainStaking().storage("NominationScheduledRequests").observe(
            keyArguments,
            keyExtractor = { (collatorId: AccountId) -> collatorId.toHexString() },
            binding = { dynamicInstance, collatorId -> bindNominationRequests(dynamicInstance, collatorId.fromHex()) }
        ).map { it.filterNotNull().byNominator(delegatorState.accountId).values }
    }

    context(StorageQueryContext)
    override suspend fun query(
        delegatorState: DelegatorState.Delegator,
        collatorId: AccountId
    ): ScheduledDelegationRequest? {
        val allCollatorRequests = runtime.metadata.parachainStaking().storage("NominationScheduledRequests").query(
            collatorId,
            binding = { bindNominationRequests(it, collatorId) }
        )

        return allCollatorRequests.find { it.delegator.contentEquals(delegatorState.accountId) }
    }

    context(StorageQueryContext)
    override suspend fun entriesRaw(delegatorState: DelegatorState.Delegator): StorageEntries {
        val nominatorIdsArgs = delegatorState.delegations.map { it.owner }.wrapSingleArgumentKeys()

        return runtime.metadata.parachainStaking().storage("NominationScheduledRequests").entriesRaw(nominatorIdsArgs)
    }

    private fun bindNominationRequests(instance: Any?, collatorId: AccountId) = instance?.let {
        bindList(instance) { listElement -> bindNominationRequest(collatorId, listElement) }
    }.orEmpty()

    private fun bindNominationRequest(
        collatorId: AccountId,
        instance: Any?,
    ): ScheduledDelegationRequest {
        val struct = instance.castToStruct()
        val nominatorField: Any? = struct.mapping["nominator"] ?: struct.mapping["delegator"]

        return ScheduledDelegationRequest(
            delegator = bindAccountId(nominatorField),
            whenExecutable = bindRoundIndex(struct["whenExecutable"]),
            action = bindDelegationAction(struct.getTyped("action")),
            collator = collatorId
        )
    }

    private fun Map<String, List<ScheduledDelegationRequest>>.byNominator(nominator: AccountId) =
        mapValuesNotNull { (_, pendingRequests) ->
            pendingRequests.find { it.delegator.contentEquals(nominator) }
        }
}

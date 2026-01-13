package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.scheduledRequests

import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.ScheduledDelegationRequest
import io.novafoundation.nova.runtime.storage.source.StorageEntries
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Alias
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Vec
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow

class DelegationScheduledRequestFactory {
    context(StorageQueryContext)
    fun create(): DelegationScheduledRequestExecutor {
        val storage = runtime.metadata.parachainStaking().storage("DelegationScheduledRequests")
        val vec = storage.type.value as Vec
        val alias = vec.typeReference.value as Alias
        val struct = alias.aliasedReference.value as Struct

        return when {
            struct.mapping.contains("delegator") -> LegacyDelegationScheduledRequestExecutor()
            else -> NewDelegationScheduledRequestExecutor()
        }
    }
}

interface DelegationScheduledRequestExecutor {
    context(StorageQueryContext)
    suspend fun entries(delegatorState: DelegatorState.Delegator): Map<String, ScheduledDelegationRequest>

    context(StorageQueryContext)
    suspend fun observe(delegatorState: DelegatorState.Delegator): Flow<Collection<ScheduledDelegationRequest>>

    context(StorageQueryContext)
    suspend fun query(delegatorState: DelegatorState.Delegator, collatorId: AccountId): ScheduledDelegationRequest?

    context(StorageQueryContext)
    suspend fun entriesRaw(delegatorState: DelegatorState.Delegator): StorageEntries
}

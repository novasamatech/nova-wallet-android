package io.novafoundation.nova.feature_staking_impl.data.parachainAvnStaking.repository.scheduledRequests

import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.scheduledRequests.DelegationScheduledRequestExecutor
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Alias
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Vec
import io.novasama.substrate_sdk_android.runtime.metadata.storage

class AvnNominationScheduledRequestFactory {
    context(StorageQueryContext)
    fun create(): DelegationScheduledRequestExecutor {
        val storage = runtime.metadata.parachainStaking().storage("NominationScheduledRequests")
        val vec = storage.type.value as Vec
        val alias = vec.typeReference.value as Alias
        val struct = alias.aliasedReference.value as Struct

        return when {
            struct.mapping.contains("nominator") || struct.mapping.contains("delegator") ->
                AvnLegacyNominationScheduledRequestExecutor()
            else -> AvnNewNominationScheduledRequestExecutor()
        }
    }
}

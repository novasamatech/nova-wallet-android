package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.selecting

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

typealias PoolAvailabilityValidationSystem = ValidationSystem<PoolAvailabilityPayload, PoolAvailabilityFailure>

class PoolAvailabilityPayload(val nominationPool: NominationPool, val chain: Chain)

sealed interface PoolAvailabilityFailure {

    object PoolIsFull : PoolAvailabilityFailure

    object PoolIsClosed : PoolAvailabilityFailure
}

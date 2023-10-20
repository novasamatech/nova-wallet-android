package io.novafoundation.nova.feature_staking_impl.domain.validations.setup

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.isOpen
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.selecting.PoolAvailabilityFailure
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.selecting.PoolAvailabilityPayload

class PoolAvailabilityValidation(
    private val nominationPoolGlobalsRepository: NominationPoolGlobalsRepository
) : Validation<PoolAvailabilityPayload, PoolAvailabilityFailure> {

    override suspend fun validate(value: PoolAvailabilityPayload): ValidationStatus<PoolAvailabilityFailure> {
        val pool = value.nominationPool
        return when {
            !pool.state.isOpen -> validationError(PoolAvailabilityFailure.PoolIsClosed)
            isPoolFull(value) -> validationError(PoolAvailabilityFailure.PoolIsFull)
            else -> valid()
        }
    }

    private suspend fun isPoolFull(value: PoolAvailabilityPayload): Boolean {
        val pool = value.nominationPool
        val maxPoolMembers = nominationPoolGlobalsRepository.maxPoolMembersPerPool(value.chain.id)
        return maxPoolMembers != null && maxPoolMembers <= pool.membersCount
    }
}

fun ValidationSystemBuilder<PoolAvailabilityPayload, PoolAvailabilityFailure>.poolAvailable(
    nominationPoolGlobalsRepository: NominationPoolGlobalsRepository
) {
    val validation = PoolAvailabilityValidation(nominationPoolGlobalsRepository = nominationPoolGlobalsRepository)
    validate(validation)
}

package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.nominationPools

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationSystemBuilder

class MaxPoolMembersValidation(
    private val nominationPoolGlobalsRepository: NominationPoolGlobalsRepository
) : StartMultiStakingValidation {

    override suspend fun validate(value: StartMultiStakingValidationPayload): ValidationStatus<StartMultiStakingValidationFailure> {
        val chainId = value.selection.stakingOption.chain.id
        val stakingType = value.selection.stakingOption.stakingType

        val maxPoolMembers = nominationPoolGlobalsRepository.maxPoolMembers(chainId) ?: return valid()
        val numberOfPoolMembers = nominationPoolGlobalsRepository.counterForPoolMembers(chainId)
        val hasFreeSlots = numberOfPoolMembers < maxPoolMembers

        return hasFreeSlots isTrueOrError {
            StartMultiStakingValidationFailure.MaxNominatorsReached(stakingType)
        }
    }
}

fun StartMultiStakingValidationSystemBuilder.maxPoolMembersNotReached(
    nominationPoolGlobalsRepository: NominationPoolGlobalsRepository
) {
    validate(MaxPoolMembersValidation(nominationPoolGlobalsRepository))
}

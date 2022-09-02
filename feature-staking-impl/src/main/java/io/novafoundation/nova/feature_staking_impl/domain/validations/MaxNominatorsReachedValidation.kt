package io.novafoundation.nova.feature_staking_impl.domain.validations

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState

class MaxNominatorsReachedValidation<P, E>(
    private val stakingRepository: StakingRepository,
    private val isAlreadyNominating: (P) -> Boolean,
    private val sharedState: StakingSharedState,
    private val errorProducer: () -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val chainId = sharedState.chainId()

        val nominatorCount = stakingRepository.nominatorsCount(chainId) ?: return ValidationStatus.Valid()
        val maxNominatorsAllowed = stakingRepository.maxNominators(chainId) ?: return ValidationStatus.Valid()

        if (isAlreadyNominating(value)) {
            return ValidationStatus.Valid()
        }

        return validOrError(nominatorCount < maxNominatorsAllowed) {
            errorProducer()
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.maximumNominatorsReached(
    stakingRepository: StakingRepository,
    isAlreadyNominating: (P) -> Boolean,
    sharedState: StakingSharedState,
    errorProducer: () -> E
) {
    validate(MaxNominatorsReachedValidation(stakingRepository, isAlreadyNominating, sharedState, errorProducer))
}

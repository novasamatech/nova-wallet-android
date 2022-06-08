package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.preliminary

import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.DelegatorStateRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.preliminary.ParachainStakingUnbondPreliminaryValidationFailure.NoAvailableCollators

class AnyAvailableCollatorForUnbondValidationFactory(
    private val delegatorStateRepository: DelegatorStateRepository,
    private val delegatorStateUseCase: DelegatorStateUseCase,
) {

    fun ParachainStakingUnbondPreliminaryValidationSystemBuilder.anyAvailableCollatorForUnbond() {
        validate(AnyAvailableCollatorsForUnbondValidation(delegatorStateRepository, delegatorStateUseCase))
    }
}

class AnyAvailableCollatorsForUnbondValidation(
    private val delegatorStateRepository: DelegatorStateRepository,
    private val delegatorStateUseCase: DelegatorStateUseCase,
) : ParachainStakingUnbondPreliminaryValidation {

    override suspend fun validate(
        value: ParachainStakingUnbondPreliminaryValidationPayload
    ): ValidationStatus<ParachainStakingUnbondPreliminaryValidationFailure> {
        val delegatorState = delegatorStateUseCase.currentDelegatorState().castOrNull<DelegatorState.Delegator>() ?: return valid()

        val pendingRequests = delegatorStateRepository.scheduledDelegationRequests(delegatorState)
        val anyCollatorAvailableForUnbond = pendingRequests.size < delegatorState.delegations.size

        return anyCollatorAvailableForUnbond isTrueOrError { NoAvailableCollators }
    }
}

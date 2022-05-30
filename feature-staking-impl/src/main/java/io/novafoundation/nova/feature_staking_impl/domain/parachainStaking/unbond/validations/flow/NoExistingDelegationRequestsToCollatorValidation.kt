package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.ParachainStakingUnbondInteractor
import jp.co.soramitsu.fearless_utils.extensions.fromHex

class NoExistingDelegationRequestsToCollatorValidationFactory(
    private val interactor: ParachainStakingUnbondInteractor,
    private val delegatorStateUseCase: DelegatorStateUseCase,
) {

    fun ValidationSystemBuilder<ParachainStakingUnbondPayload, ParachainStakingUnbondValidationFailure>.noExistingDelegationRequestsToCollator() {
        validate(NoExistingDelegationRequestsToCollatorValidation(interactor, delegatorStateUseCase))
    }
}

class NoExistingDelegationRequestsToCollatorValidation(
    private val interactor: ParachainStakingUnbondInteractor,
    private val delegatorStateUseCase: DelegatorStateUseCase,
) : ParachainStakingUnbondValidation {

    override suspend fun validate(value: ParachainStakingUnbondPayload): ValidationStatus<ParachainStakingUnbondValidationFailure> {
        val delegatorState = delegatorStateUseCase.currentDelegatorState()
        val collatorId = value.collator.accountIdHex.fromHex()

        val canUnbond = interactor.canUnbond(collatorId, delegatorState)

        return canUnbond isTrueOrError { ParachainStakingUnbondValidationFailure.AlreadyHasDelegationRequestToCollator }
    }
}

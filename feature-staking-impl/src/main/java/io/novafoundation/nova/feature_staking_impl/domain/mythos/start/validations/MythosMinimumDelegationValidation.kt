package io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosStakingRepository
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.activeStake
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import javax.inject.Inject

@FeatureScope
class MythosMinimumDelegationValidationFactory @Inject constructor(
    private val mythosStakingRepository: MythosStakingRepository,
) {

    context(StartMythosStakingValidationSystemBuilder)
    fun minimumDelegation() {
        validate(MythosMinimumDelegationValidation(mythosStakingRepository))
    }
}

private class MythosMinimumDelegationValidation(
    private val mythosStakingRepository: MythosStakingRepository,
) : StartMythosStakingValidation {

    override suspend fun validate(value: StartMythosStakingValidationPayload): ValidationStatus<StartMythosStakingValidationFailure> {
        val minStake = mythosStakingRepository.minStake(value.chainId)

        val amountPlanks = value.asset.token.planksFromAmount(value.amount)
        val newStake = value.delegatorState.activeStake + amountPlanks

        return (newStake >= minStake) isTrueOrError {
            StartMythosStakingValidationFailure.TooLowStakeAmount(
                minimumStake = minStake,
                asset = value.asset
            )
        }
    }
}

package io.novafoundation.nova.feature_staking_impl.domain.validations.setup

import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks

class MinimumAmountValidation(
    private val stakingRepository: StakingRepository,
) : Validation<SetupStakingPayload, SetupStakingValidationFailure> {

    override suspend fun validate(value: SetupStakingPayload): ValidationStatus<SetupStakingValidationFailure> {
        val assetConfiguration = value.asset.token.configuration

        val minimumBondInPlanks = stakingRepository.minimumNominatorBond(assetConfiguration.chainId)
        val minimumBond = assetConfiguration.amountFromPlanks(minimumBondInPlanks)

        // either first time bond or already existing bonded balance
        val amountToCheckAgainstMinimum = value.bondAmount ?: value.asset.bonded

        return if (amountToCheckAgainstMinimum >= minimumBond) {
            ValidationStatus.Valid()
        } else {
            ValidationStatus.NotValid(DefaultFailureLevel.ERROR, SetupStakingValidationFailure.TooSmallAmount(minimumBond))
        }
    }
}

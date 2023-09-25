package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.nominationPools

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.feature_staking_impl.data.asset
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure.AmountLessThanMinimum
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationSystemBuilder
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingProperties
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.StakingMinimumBondError
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.StakingMinimumBondError.ThresholdType

class MinJoinBondValidation(
    private val stakingProperties: SingleStakingProperties,
) : StartMultiStakingValidation {

    override suspend fun validate(value: StartMultiStakingValidationPayload): ValidationStatus<StartMultiStakingValidationFailure> {
        val minStake = stakingProperties.minStake()
        val amountIsEnoughToStake = value.selection.stake >= minStake

        return amountIsEnoughToStake isTrueOrError {
            val context = StakingMinimumBondError.Context(
                threshold = minStake,
                chainAsset = value.selection.stakingOption.asset,
                thresholdType = ThresholdType.REQUIRED
            )

            AmountLessThanMinimum(context)
        }
    }
}

context (SingleStakingProperties)
fun StartMultiStakingValidationSystemBuilder.enoughForMinJoinBond() {
    validate(MinJoinBondValidation(this@SingleStakingProperties))
}

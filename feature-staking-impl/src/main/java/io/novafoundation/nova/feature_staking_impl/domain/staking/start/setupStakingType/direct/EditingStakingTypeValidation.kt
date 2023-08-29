package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingProperties
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.stakingAmountValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.stakingTypeAvailability
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

typealias EditingStakingTypeValidationSystem = ValidationSystem<EditingStakingTypePayload, EditingStakingTypeFailure>

class EditingStakingTypePayload(
    val selectedAmount: BigInteger,
    val stakingType: Chain.Asset.StakingType,
    val minStake: BigInteger,
)

sealed interface EditingStakingTypeFailure {
    class AmountIsLessThanMinStake(val minStake: BigInteger, val stakingType: Chain.Asset.StakingType) : EditingStakingTypeFailure

    class StakingTypeIsAlreadyUsing(val stakingType: Chain.Asset.StakingType) : EditingStakingTypeFailure
}

fun ValidationSystem.Companion.editingStakingType(
    singleStakingProperties: SingleStakingProperties,
    availableStakingTypes: List<Chain.Asset.StakingType>
): EditingStakingTypeValidationSystem {
    return ValidationSystem {
        stakingAmountValidation(
            singleStakingProperties,
            { it.selectedAmount }
        ) { EditingStakingTypeFailure.AmountIsLessThanMinStake(it.minStake, it.stakingType) }

        stakingTypeAvailability(
            availableStakingTypes,
            { it.stakingType },
        ) { EditingStakingTypeFailure.StakingTypeIsAlreadyUsing(it.stakingType) }
    }
}

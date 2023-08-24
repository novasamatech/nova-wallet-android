package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct

import androidx.constraintlayout.solver.widgets.Chain
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigInteger


typealias EditableDirectStakingAvailabilityValidation =
    ValidationSystem<EditableDirectStakingAvailabilityPayload, EditableDirectStakingAvailabilityFailure>

class EditableDirectStakingAvailabilityPayload(
    val chain: Chain,
    val asset: Asset,
    val selectedAmount: BigInteger,
    val minStake: BigInteger
)

sealed interface EditableDirectStakingAvailabilityFailure {
    object AmountIsLessThanMinStake

    object StakeTypeIsAlreadyUsing
}

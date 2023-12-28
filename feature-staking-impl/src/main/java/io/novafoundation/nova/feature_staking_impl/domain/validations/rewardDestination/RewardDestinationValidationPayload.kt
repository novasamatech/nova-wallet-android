package io.novafoundation.nova.feature_staking_impl.domain.validations.rewardDestination

import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import java.math.BigDecimal

class RewardDestinationValidationPayload(
    val availableControllerBalance: BigDecimal,
    val fee: DecimalFee,
    val stashState: StakingState.Stash
)

package io.novafoundation.nova.feature_staking_impl.domain.validations.rewardDestination

import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

class RewardDestinationValidationPayload(
    val availableControllerBalance: BigDecimal,
    val asset: Asset,
    val fee: Fee,
    val stashState: StakingState.Stash
)

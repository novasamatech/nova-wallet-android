package io.novafoundation.nova.feature_staking_impl.domain.validations.balance

import io.novafoundation.nova.feature_staking_api.domain.model.StakingState

class ManageStakingValidationPayload(
    val stashState: StakingState.Stash
)

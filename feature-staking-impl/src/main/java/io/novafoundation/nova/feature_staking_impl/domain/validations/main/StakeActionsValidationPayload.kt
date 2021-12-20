package io.novafoundation.nova.feature_staking_impl.domain.validations.main

import io.novafoundation.nova.feature_staking_api.domain.model.StakingState

class StakeActionsValidationPayload(
    val stashState: StakingState.Stash
)

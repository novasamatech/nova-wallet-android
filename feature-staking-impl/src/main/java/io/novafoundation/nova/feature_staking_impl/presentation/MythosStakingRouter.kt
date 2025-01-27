package io.novafoundation.nova.feature_staking_impl.presentation

import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload

interface MythosStakingRouter : StarkingReturnableRouter {

    fun openCollatorDetails(payload: StakeTargetDetailsPayload)
}

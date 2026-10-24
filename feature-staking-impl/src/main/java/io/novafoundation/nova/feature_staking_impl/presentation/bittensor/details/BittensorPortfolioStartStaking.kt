package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolio
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.AvailableStakingOptionsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.model.StartStakingLandingPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

/** The Earn landing for this portfolio's asset: the first-stake entry and "Add position" share it. */
internal fun BittensorPortfolio.startStakingPayload(): StartStakingLandingPayload {
    val options = AvailableStakingOptionsPayload(chain.id, asset.token.configuration.id, listOf(Chain.Asset.StakingType.BITTENSOR_ROOT))
    return StartStakingLandingPayload(options)
}

package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class CrossChainRewardDestination(
    val addressInDestination: String,
    val destination: Chain,
)

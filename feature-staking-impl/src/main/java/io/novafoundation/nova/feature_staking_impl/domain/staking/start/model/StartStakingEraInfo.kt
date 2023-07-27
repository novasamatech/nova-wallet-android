package io.novafoundation.nova.feature_staking_impl.domain.staking.start.model

import kotlin.time.Duration

class StartStakingEraInfo(
    val unstakeTime: Duration,
    val eraDuration: Duration,
    val firstRewardReceivingDuration: Duration,
)

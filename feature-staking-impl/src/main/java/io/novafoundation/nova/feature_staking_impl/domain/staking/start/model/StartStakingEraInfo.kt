package io.novafoundation.nova.feature_staking_impl.domain.staking.start.model

import kotlin.time.Duration

class StartStakingEraInfo(
    val remainingEraTime: Duration,
    val unstakeTime: Duration,
    val eraDuration: Duration,
)

package io.novafoundation.nova.feature_staking_impl.domain.model

import java.math.BigDecimal

class StakeSummary<S>(
    val status: S,
    val totalStaked: BigDecimal,
)

sealed class NominatorStatus {
    object Active : NominatorStatus()

    class Waiting(val timeLeft: Long) : NominatorStatus()

    class Inactive(val reason: Reason) : NominatorStatus() {

        enum class Reason {
            MIN_STAKE, NO_ACTIVE_VALIDATOR
        }
    }
}

enum class StashNoneStatus {
    INACTIVE
}

enum class ValidatorStatus {
    ACTIVE, INACTIVE
}

package io.novafoundation.nova.feature_staking_impl.domain.model

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class StakeSummary<S>(
    val status: S,
    val activeStake: Balance,
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

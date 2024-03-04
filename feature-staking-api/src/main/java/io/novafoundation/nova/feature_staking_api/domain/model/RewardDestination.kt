package io.novafoundation.nova.feature_staking_api.domain.model

import io.novasama.substrate_sdk_android.runtime.AccountId

sealed class RewardDestination {

    object Restake : RewardDestination()

    class Payout(val targetAccountId: AccountId) : RewardDestination()
}

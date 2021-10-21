package io.novafoundation.nova.feature_staking_impl.presentation.common.rewardDestination

import io.novafoundation.nova.common.address.AddressModel

sealed class RewardDestinationModel {

    object Restake : RewardDestinationModel()

    class Payout(val destination: AddressModel) : RewardDestinationModel() {

        override fun equals(other: Any?): Boolean {
            return other is Payout && other.destination.address == destination.address
        }
    }
}

package io.novafoundation.nova.feature_staking_impl.domain.staking.start.model

sealed interface PayoutType {

    sealed interface Automatic : PayoutType {

        object Restake : Automatic

        object Payout : Automatic
    }

    object Manual : PayoutType
}

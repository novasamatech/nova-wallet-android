package io.novafoundation.nova.feature_staking_impl.domain.model

sealed interface PayoutType {

    sealed interface Automatically : PayoutType {

        object Restake : Automatically

        object Payout : Automatically
    }

    object Manual : PayoutType
}

package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start

sealed class DelegationsLimit {

    object NotReached : DelegationsLimit()

    class Reached(val limit: Int) : DelegationsLimit()
}

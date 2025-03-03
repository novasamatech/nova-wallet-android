package io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget

sealed class ChooseStakedStakeTargetsResponse<out T> {

    object New : ChooseStakedStakeTargetsResponse<Nothing>()

    class Existing<T>(val target: T) : ChooseStakedStakeTargetsResponse<T>()
}

package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.domain.ExtendedLoadingState

data class BittensorSubnetPoolSideUi(
    val title: String,
    val amount: CharSequence,
    val fiat: CharSequence?
)

data class BittensorSubnetPoolSplitUi(
    val nativeTitle: String,
    val alphaTitle: String,
    val nativeShare: CharSequence,
    val alphaShare: CharSequence,
    val nativeFraction: Float?
)

/** Formatting and completeness/registration checks belong to the supplying ViewModel. */
data class BittensorSubnetParametersUi(
    val split: ExtendedLoadingState<BittensorSubnetPoolSplitUi>,
    val nativePool: ExtendedLoadingState<BittensorSubnetPoolSideUi>,
    val alphaPool: ExtendedLoadingState<BittensorSubnetPoolSideUi>,
    val emission: ExtendedLoadingState<CharSequence?>
)

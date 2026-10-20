package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorSaleProfitUi

data class BittensorPositionEarnedUi(
    val value: ExtendedLoadingState<BittensorSaleProfitUi>,
    val isVisible: Boolean
) {
    fun bind(
        renderValue: (ExtendedLoadingState<BittensorSaleProfitUi>) -> Unit,
        renderVisibility: (Boolean) -> Unit
    ) {
        // Loading renders through TableCellView.showProgress(), which makes the cell visible.
        renderValue(value)
        renderVisibility(isVisible)
    }
}

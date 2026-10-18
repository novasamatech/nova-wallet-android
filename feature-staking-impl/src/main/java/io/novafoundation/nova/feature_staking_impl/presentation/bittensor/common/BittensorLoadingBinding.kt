package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

fun TableCellView.showBittensorLoadingValue(state: ExtendedLoadingState<CharSequence?>) {
    showBittensorLoadingState(state) { showValue(it ?: "—") }
}

fun TableCellView.showBittensorLoadingAmount(state: ExtendedLoadingState<AmountModel>) {
    showBittensorLoadingState(state) { showValue(it.token, it.fiat) }
}

private fun <T> TableCellView.showBittensorLoadingState(state: ExtendedLoadingState<T>, showData: (T) -> Unit) {
    when (state) {
        ExtendedLoadingState.Loading -> showProgress()
        is ExtendedLoadingState.Error -> showValue("—")
        is ExtendedLoadingState.Loaded -> showData(state.data)
    }
}

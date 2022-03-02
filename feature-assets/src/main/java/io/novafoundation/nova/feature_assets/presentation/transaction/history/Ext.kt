package io.novafoundation.nova.feature_assets.presentation.transaction.history

import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.TransactionHistoryUi.State

fun TransferHistorySheet.showState(state: State) {
    when (state) {
        is State.Empty -> showPlaceholder()
        is State.EmptyProgress -> showProgress()
        is State.Data -> showTransactions(state.items)
    }
}

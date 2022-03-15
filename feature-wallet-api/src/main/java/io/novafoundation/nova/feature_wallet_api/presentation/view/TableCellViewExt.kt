package io.novafoundation.nova.feature_wallet_api.presentation.view

import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.feature_wallet_api.presentation.model.WalletModel

fun TableCellView.showWallet(walletModel: WalletModel) {
    showValue(walletModel.name)
}

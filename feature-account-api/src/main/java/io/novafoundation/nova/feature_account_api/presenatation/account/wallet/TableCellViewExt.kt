package io.novafoundation.nova.feature_account_api.presenatation.account.wallet

import io.novafoundation.nova.common.view.TableCellView

fun TableCellView.showWallet(walletModel: WalletModel) {
    showValue(walletModel.name)

    walletModel.icon?.let(::setImage)
}

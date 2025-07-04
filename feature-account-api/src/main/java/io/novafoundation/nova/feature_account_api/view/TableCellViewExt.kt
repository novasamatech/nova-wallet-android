package io.novafoundation.nova.feature_account_api.view

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.OptionalAddressModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.common.view.showLoadingState
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi

fun TableCellView.showAddress(addressModel: AddressModel) {
    setImage(addressModel.image)

    showValue(addressModel.nameOrAddress)
}

fun TableCellView.showAddressOrHide(addressModel: AddressModel?) = letOrHide(addressModel, ::showAddress)

fun TableCellView.showOptionalAddress(addressModel: OptionalAddressModel) {
    addressModel.image?.let(::setImage)

    showValue(addressModel.nameOrAddress)
}

fun TableCellView.showChain(chainUi: ChainUi) {
    loadImage(
        url = chainUi.icon,
        placeholderRes = R.drawable.bg_chain_placeholder,
        roundedCornersDp = null
    )

    showValue(chainUi.name)
}

fun TableCellView.showChainOrHide(chainUi: ChainUi?) {
    if (chainUi != null) {
        makeVisible()

        showChain(chainUi)
    } else {
        makeGone()
    }
}

fun TableCellView.showWallet(walletModel: WalletModel) {
    showValue(walletModel.name)

    walletModel.icon?.let(::setImage)
}

fun TableCellView.showAccountWithLoading(loadingState: ExtendedLoadingState<AccountModel>) {
    showLoadingState(loadingState) {
        showAccount(it)
    }
}

fun TableCellView.showAccount(accountModel: AccountModel) {
    when (accountModel) {
        is AccountModel.Address -> showAddress(accountModel)
        is AccountModel.Wallet -> showWallet(accountModel)
    }
}

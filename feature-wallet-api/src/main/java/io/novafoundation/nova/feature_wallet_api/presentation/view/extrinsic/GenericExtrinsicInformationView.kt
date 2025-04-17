package io.novafoundation.nova.feature_wallet_api.presentation.view.extrinsic

import android.content.Context
import android.util.AttributeSet
import android.view.View
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.TableView
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_wallet_api.databinding.ViewGenericExtrinsicInformationBinding
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.view.FeeView

class GenericExtrinsicInformationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : TableView(context, attrs, defStyle) {

    private val binder = ViewGenericExtrinsicInformationBinding.inflate(inflater(), this)

    val fee: FeeView
        get() = binder.viewGenericExtrinsicInformationFee

    fun setOnAccountClickedListener(action: (View) -> Unit) {
        binder.viewGenericExtrinsicInformationAccount.setOnClickListener(action)
    }

    fun setWallet(walletModel: WalletModel) {
        binder.viewGenericExtrinsicInformationWallet.showWallet(walletModel)
    }

    fun setAccount(addressModel: AddressModel) {
        binder.viewGenericExtrinsicInformationAccount.showAddress(addressModel)
    }

    fun setFeeStatus(feeStatus: FeeStatus<*, FeeDisplay>) {
        binder.viewGenericExtrinsicInformationFee.setFeeStatus(feeStatus)
    }
}

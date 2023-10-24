package io.novafoundation.nova.feature_wallet_api.presentation.view.extrinsic

import android.content.Context
import android.util.AttributeSet
import android.view.View
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.view.TableView
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.view.FeeView
import kotlinx.android.synthetic.main.view_generic_extrinsic_information.view.viewGenericExtrinsicInformationAccount
import kotlinx.android.synthetic.main.view_generic_extrinsic_information.view.viewGenericExtrinsicInformationFee
import kotlinx.android.synthetic.main.view_generic_extrinsic_information.view.viewGenericExtrinsicInformationWallet

class GenericExtrinsicInformationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : TableView(context, attrs, defStyle) {

    val fee: FeeView
        get() = viewGenericExtrinsicInformationFee

    init {
        View.inflate(context, R.layout.view_generic_extrinsic_information, this)
    }

    fun setOnAccountClickedListener(action: (View) -> Unit) {
        viewGenericExtrinsicInformationAccount.setOnClickListener(action)
    }

    fun setWallet(walletModel: WalletModel) {
        viewGenericExtrinsicInformationWallet.showWallet(walletModel)
    }

    fun setAccount(addressModel: AddressModel) {
        viewGenericExtrinsicInformationAccount.showAddress(addressModel)
    }

    fun setFeeStatus(feeStatus: FeeStatus<*>) {
        viewGenericExtrinsicInformationFee.setFeeStatus(feeStatus)
    }
}

package io.novafoundation.nova.feature_account_impl.presentation.account.list.selectAddress

import android.os.Bundle
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressRequester
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.account.list.WalletListFragment

class SelectAddressFragment : WalletListFragment<SelectAddressViewModel>() {

    companion object {
        private const val KEY_REQUEST = "KEY_REQUEST"

        fun getBundle(request: SelectAddressRequester.Request): Bundle {
            return Bundle().apply {
                putParcelable(KEY_REQUEST, request)
            }
        }
    }

    override fun initViews() {
        super.initViews()
        setTitleRes(R.string.assets_select_send_your_wallets)
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .selectAddressComponentFactory()
            .create(this, argument(KEY_REQUEST))
            .inject(this)
    }
}

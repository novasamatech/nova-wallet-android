package io.novafoundation.nova.feature_account_impl.presentation.account.list.selecting

import android.os.Bundle
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.SelectWalletRequester
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.account.list.WalletListFragment

class SelectWalletFragment : WalletListFragment<SelectWalletViewModel>() {

    companion object {
        private const val KEY_REQUEST = "KEY_REQUEST"

        fun getBundle(request: SelectWalletRequester.Request): Bundle {
            return Bundle().apply {
                putParcelable(KEY_REQUEST, request)
            }
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .selectWalletComponentFactory()
            .create(this, argument(KEY_REQUEST))
            .inject(this)
    }
}

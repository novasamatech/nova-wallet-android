package io.novafoundation.nova.feature_account_impl.presentation.mixin.selectWallet

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.account.list.WalletListFragment

class SelectWalletFragment : WalletListFragment<SelectWalletViewModel>() {

    override fun initViews() {
        super.initViews()
        setTitleRes(R.string.account_select_wallet)
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .selectWalletComponentFactory()
            .create(this)
            .inject(this)
    }
}

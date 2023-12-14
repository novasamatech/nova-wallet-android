package io.novafoundation.nova.feature_account_impl.presentation.account.list.switching

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.account.list.WalletListFragment

class SwitchWalletFragment : WalletListFragment<SwitchWalletViewModel>() {

    override fun initViews() {
        super.initViews()
        setTitleRes(R.string.account_select_wallet)
        setActionIcon(R.drawable.ic_settings_outline)
        setActionClickListener { viewModel.settingsClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .switchWalletComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
    }
}

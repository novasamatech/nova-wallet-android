package io.novafoundation.nova.feature_dapp_impl.walletConnect.presentation.sessions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import kotlinx.android.synthetic.main.fragment_wc_sessions.wcSessionsToolbar

class WalletConnectSessionsFragment : BaseFragment<WalletConnectSessionsViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_wc_sessions, container, false)

    override fun initViews() {
        wcSessionsToolbar.setHomeButtonListener { viewModel.exit() }
        wcSessionsToolbar.setRightActionClickListener { viewModel.initiateScan() }
        wcSessionsToolbar.applyStatusBarInsets()
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(
            requireContext(),
            DAppFeatureApi::class.java
        )
            .walletConnectSessionsComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: WalletConnectSessionsViewModel) {

    }
}

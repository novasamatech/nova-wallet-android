package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.setMessageOrHide
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.presenatation.chain.showChainsOverview
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.setupSelectWalletMixin
import io.novafoundation.nova.feature_external_sign_api.presentation.dapp.showDAppIcon
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi
import io.novafoundation.nova.feature_wallet_connect_impl.R
import io.novafoundation.nova.feature_wallet_connect_impl.di.WalletConnectFeatureComponent
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve.view.WCNetworksBottomSheet

import javax.inject.Inject

class WalletConnectApproveSessionFragment : BaseFragment<WalletConnectApproveSessionViewModel>() {

    @Inject
    protected lateinit var imageLoader: ImageLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_wc_session_approve, container, false)

    override fun initViews() {
        onBackPressed { viewModel.exit() }

        wcApproveSessionToolbar.setHomeButtonListener { viewModel.exit() }
        wcApproveSessionToolbar.applyStatusBarInsets()

        wcApproveSessionReject.setOnClickListener { viewModel.rejectClicked() }
        wcApproveSessionReject.prepareForProgress(viewLifecycleOwner)

        wcApproveSessionAllow.prepareForProgress(viewLifecycleOwner)
        wcApproveSessionAllow.setOnClickListener { viewModel.approveClicked() }

        wcApproveSessionNetworks.setOnClickListener { viewModel.networksClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<WalletConnectFeatureComponent>(
            requireContext(),
            WalletConnectFeatureApi::class.java
        )
            .walletConnectApproveSessionComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: WalletConnectApproveSessionViewModel) {
        setupSelectWalletMixin(viewModel.selectWalletMixin, wcApproveSessionWallet)

        viewModel.sessionMetadata.observe { sessionMetadata ->
            wcApproveSessionDApp.showValueOrHide(sessionMetadata.dAppUrl)
            wcApproveSessionIcon.showDAppIcon(sessionMetadata.icon, imageLoader)
        }

        viewModel.chainsOverviewFlow.observe(wcApproveSessionNetworks::showChainsOverview)

        viewModel.title.observe(wcApproveSessionTitle::setText)

        viewModel.sessionAlerts.observe { sessionAlerts ->
            wcApproveSessionChainsAlert.setMessageOrHide(sessionAlerts.unsupportedChains?.alertContent)
            wcApproveSessionAccountsAlert.setMessageOrHide(sessionAlerts.missingAccounts?.alertContent)
        }

        viewModel.allowButtonState.observe(wcApproveSessionAllow::setState)
        viewModel.rejectButtonState.observe(wcApproveSessionReject::setState)

        viewModel.showNetworksBottomSheet.observeEvent { data ->
            WCNetworksBottomSheet(context = requireContext(), data = data)
                .show()
        }
    }
}

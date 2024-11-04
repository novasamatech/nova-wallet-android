package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import by.kirich1409.viewbindingdelegate.viewBinding
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
import io.novafoundation.nova.feature_wallet_connect_impl.databinding.FragmentWcSessionApproveBinding
import io.novafoundation.nova.feature_wallet_connect_impl.di.WalletConnectFeatureComponent
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve.view.WCNetworksBottomSheet

import javax.inject.Inject

class WalletConnectApproveSessionFragment : BaseFragment<WalletConnectApproveSessionViewModel, FragmentWcSessionApproveBinding>() {

    override val binder by viewBinding(FragmentWcSessionApproveBinding::bind)

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_wc_session_approve, container, false)

    override fun initViews() {
        onBackPressed { viewModel.exit() }

        binder.wcApproveSessionToolbar.setHomeButtonListener { viewModel.exit() }
        binder.wcApproveSessionToolbar.applyStatusBarInsets()

        binder.wcApproveSessionReject.setOnClickListener { viewModel.rejectClicked() }
        binder.wcApproveSessionReject.prepareForProgress(viewLifecycleOwner)

        binder.wcApproveSessionAllow.prepareForProgress(viewLifecycleOwner)
        binder.wcApproveSessionAllow.setOnClickListener { viewModel.approveClicked() }

        binder.wcApproveSessionNetworks.setOnClickListener { viewModel.networksClicked() }
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
        setupSelectWalletMixin(viewModel.selectWalletMixin, binder.wcApproveSessionWallet)

        viewModel.sessionMetadata.observe { sessionMetadata ->
            binder.wcApproveSessionDApp.showValueOrHide(sessionMetadata.dAppUrl)
            binder.wcApproveSessionIcon.showDAppIcon(sessionMetadata.icon, imageLoader)
        }

        viewModel.chainsOverviewFlow.observe(binder.wcApproveSessionNetworks::showChainsOverview)

        viewModel.title.observe(binder.wcApproveSessionTitle::setText)

        viewModel.sessionAlerts.observe { sessionAlerts ->
            binder.wcApproveSessionChainsAlert.setMessageOrHide(sessionAlerts.unsupportedChains?.alertContent)
            binder.wcApproveSessionAccountsAlert.setMessageOrHide(sessionAlerts.missingAccounts?.alertContent)
        }

        viewModel.allowButtonState.observe(binder.wcApproveSessionAllow::setState)
        viewModel.rejectButtonState.observe(binder.wcApproveSessionReject::setState)

        viewModel.showNetworksBottomSheet.observeEvent { data ->
            WCNetworksBottomSheet(context = requireContext(), data = data)
                .show()
        }
    }
}

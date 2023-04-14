package io.novafoundation.nova.feature_dapp_impl.walletConnect.presentation.sessions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DappPendingConfirmation
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.sheets.ConfirmAuthorizeBottomSheet
import kotlinx.android.synthetic.main.fragment_wc_sessions.wcSessionsToolbar
import javax.inject.Inject

class WalletConnectSessionsFragment : BaseFragment<WalletConnectSessionsViewModel>() {

    @Inject
    protected lateinit var imageLoader: ImageLoader

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
        viewModel.authorizeDapp.awaitableActionLiveData.observeEvent { action ->
            ConfirmAuthorizeBottomSheet(
                context = requireContext(),
                confirmation = DappPendingConfirmation(
                    onConfirm = { action.onSuccess(true) },
                    onDeny = { action.onSuccess(false) },
                    onCancel = { action.onSuccess(false) },
                    action = DappPendingConfirmation.Action.Authorize(action.payload)
                ),
                imageLoader = imageLoader
            ).show()
        }
    }
}

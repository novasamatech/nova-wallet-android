package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_external_sign_api.presentation.externalSign.AuthorizeDappBottomSheet
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi
import io.novafoundation.nova.feature_wallet_connect_impl.R
import io.novafoundation.nova.feature_wallet_connect_impl.di.WalletConnectFeatureComponent
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
        FeatureUtils.getFeature<WalletConnectFeatureComponent>(
            requireContext(),
            WalletConnectFeatureApi::class.java
        )
            .walletConnectSessionsComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: WalletConnectSessionsViewModel) {
        viewModel.authorizeDapp.awaitableActionLiveData.observeEvent { action ->
            AuthorizeDappBottomSheet(
                context = requireContext(),
                onConfirm = { action.onSuccess(true) },
                onDeny = { action.onSuccess(false) },
                payload = action.payload,
                imageLoader = imageLoader
            ).show()
        }
    }
}

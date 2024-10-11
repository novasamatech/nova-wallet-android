package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainListBottomSheet
import io.novafoundation.nova.feature_account_api.presenatation.chain.showChainsOverview
import io.novafoundation.nova.feature_external_sign_api.presentation.dapp.showDAppIcon
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi
import io.novafoundation.nova.feature_wallet_connect_impl.R
import io.novafoundation.nova.feature_wallet_connect_impl.di.WalletConnectFeatureComponent

import javax.inject.Inject

class WalletConnectSessionDetailsFragment : BaseFragment<WalletConnectSessionDetailsViewModel>() {

    companion object {

        private const val KEY_PAYLOAD = "WalletConnectSessionsFragment.Payload"
        fun getBundle(payload: WalletConnectSessionDetailsPayload) = bundleOf(KEY_PAYLOAD to payload)
    }

    @Inject
    protected lateinit var imageLoader: ImageLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_wc_session_details, container, false)

    override fun initViews() {
        wcSessionDetailsToolbar.setHomeButtonListener { viewModel.exit() }
        wcSessionDetailsToolbar.applyStatusBarInsets()

        wcSessionDetailsDisconnect.setOnClickListener { viewModel.disconnect() }
        wcSessionDetailsDisconnect.prepareForProgress(viewLifecycleOwner)
        wcSessionDetailsNetworks.setOnClickListener { viewModel.networksClicked() }

        wcSessionDetailsStatus.showValue(getString(R.string.common_active))
    }

    override fun inject() {
        FeatureUtils.getFeature<WalletConnectFeatureComponent>(
            requireContext(),
            WalletConnectFeatureApi::class.java
        )
            .walletConnectSessionDetailsComponentFactory()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: WalletConnectSessionDetailsViewModel) {
        viewModel.sessionUi.observe { sessionUi ->
            wcSessionDetailsWallet.showWallet(sessionUi.wallet)
            wcSessionDetailsDApp.showValueOrHide(sessionUi.dappUrl)
            wcSessionDetailsNetworks.showChainsOverview(sessionUi.networksOverview)

            wcSessionDetailsTitle.text = sessionUi.dappTitle
            wcSessionDetailsIcon.showDAppIcon(sessionUi.dappIcon, imageLoader)

            with(sessionUi.status) {
                wcSessionDetailsStatus.setImage(icon, sizeDp = 14)
                wcSessionDetailsStatus.setPrimaryValueStyle(labelStyle)
                wcSessionDetailsStatus.showValue(label)
            }
        }

        viewModel.showChainBottomSheet.observeEvent { chainList ->
            ChainListBottomSheet(
                context = requireContext(),
                data = chainList
            ).show()
        }

        viewModel.disconnectButtonState.observe(wcSessionDetailsDisconnect::setState)
    }
}

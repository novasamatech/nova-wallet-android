package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details

import androidx.core.os.bundleOf

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.view.showWallet
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainListBottomSheet
import io.novafoundation.nova.feature_account_api.presenatation.chain.showChainsOverview
import io.novafoundation.nova.feature_external_sign_api.presentation.dapp.showDAppIcon
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi
import io.novafoundation.nova.feature_wallet_connect_impl.R
import io.novafoundation.nova.feature_wallet_connect_impl.databinding.FragmentWcSessionDetailsBinding
import io.novafoundation.nova.feature_wallet_connect_impl.di.WalletConnectFeatureComponent

import javax.inject.Inject

class WalletConnectSessionDetailsFragment : BaseFragment<WalletConnectSessionDetailsViewModel, FragmentWcSessionDetailsBinding>() {

    companion object {

        private const val KEY_PAYLOAD = "WalletConnectSessionsFragment.Payload"
        fun getBundle(payload: WalletConnectSessionDetailsPayload) = bundleOf(KEY_PAYLOAD to payload)
    }

    override fun createBinding() = FragmentWcSessionDetailsBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun initViews() {
        binder.wcSessionDetailsToolbar.setHomeButtonListener { viewModel.exit() }

        binder.wcSessionDetailsDisconnect.setOnClickListener { viewModel.disconnect() }
        binder.wcSessionDetailsDisconnect.prepareForProgress(viewLifecycleOwner)
        binder.wcSessionDetailsNetworks.setOnClickListener { viewModel.networksClicked() }

        binder.wcSessionDetailsStatus.showValue(getString(R.string.common_active))
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
            binder.wcSessionDetailsWallet.showWallet(sessionUi.wallet)
            binder.wcSessionDetailsDApp.showValueOrHide(sessionUi.dappUrl)
            binder.wcSessionDetailsNetworks.showChainsOverview(sessionUi.networksOverview)

            binder.wcSessionDetailsTitle.text = sessionUi.dappTitle
            binder.wcSessionDetailsIcon.showDAppIcon(sessionUi.dappIcon, imageLoader)

            with(sessionUi.status) {
                binder.wcSessionDetailsStatus.setImage(icon, sizeDp = 14)
                binder.wcSessionDetailsStatus.setPrimaryValueStyle(labelStyle)
                binder.wcSessionDetailsStatus.showValue(label)
            }
        }

        viewModel.showChainBottomSheet.observeEvent { chainList ->
            ChainListBottomSheet(
                context = requireContext(),
                data = chainList
            ).show()
        }

        viewModel.disconnectButtonState.observe(binder.wcSessionDetailsDisconnect::setState)
    }
}

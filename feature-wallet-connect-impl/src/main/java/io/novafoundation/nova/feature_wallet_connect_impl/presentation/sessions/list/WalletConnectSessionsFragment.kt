package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list

import androidx.core.os.bundleOf

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi
import io.novafoundation.nova.feature_wallet_connect_impl.databinding.FragmentWcSessionsBinding
import io.novafoundation.nova.feature_wallet_connect_impl.di.WalletConnectFeatureComponent
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.model.SessionListModel

import javax.inject.Inject

class WalletConnectSessionsFragment : BaseFragment<WalletConnectSessionsViewModel, FragmentWcSessionsBinding>(), WalletConnectSessionsAdapter.Handler {

    companion object {

        private const val KEY_PAYLOAD = "WalletConnectSessionsFragment.Payload"
        fun getBundle(payload: WalletConnectSessionsPayload) = bundleOf(KEY_PAYLOAD to payload)
    }

    override fun createBinding() = FragmentWcSessionsBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val sessionsAdapter = WalletConnectSessionsAdapter(handler = this)

    override fun initViews() {
        binder.wcSessionsToolbar.setHomeButtonListener { viewModel.exit() }

        binder.wcSessionsConnectionsList.setHasFixedSize(true)
        binder.wcSessionsConnectionsList.adapter = sessionsAdapter

        binder.wcSessionsNewConnection.setOnClickListener { viewModel.newSessionClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<WalletConnectFeatureComponent>(
            requireContext(),
            WalletConnectFeatureApi::class.java
        )
            .walletConnectSessionsComponentFactory()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: WalletConnectSessionsViewModel) {
        viewModel.sessionsFlow.observe { sessions ->
            sessionsAdapter.submitList(sessions)

            binder.wcSessionsConnectionsList.setVisible(sessions.isNotEmpty())
            binder.wcSessionsConnectionsPlaceholder.setVisible(sessions.isEmpty())
        }
    }

    override fun itemClicked(item: SessionListModel) {
        viewModel.sessionClicked(item)
    }
}

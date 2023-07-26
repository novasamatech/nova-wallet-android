package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi
import io.novafoundation.nova.feature_wallet_connect_impl.R
import io.novafoundation.nova.feature_wallet_connect_impl.di.WalletConnectFeatureComponent
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.model.SessionListModel
import kotlinx.android.synthetic.main.fragment_wc_sessions.wcSessionsConnectionsList
import kotlinx.android.synthetic.main.fragment_wc_sessions.wcSessionsConnectionsPlaceholder
import kotlinx.android.synthetic.main.fragment_wc_sessions.wcSessionsNewConnection
import kotlinx.android.synthetic.main.fragment_wc_sessions.wcSessionsToolbar
import javax.inject.Inject

class WalletConnectSessionsFragment : BaseFragment<WalletConnectSessionsViewModel>(), WalletConnectSessionsAdapter.Handler {

    companion object {

        private const val KEY_PAYLOAD = "WalletConnectSessionsFragment.Payload"
        fun getBundle(payload: WalletConnectSessionsPayload) = bundleOf(KEY_PAYLOAD to payload)
    }

    @Inject
    protected lateinit var imageLoader: ImageLoader

    private val sessionsAdapter = WalletConnectSessionsAdapter(handler = this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_wc_sessions, container, false)

    override fun initViews() {
        wcSessionsToolbar.setHomeButtonListener { viewModel.exit() }
        wcSessionsToolbar.applyStatusBarInsets()

        wcSessionsConnectionsList.setHasFixedSize(true)
        wcSessionsConnectionsList.adapter = sessionsAdapter

        wcSessionsNewConnection.setOnClickListener { viewModel.newSessionClicked() }
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

            wcSessionsConnectionsList.setVisible(sessions.isNotEmpty())
            wcSessionsConnectionsPlaceholder.setVisible(sessions.isEmpty())
        }
    }

    override fun itemClicked(item: SessionListModel) {
        viewModel.sessionClicked(item)
    }
}

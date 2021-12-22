package io.novafoundation.nova.feature_dapp_impl.presentation.browser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.DappPendingConfirmation.Action
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.confirm.ConfirmAuthorizeBottomSheet
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3WebViewClientFactory
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewHolder
import io.novafoundation.nova.feature_dapp_impl.web3.webview.injectWeb3
import kotlinx.android.synthetic.main.fragment_dapp_browser.dappBrowserWebView
import javax.inject.Inject

class DAppBrowserFragment : BaseFragment<DAppBrowserViewModel>() {

    @Inject
    lateinit var web3WebViewClientFactory: Web3WebViewClientFactory

    @Inject
    lateinit var webViewHolder: WebViewHolder

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_dapp_browser, container, false)
    }

    override fun initViews() {
        webViewHolder.set(dappBrowserWebView)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        webViewHolder.release()
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(this, DAppFeatureApi::class.java)
            .browserComponentFactory()
            .create(this)
            .inject(this)
    }

    @Suppress("UNCHECKED_CAST")
    override fun subscribe(viewModel: DAppBrowserViewModel) {
        dappBrowserWebView.injectWeb3(web3WebViewClientFactory)

        dappBrowserWebView.loadUrl("https://singular.rmrk.app")
//        dappBrowserWebView.loadUrl("https://polkadot.js.org/apps/?rpc=wss%3A%2F%2Fwestend.api.onfinality.io%2Fpublic-ws#/accounts")

        viewModel.showConfirmationSheet.observeEvent {
            when (it.action) {
                is Action.Authorize -> {
                    showConfirmAuthorizeSheet(it as DappPendingConfirmation<Action.Authorize>)
                }

                is Action.SignExtrinsic -> {} // TODO
            }
        }
    }

    private fun showConfirmAuthorizeSheet(pendingConfirmation: DappPendingConfirmation<Action.Authorize>) {
        ConfirmAuthorizeBottomSheet(
            context = requireContext(),
            confirmation = pendingConfirmation,
            imageLoader = imageLoader
        ).show()
    }
}

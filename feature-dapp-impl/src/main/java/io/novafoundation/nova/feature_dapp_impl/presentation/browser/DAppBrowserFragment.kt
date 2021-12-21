package io.novafoundation.nova.feature_dapp_impl.presentation.browser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
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

    override fun subscribe(viewModel: DAppBrowserViewModel) {
        dappBrowserWebView.injectWeb3(web3WebViewClientFactory)

        dappBrowserWebView.loadUrl("https://singular.rmrk.app")
    }
}

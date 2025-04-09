package io.novafoundation.nova.feature_assets.presentation.trade.webInterface

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import kotlinx.android.synthetic.main.fragment_trade_web_interface.tradeWebToolbar
import kotlinx.android.synthetic.main.fragment_trade_web_interface.tradeWebView

class TradeWebFragment : BaseFragment<TradeWebViewModel>() {

    companion object : PayloadCreator<TradeWebPayload> by FragmentPayloadCreator()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_trade_web_interface, container, false)
    }

    override fun initViews() {
        tradeWebToolbar.applyStatusBarInsets()
        tradeWebToolbar.setHomeButtonListener { viewModel.back() }

        tradeWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            useWideViewPort = true
            displayZoomControls = false
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .tradeWebComponent()
            .create(this, payload())
            .inject(this)
    }

    override fun subscribe(viewModel: TradeWebViewModel) {
        viewModel.integrator.observe {
            it.run(tradeWebView)
        }

        viewModel.webChromeClientFlow.observeFirst {
            tradeWebView.webChromeClient = it
        }
    }
}
